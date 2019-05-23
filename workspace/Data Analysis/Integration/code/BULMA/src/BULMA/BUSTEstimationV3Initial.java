package BULMA;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.collections.IteratorUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;

import com.clearspring.analytics.util.Lists;

import BULMADependences.BulmaBusteOutput;
import BULMADependences.BulmaOutputGrouping;
import BULMADependences.OutputString;
import BULMADependences.Problem;
import BULMADependences.ShapeLine;
import PointDependencies.GeoPoint;
import PointDependencies.ShapePoint;
import scala.Tuple2;

//TODO try to add this after the last bulma output

/**
 * (Spark implementation version)
 * 
 * This class does the post-processing of Bulma, get its output and interpolates
 * the shape file. Besides, this class add the stop points file to the output.
 * In this version (V3), this class includes the bus tickets file to the output
 * and returns only the lines with busStop.
 * 
 * @author Andreza
 *
 */
public class BUSTEstimationV3Initial {

	private static final String SEPARATOR = ",";
	private static final String SLASH = "/";
	protected static final String OUTPUT_HEADER = "route,tripNum,shapeId,shapeSequence,shapeLat,shapeLon,distanceTraveledShape,"
			+ "busCode,gpsPointId,gpsLat,gpsLon,distanceToShapePoint,gps_datetime,stopPointId,problem,"
			+ "boarding_id,lineName,cardNum,birthdate,gender,boarding_datetime";

	public static void main(String[] args) throws IOException, URISyntaxException, ParseException {

		if (args.length < 6) {
			System.err.println(
					"Usage: <Output Bulma directory> <shape file> <Bus stops file> <Bus tickets directory> <outputPath> <number of partitions>");
			System.exit(1);
		}
		Long initialTime = System.currentTimeMillis();

		String pathBulmaOutput = args[0];
		String pathFileShapes = args[1];
		String busStopsFile = args[2];
		String busTicketPath = args[3];
		String outputPath = args[4];
		final Integer minPartitions = Integer.valueOf(args[5]);

//		SparkConf sparkConf = new SparkConf().setAppName("BUSTEstimationV3").setMaster("local");
		SparkConf sparkConf = new SparkConf().setAppName("BUSTEstimationV3");
		JavaSparkContext context = new JavaSparkContext(sparkConf);

		generateOutputFilesHDFS(context, pathBulmaOutput, pathFileShapes, busStopsFile, busTicketPath, outputPath,
				minPartitions);

		context.stop();
		context.close();
		System.out.println("Execution time: " + (System.currentTimeMillis() - initialTime) + " ms");
	}

	private static void generateOutputFilesHDFS(JavaSparkContext context, String pathBulmaOutput, String pathFileShapes,
			String busStopsFile, String busTicketPath, String output, int minPartitions)
			throws IOException, URISyntaxException, ParseException {

		/**
		 * Removes empty lines from file
		 * 
		 * @return the file without the empty lines
		 */
		Function2<Integer, Iterator<String>, Iterator<String>> removeEmptyLines = new Function2<Integer, Iterator<String>, Iterator<String>>() {

			private static final long serialVersionUID = -4475494148847393258L;

			public Iterator<String> call(Integer index, Iterator<String> iterator) throws Exception {
				List<String> output = new LinkedList<String>();
				String line;
				while (iterator.hasNext()) {
					line = iterator.next();
					if (!line.isEmpty()) {
						output.add(line);
					}

				}
				return output.iterator();

			}
		};

		Configuration conf = new Configuration();
		FileSystem fs = FileSystem.get(new URI(pathBulmaOutput), conf);
		FileStatus[] fileStatus = fs.listStatus(new Path(pathBulmaOutput));

		for (FileStatus file : fileStatus) {

			String dailyPathDir = pathBulmaOutput + SLASH + file.getPath().getName();

			FileStatus[] fileStatusDaily = fs.listStatus(new Path(dailyPathDir));

			JavaRDD<String> bulmaOutputString = context.textFile(dailyPathDir + SLASH + "part-00000");

			for (FileStatus filePart : fileStatusDaily) {
				if (!filePart.getPath().getName().equals("_SUCCESS")
						&& !filePart.getPath().getName().equals("part-00000")) {
					bulmaOutputString = bulmaOutputString
							.union(context.textFile(dailyPathDir + SLASH + filePart.getPath().getName()));

					System.out.println(dailyPathDir + SLASH + filePart.getPath().getName());
				}
			}
			bulmaOutputString = bulmaOutputString.mapPartitionsWithIndex(removeEmptyLines, false);

			
			String stringDate = file.getPath().getName().substring(0, file.getPath().getName().lastIndexOf("_veiculos"));
			
			String previousDate = subtractDay(stringDate);
			
			JavaRDD<String> result = execute(context, bulmaOutputString, pathFileShapes, busStopsFile,
					minPartitions, previousDate.replace("_", "-"));

			/**
			 * Inserts a header into each output file
			 * 
			 * @return the output file with a new header
			 */
			Function2<Integer, Iterator<String>, Iterator<String>> insertHeader = new Function2<Integer, Iterator<String>, Iterator<String>>() {

				private static final long serialVersionUID = 6196875196870694185L;

				public Iterator<String> call(Integer index, Iterator<String> iterator) throws Exception {
					List<String> output = new LinkedList<String>();
					output.add(OUTPUT_HEADER);
					output.addAll(IteratorUtils.toList(iterator));

					return output.iterator();
				}
			};

			result
			.mapPartitionsWithIndex(insertHeader, false)
					.saveAsTextFile(output + SLASH + previousDate);
		}

	}

	@SuppressWarnings("serial")
	private static JavaRDD<String> execute(JavaSparkContext context, JavaRDD<String> bulmaOutputString,
			String pathFileShapes, String busStopsFile, int minPartitions, final String previousDate) {

		/**
		 * Removes header (first line) from file
		 * 
		 * @return the file without the header
		 */
		Function2<Integer, Iterator<String>, Iterator<String>> removeHeader = new Function2<Integer, Iterator<String>, Iterator<String>>() {

			public Iterator<String> call(Integer index, Iterator<String> iterator) throws Exception {
				if (index == 0 && iterator.hasNext()) {
					iterator.next();
					return iterator;
				} else {
					return iterator;
				}
			}
		};
		
		JavaRDD<String> busStopsString = context.textFile(busStopsFile, minPartitions)
				.mapPartitionsWithIndex(removeHeader, false);

		JavaPairRDD<String, Iterable<BulmaBusteOutput>> rddBulmaOutputGrouped = bulmaOutputString
				.mapToPair(new PairFunction<String, String, BulmaBusteOutput>() {

					public Tuple2<String, BulmaBusteOutput> call(String bulmaOutputString) throws Exception {
						StringTokenizer st = new StringTokenizer(bulmaOutputString, SEPARATOR);
						BulmaBusteOutput bulmaOutput = new BulmaBusteOutput(st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken(),
								st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken(),
								st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken(),
								st.nextToken(), previousDate);

						String codeTripShapeKey = bulmaOutput.getBusCode() + ":" + bulmaOutput.getTripNum() + ":"
								+ bulmaOutput.getShapeId();

						return new Tuple2<String, BulmaBusteOutput>(codeTripShapeKey, bulmaOutput);
					}
				}).groupByKey(minPartitions);
		
		JavaPairRDD<String, Object> rddBulmaOutputGrouping = rddBulmaOutputGrouped
				.mapToPair(new PairFunction<Tuple2<String, Iterable<BulmaBusteOutput>>, String, Object>() {

					public Tuple2<String, Object> call(Tuple2<String, Iterable<BulmaBusteOutput>> codeTripShape_BulmaOutput) throws Exception {
						Map<String, BulmaBusteOutput> mapOutputGrouping = new HashMap<String, BulmaBusteOutput>();
						String codeTripShapeKey = codeTripShape_BulmaOutput._1.split("\\:")[2]; // [2] = shapeId
									
						List<BulmaBusteOutput> listBulmaOutput = Lists.newArrayList(codeTripShape_BulmaOutput._2);
						Collections.sort(listBulmaOutput);
						
						for (BulmaBusteOutput bulmaOutput : listBulmaOutput) {
							mapOutputGrouping.put(bulmaOutput.getShapeSequence(), bulmaOutput);
						}

						//use map if you want to change the key
						return new Tuple2<String, Object>(codeTripShapeKey, new BulmaOutputGrouping(mapOutputGrouping));
					}
				});		

		JavaPairRDD<String, Object> rddBusStops = busStopsString.mapToPair(new PairFunction<String, String, Object>() {

			public Tuple2<String, Object> call(String busStopsString) throws Exception {
				String[] splittedEntry = busStopsString.split(SEPARATOR);
				// shapeID , shapeSequence + '.' + stopId
				return new Tuple2<String, Object>(splittedEntry[7], splittedEntry[8] + "." + splittedEntry[2]);
			}
		});

		JavaRDD<String> shapeString = context.textFile(pathFileShapes, minPartitions)
				.mapPartitionsWithIndex(removeHeader, false);

		JavaPairRDD<String, Iterable<ShapePoint>> rddShapePointsGrouped = shapeString
				.mapToPair(new PairFunction<String, String, ShapePoint>() {

					public Tuple2<String, ShapePoint> call(String shapeString) throws Exception {
						ShapePoint shapePoint = ShapePoint.createShapePointRoute(shapeString);
						return new Tuple2<String, ShapePoint>(shapePoint.getId(), shapePoint);
					}
				}).groupByKey(minPartitions);
		
		JavaPairRDD<String, Object> rddShapeLinePair = rddShapePointsGrouped
				.mapToPair(new PairFunction<Tuple2<String, Iterable<ShapePoint>>, String, Object>() {

					public Tuple2<String, Object> call(Tuple2<String, Iterable<ShapePoint>> shapeId_shapePointList) throws Exception {

						LinkedList<GeoPoint> listShapePoints = new LinkedList<GeoPoint>();
						Iterator<ShapePoint> it = shapeId_shapePointList._2.iterator();
						while (it.hasNext()) {
							listShapePoints.add(it.next());
						}
												
						Collections.sort(listShapePoints);

						String route = ((ShapePoint) listShapePoints.get(listShapePoints.size() - 1)).getRoute();
						ShapeLine shapeLine = new ShapeLine(shapeId_shapePointList._1, listShapePoints, route);

						return new Tuple2<String, Object>(shapeId_shapePointList._1, shapeLine);
					}
				});

		JavaPairRDD<String, Iterable<Object>> rddUnion = rddBulmaOutputGrouping.union(rddShapeLinePair)
				.union(rddBusStops).groupByKey(minPartitions);

		
		JavaRDD<String> rddInterpolation = rddUnion
				.flatMap(new FlatMapFunction<Tuple2<String, Iterable<Object>>, String>() {

					private ShapeLine shapeLine;
					private List<BulmaOutputGrouping> listBulmaOutputGrouping;
					private Map<String, String> mapStopPoints; // Map<ShapeSequence,StopPointId>
					private Map<String, String> mapAux;

					public Iterator<String> call(Tuple2<String, Iterable<Object>> shapeId_objects) throws Exception {
						List<String> listOutput = new LinkedList<String>();

						shapeLine = null;
						listBulmaOutputGrouping = new ArrayList<BulmaOutputGrouping>();
						mapStopPoints = new HashMap<String, String>();
						mapAux = new HashMap<String, String>();

						List<Object> listInput = Lists.newArrayList(shapeId_objects._2);
						for (Object obj : listInput) {
							if (obj instanceof BulmaOutputGrouping) {
								listBulmaOutputGrouping.add((BulmaOutputGrouping) obj);
							} else if (obj instanceof ShapeLine) {
								if (shapeLine != null) {
									System.err.println("Error");
								}
								shapeLine = (ShapeLine) obj;
							} else {
								String shapeSequenceStopId = (String) obj;
								String[] splittedObj = shapeSequenceStopId.split("\\.");

								mapStopPoints.put(splittedObj[0], splittedObj[1]);
								mapAux.put(splittedObj[0], splittedObj[1]);
							}
						}

						if (shapeLine == null) {
							return listOutput.iterator();
						}

						for (BulmaOutputGrouping bulmaOutputGrouping : listBulmaOutputGrouping) {
							Tuple2<Float, String> previousPoint = null;
							Tuple2<Float, String> nextPoint = null;
							List<Integer> pointsBetweenGPS = new LinkedList<Integer>();

							String tripNum = "-";

							for (int i = 0; i < shapeLine.getListGeoPoints().size(); i++) {
								ShapePoint currentShapePoint = (ShapePoint) shapeLine.getListGeoPoints().get(i);
								String currentShapeSequence = currentShapePoint.getPointSequence();
								String currentDistanceTraveled = currentShapePoint.getDistanceTraveled().toString();
								String currentShapeId = shapeLine.getId(); // shape id
								String currentLatShape = currentShapePoint.getLatitude();
								String currentLonShape = currentShapePoint.getLongitude();
								String currentRoute = shapeLine.getRoute();

								String currentTimestamp;
								String currentGPSDateTime;

								if (previousPoint == null) {
									if (bulmaOutputGrouping.containsShapeSequence(currentShapeSequence)) {

										BulmaBusteOutput currentOutput = bulmaOutputGrouping.getMapOutputGrouping()
												.get(currentShapeSequence);

										currentTimestamp = currentOutput.getTimestamp();
										currentGPSDateTime = currentOutput.getGps_datetime();
										
										previousPoint = new Tuple2<Float, String>(
												currentShapePoint.getDistanceTraveled(), currentTimestamp);

										String busCode = currentOutput.getBusCode();
										String gpsPointId = currentOutput.getGpsPointId();
										String problemCode = currentOutput.getTripProblem();
										tripNum = currentOutput.getTripNum();
										String latGPS = currentOutput.getLatGPS();
										String lonGPS = currentOutput.getLonGPS();
										String distanceToShape = currentOutput.getDistance();

										addOutput(currentRoute, tripNum, currentShapeId, currentShapeSequence,
												currentLatShape, currentLonShape, currentDistanceTraveled, busCode,
												gpsPointId, latGPS, lonGPS, distanceToShape, currentGPSDateTime,
												problemCode, listOutput);

									} else {
										addOutput(currentRoute, tripNum, currentShapeId, currentShapeSequence,
												currentLatShape, currentLonShape, currentDistanceTraveled, "-", "-",
												"-", "-", "-", "-", "-", listOutput);
									}
								} else {

									if (bulmaOutputGrouping.containsShapeSequence(currentShapeSequence)) {
										BulmaBusteOutput currentOutput = bulmaOutputGrouping.getMapOutputGrouping()
												.get(currentShapeSequence);

										String busCode = currentOutput.getBusCode();
										String gpsPointId = currentOutput.getGpsPointId();
										String problemCode = currentOutput.getTripProblem();
										tripNum = currentOutput.getTripNum();
										String latGPS = currentOutput.getLatGPS();
										String lonGPS = currentOutput.getLonGPS();
										String distanceToShape = currentOutput.getDistance();
										currentTimestamp = currentOutput.getTimestamp();
										currentGPSDateTime = currentOutput.getGps_datetime();

										nextPoint = new Tuple2<Float, String>(
												currentShapePoint.getDistanceTraveled(), currentTimestamp);

										generateOutputFromPointsInBetween(currentShapeId, tripNum, previousPoint,
												pointsBetweenGPS, nextPoint, shapeLine.getListGeoPoints(), busCode,
												listOutput, previousDate);

										addOutput(currentRoute, tripNum, currentShapeId, currentShapeSequence,
												currentLatShape, currentLonShape, currentDistanceTraveled, busCode,
												gpsPointId, latGPS, lonGPS, distanceToShape, currentGPSDateTime,
												problemCode, listOutput);

										previousPoint = nextPoint;
										nextPoint = null;
										pointsBetweenGPS = new LinkedList<Integer>();

									} else {
										pointsBetweenGPS.add(i);
									}
								}
							}

							if (!pointsBetweenGPS.isEmpty()) {
								for (Integer indexPointInBetween : pointsBetweenGPS) {

									ShapePoint currentShapePoint = (ShapePoint) shapeLine.getListGeoPoints()
											.get(indexPointInBetween);
									String currentShapeSequence = currentShapePoint.getPointSequence();
									String currentDistanceTraveled = currentShapePoint.getDistanceTraveled().toString();
									String currentShapeId = shapeLine.getId();

									String currentLatShape = currentShapePoint.getLatitude();
									String currentLonShape = currentShapePoint.getLongitude();
									String currentRoute = shapeLine.getRoute();

									addOutput(currentRoute, tripNum, currentShapeId, currentShapeSequence,
											currentLatShape, currentLonShape, currentDistanceTraveled, "-", "-", "-",
											"-", "-", "-", "-", listOutput);

								}
							}
						}

						return listOutput.iterator();
					}

					private void addOutput(String route, String tripNum, String shapeId, String shapeSequence,
							String shapeLat, String shapeLon, String distanceTraveledShape, String busCode,
							String gpsPointId, String gpsLat, String gpsLon, String distanceToShapePoint,
							String gps_date_time, String problemCode, List<String> listOutput) {

						String stopPointId = mapStopPoints.get(shapeSequence);
						mapAux.remove(shapeSequence);
						if (stopPointId != null) {
							String problem;

							try {
								problem = Problem.getById(Integer.valueOf(problemCode));
							} catch (Exception e) {
								problem = "BETWEEN";
							}

							String outputString = route + SEPARATOR + tripNum + SEPARATOR + shapeId + SEPARATOR
									+ shapeSequence + SEPARATOR + shapeLat + SEPARATOR + shapeLon + SEPARATOR
									+ distanceTraveledShape + SEPARATOR + busCode + SEPARATOR + gpsPointId + SEPARATOR
									+ gpsLat + SEPARATOR + gpsLon + SEPARATOR + distanceToShapePoint + SEPARATOR
									+ gps_date_time + SEPARATOR + stopPointId + SEPARATOR + problem;

							listOutput.add(outputString);
						}

					}

					private void generateOutputFromPointsInBetween(String shapeId, String tripNum,
							Tuple2<Float, String> previousGPSPoint, List<Integer> pointsBetweenGPS,
							Tuple2<Float, String> nextGPSPoint, List<GeoPoint> listGeoPointsShape, String busCode,
							List<String> listOutput, String previousDate) throws ParseException {

						Float previousDistanceTraveled = previousGPSPoint._1;
						long previousTime = getTimeLong(previousGPSPoint._2);
						Float nextDistanceTraveled = nextGPSPoint._1;
						long nextTime = getTimeLong(nextGPSPoint._2);
						Float distanceTraveled = nextDistanceTraveled - previousDistanceTraveled;
						long time = nextTime - previousTime;
						String gpsDateTime;

						Float currentDistanceTraveled;
						long generatedTimeDifference;
						long generatedTime;
						String generatedTimeString;
						String sequence;
						String distance;
						String latShape;
						String lonShape;
						String route;
						for (Integer indexPointsInBetween : pointsBetweenGPS) {

							currentDistanceTraveled = ((ShapePoint) listGeoPointsShape.get(indexPointsInBetween)).getDistanceTraveled()
									- previousDistanceTraveled;
							generatedTimeDifference = (long) ((currentDistanceTraveled * time) / distanceTraveled);
							generatedTime = previousTime + generatedTimeDifference;
							generatedTimeString = getTimeString(generatedTime);
							gpsDateTime = previousDate + " " + generatedTimeString;
							sequence = ((ShapePoint) listGeoPointsShape.get(indexPointsInBetween)).getPointSequence();
							latShape = listGeoPointsShape.get(indexPointsInBetween).getLatitude();
							lonShape = listGeoPointsShape.get(indexPointsInBetween).getLongitude();
							route = ((ShapePoint) listGeoPointsShape.get(indexPointsInBetween)).getRoute();
							distance = ((ShapePoint) listGeoPointsShape.get(indexPointsInBetween)).getDistanceTraveled().toString();

							addOutput(route, tripNum, shapeId, sequence, latShape, lonShape, distance, busCode, "-",
									"-", "-", "-", gpsDateTime, "-", listOutput);
						}
					}

					private String getTimeString(long generatedTime) {
						Date date = new Date(generatedTime);
						DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
						return formatter.format(date);
					}

					private long getTimeLong(String timestamp) throws ParseException {
						SimpleDateFormat parser = new SimpleDateFormat("HH:mm:ss");
						return parser.parse(timestamp).getTime();
					}
				});

		JavaPairRDD<String, Iterable<OutputString>> rddMapInterpolation = rddInterpolation
				.mapToPair(new PairFunction<String, String, OutputString>() {

					public Tuple2<String, OutputString> call(String stringOutput) throws Exception {
						String[] splittedEntry = stringOutput.split(SEPARATOR);
						OutputString str = new OutputString(stringOutput);
						
						return new Tuple2<String, OutputString>(splittedEntry[7], str);
					}
				}).groupByKey();
		
		
		
		JavaRDD<String> rddOutput = rddMapInterpolation
				.flatMap(new FlatMapFunction<Tuple2<String,Iterable<OutputString>>, String>() {

					public Iterator<String> call(Tuple2<String, Iterable<OutputString>> busCode_stringOutput) throws Exception {

						List<String> listOutput = new LinkedList<String>();
						List<OutputString> OutputList = Lists.newArrayList(busCode_stringOutput._2);									
						
						Collections.sort(OutputList);
						
												
						String currentBusCode = busCode_stringOutput._1;
						String nextTimeString = null;
						for (int i = OutputList.size() - 1; i >= 0; i--) {
							String currentString = OutputList.get(i).getOutputString();
							String currentBusStop = currentString.split(SEPARATOR)[13];

							if (!currentBusStop.equals("-")) {
								String currentTimeString = currentString.split(SEPARATOR)[12];
								if (!currentTimeString.equals("-")) {
									currentTimeString = currentTimeString.split(" ")[1];
									if (nextTimeString == null) {
										nextTimeString = currentTimeString;
										listOutput.add(0, currentString + SEPARATOR + "-" + SEPARATOR + "-" + SEPARATOR
												+ "-" + SEPARATOR + "-" + SEPARATOR + "-" + SEPARATOR + "-");

									} else {
										listOutput.add(0, currentString + SEPARATOR + "-" + SEPARATOR + "-"
													+ SEPARATOR + "-" + SEPARATOR + "-" + SEPARATOR + "-" + SEPARATOR + "-");

										nextTimeString = currentTimeString;

									}
								} else {									
									listOutput.add(0, currentString + SEPARATOR + "-" + SEPARATOR + "-" + SEPARATOR
											+ "-" + SEPARATOR + "-" + SEPARATOR + "-" + SEPARATOR + "-");
								}

							}

						}

						return listOutput.iterator();
					}

				});

		return rddOutput;

//		return rddMapInterpolation;

	}
	
	/**
	 * Gets the previous date based on date passed as parameter
	 * 
	 * @param stringDate
	 * 	The current date
	 * @return
	 * 	The previous date
	 * @throws ParseException
	 */
	public static String subtractDay(String stringDate) throws ParseException {

		DateFormat targetFormat = new SimpleDateFormat("yyyy_MM_dd", Locale.ENGLISH);
		Date date = targetFormat.parse(stringDate);
		
	    Calendar cal = Calendar.getInstance();
	    cal.setTime(date);
	    cal.add(Calendar.DAY_OF_MONTH, -1);
	    
	    DateFormat originalFormat = new SimpleDateFormat("EEE MMM dd kk:mm:ss z yyyy", Locale.ENGLISH);
	    Date newDate = originalFormat.parse(cal.getTime().toString());
	    String formattedDate = targetFormat.format(newDate); 
	    
	    
	    return formattedDate;
	}

}
