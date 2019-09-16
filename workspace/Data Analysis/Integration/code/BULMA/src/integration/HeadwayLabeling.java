package integration;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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

import BULMADependences.AlertData;
import BULMADependences.JamData;
import BULMADependences.OutputString;
import PointDependencies.GeoPoint;
import scala.Tuple2;

/**
 * 
 * THIRD CODE:
 * Find the CONSECUTIVE bus (with less difference in time at the same stop), calculating the headway and the distance to it.
 * 
 * @input route, trip_number/no_shape_code, shape_id/-, route_frequency/-, shape_sequence/-, shape_lat/-, shape_lon/-, 
 *		distance_traveled, bus_code, gps_id, gps_lat, gps_lon, distance_to_shape_point/-, gps_timestamp,  stop_id, trip_problem_code,
 *		<weather_data>, <waze_data>
 *
 * @output route, trip_number/no_shape_code, shape_id/-, route_frequency/-, shape_sequence/-, shape_lat/-, shape_lon/-, 
 *		distance_traveled, bus_code, gps_id, gps_lat, gps_lon, distance_to_shape_point/-, gps_timestamp,  stop_id, trip_problem_code,
 *		<weather_data>, <waze_data>, headway, headwayThreshold, busBunching, distance_between_buses, GPShour, <second_bus_data>
 *  89 VARIABLES
 *  
 * @author veruska
 *
 */

public class HeadwayLabeling {

	private static final String SEPARATOR = ",";
	private static final String SLASH = "/";
	private static final int BB_THRESHOLD = 5; // headway = 5 minutes is considered bb
	private static final String OUTPUT_HEADER = "route,tripNum,shapeId,routeFrequency,shapeSequence,shapeLat,shapeLon,distanceTraveledShape,"
			+ "busCode,gpsPointId,gpsLat,gpsLon,distanceToShapePoint,gps_datetime,stopPointId,problem,precipitation,precipitationTime,precipitationStationDistance,alertDateTime,alertSubtype,alertType,"
			+ "alertRoadType,alertConfidence,alertNComments,alertNImages,alertNThumbsUp,alertReliability,alertReportMood,alertReportRating,alertSpeed,alertLatitude,"
			+ "alertLongitude,alertDistanceToClosestShapePoint,alertIsJamUnifiedAlert,alertInScale,jamUpdateDateTime,jamExpirationDateTime,jamBlockType,"
			+ "jamDelay,jamLength,jamLevel,jamSeverity,jamSpeedKM,headway,headwayThreshold,busBunching,GPShour,"
			+ "tripNumSB,shapeSequenceSB,shapeLatSB,shapeLonSB,distanceTraveledShapeSB,busCodeSB,gpsPointIdSB,gpsLatSB,gpsLonSB,distanceToShapePointSB,gps_datetimeSB,"
			+ "stopPointIdSB,problemSB,precipitationSB,precipitationTimeSB,precipitationStationDistanceSB,alertDateTimeSB,alertSubtypeSB,alertTypeSB,alertRoadTypeSB,alertConfidenceSB,alertNCommentsSB,"
			+ "alertNImagesSB,alertNThumbsUpSB,alertReliabilitySB,alertReportMoodSB,alertReportRatingSB,alertSpeedSB,alertLatitudeSB,alertLongitudeSB,"
			+ "alertDistanceToClosestShapePointSB,alertIsJamUnifiedAlertSB,alertInScaleSB,jamUpdateDateTimeSB,jamExpirationDateTimeSB,jamBlockTypeSB,jamDelaySB,jamLengthSB,"
			+ "jamLevelSB,jamSeveritySB,jamSpeedKMSB";
	
	//input variables index
	private static int gpsRoute = 0;
	private static int gpsTripNum = 1;
	private static int gpsShapeId = 2;
	private static int gpsRouteFrequency = 3;
	private static int gpsShapeSequence = 4;
	private static int gpsLatShape = 5;
	private static int gpsLonShape = 6;
	private static int gpsDistanceTraveled = 7;
	private static int gpsBusCode = 8;
	private static int gpsPointId = 9;
	private static int gpsLat = 10;
	private static int gpsLon = 11;
	private static int gpsDistanceToShapePoint = 12;
	private static int gpsTimestamp = 13;
	private static int gpsStopID = 14;
	private static int gpsTripProblem = 15;
	private static int gpsPrecipitation = 16;
	private static int gpsPrecipitationTime = 17;
	private static int gpsPrecipitationStationDistance = 18;
	
	private static int wazePublicationTime = 19;
	private static int wazeSubtype = 20;
	private static int wazeType = 21;
	private static int wazeRoadType = 22;
	private static int wazeConfidence = 23;
	private static int wazeNComments = 24;
	private static int wazeNImages = 25;
	private static int wazeNThumbsUp = 26;
	private static int wazeReliability = 27;
	private static int wazeReportMood = 28;
	private static int wazeReportRating = 29;
	private static int wazeSpeed = 30;
	private static int wazeLatitude = 31;
	private static int wazeLongitude = 32;
	private static int wazeDistanceToClosShapePoint = 33;
	private static int wazeIsJamUnifiedAlert = 34;
	private static int wazeInScale = 35;

	private static int jamUpdateDateTime = 36;
	private static int jamExpirationDateTime = 37;
	private static int jamBlockType = 38;
	private static int jamDelay = 39;
	private static int jamLength = 40;
	private static int jamLevel = 41;
	private static int jamSeverity = 42;
	private static int jamSpeedKMH = 43;

	private static HashMap<String, HashMap<String, Long>> scheduledHeadwaysMap = new HashMap<String, HashMap<String, Long>>();

	public static void main(String[] args) throws IOException, URISyntaxException, ParseException {

		if (args.length < 5) {
			System.err.println("Usage: <city> <integrated data directory> <GTFS path> <output path> <number of partitions>");
			System.exit(1);
		}

		Long initialTime = System.currentTimeMillis();

		String city = args[0];
		String busteOutputPath = args[1] + city + "/";
		String GTFSFilePath = args[2] + city + "/";
		String stopTimesShapesPath = GTFSFilePath + "stop_times_shapes.txt";
		String outputPath = args[3] + city + "/";
		final Integer minPartitions = Integer.valueOf(args[4]);

		SparkConf sparkConf = new SparkConf().setAppName("HeadwayLabeling").setMaster("local");
		// SparkConf sparkConf = new SparkConf().setAppName("HeadwayLabeling"); // to run on cluster
		JavaSparkContext context = new JavaSparkContext(sparkConf);

		generateOutputFilesHDFS(context, busteOutputPath, stopTimesShapesPath, outputPath, city, minPartitions);

		context.stop();
		context.close();
		System.out.println(city + " - Headway Labeling \nExecution time: " + TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - initialTime) + " min");
	}

	private static void generateOutputFilesHDFS(JavaSparkContext context, String pathBusteOutput, String stopTimesShapesPath,
			String output, String city, int minPartitions) throws IOException, URISyntaxException, ParseException {

		/**
		 * Removes empty lines and header from file
		 * 
		 * @return the file without the empty lines and header
		 */
		Function2<Integer, Iterator<String>, Iterator<String>> removeEmptyLinesAndHeader = new Function2<Integer, Iterator<String>, Iterator<String>>() {

			private static final long serialVersionUID = -4475494148847393258L;

			public Iterator<String> call(Integer index, Iterator<String> iterator) throws Exception {
				List<String> output = new LinkedList<String>();
				String line;
				
				if (index == 0) { //skip header
					line = iterator.next();
				}
				
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
		FileSystem fs = FileSystem.get(new URI(pathBusteOutput), conf);
		FileStatus[] fileStatus = fs.listStatus(new Path(pathBusteOutput));

		// For each folder (day), get the outputs
		for (FileStatus file : fileStatus) {

			String dirName = file.getPath().getName();
			if (dirName.contains("Integrated_Data")) {

				String dailyPathDir = pathBusteOutput + SLASH + dirName;
				FileStatus[] fileStatusDaily = fs.listStatus(new Path(dailyPathDir));

				JavaRDD<String> busteOutputString = context.textFile(dailyPathDir + SLASH + "part-00000");

				// Join all the output in the same RDD of the same day
				for (FileStatus filePart : fileStatusDaily) {
					if (!filePart.getPath().getName().equals("_SUCCESS")
							&& !filePart.getPath().getName().equals("part-00000")) {
						busteOutputString = busteOutputString
								.union(context.textFile(dailyPathDir + SLASH + filePart.getPath().getName()));

						System.out.println(dailyPathDir + SLASH + filePart.getPath().getName());
					}
				}
				busteOutputString = busteOutputString.mapPartitionsWithIndex(removeEmptyLinesAndHeader, false);

				String stringDate = dirName.substring(dirName.lastIndexOf("_") + 1, dirName.length());

				JavaRDD<String> result = execute(context, busteOutputString, stopTimesShapesPath, output, stringDate, city, minPartitions);

				/**
				 * Inserts a header into each output file
				 * 
				 * @return the output file with a new header
				 */
				Function2<Integer, Iterator<String>, Iterator<String>> insertHeader = new Function2<Integer, Iterator<String>, Iterator<String>>() {

					private static final long serialVersionUID = 6196875196870694185L;

					@SuppressWarnings("unchecked")
					public Iterator<String> call(Integer index, Iterator<String> iterator) throws Exception {
						List<String> output = new LinkedList<String>();
						output.add(OUTPUT_HEADER);
						output.addAll(IteratorUtils.toList(iterator));

						return output.iterator();
					}
				};

				result.mapPartitionsWithIndex(insertHeader, false).saveAsTextFile(output + SLASH + "output_" + stringDate);
			}
		}
	}

	@SuppressWarnings("serial")
	private static JavaRDD<String> execute(JavaSparkContext context, JavaRDD<String> busteOutputString,
			String stopTimesShapesPath, String outputPath, String stringDate, final String city, int minPartitions) {
		
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
		
		// route, trip_number/no_shape_code, shape_id/-, route_frequency/-,
		// shape_sequence/-, shape_lat/-, shape_lon/-,
		// distance_traveled, bus_code, gps_id, gps_lat, gps_lon,
		// distance_to_shape_point/-, gps_timestamp, stop_id, trip_problem_code

		// Grouping Integrated data by route-stopID
		JavaPairRDD<String, Iterable<OutputString>> rddIntegratedDataGrouped = busteOutputString
				.mapToPair(new PairFunction<String, String, OutputString>() {

					public Tuple2<String, OutputString> call(String string) throws Exception {
						String bulmaOutputString = string.replaceAll(",,", ",-,");
						String[] stringSplitted = bulmaOutputString.split(SEPARATOR);
						
						
						AlertData alert = null;
						if (!stringSplitted[wazePublicationTime].equals("-")) {
							alert = new AlertData(stringSplitted[wazePublicationTime], stringSplitted[wazeSubtype], stringSplitted[wazeType], 
									stringSplitted[wazeRoadType], stringSplitted[wazeConfidence], stringSplitted[wazeNComments], stringSplitted[wazeNImages], 
									stringSplitted[wazeNThumbsUp], stringSplitted[wazeReliability], stringSplitted[wazeReportMood], stringSplitted[wazeReportRating], 
									stringSplitted[wazeSpeed], stringSplitted[wazeLatitude], stringSplitted[wazeLongitude], stringSplitted[wazeDistanceToClosShapePoint],
									stringSplitted[wazeIsJamUnifiedAlert], stringSplitted[wazeInScale]);
						}
						
						JamData jam = null;
						if (!stringSplitted[jamUpdateDateTime].equals("-")) {
							jam = new JamData(stringSplitted[jamUpdateDateTime], stringSplitted[jamExpirationDateTime], stringSplitted[jamBlockType], 
									stringSplitted[jamDelay], stringSplitted[jamLength], stringSplitted[jamLevel], stringSplitted[jamSeverity], 
									stringSplitted[jamSpeedKMH]);
						}
						
						OutputString integratedData = new OutputString(stringSplitted[gpsRoute], stringSplitted[gpsTripNum], stringSplitted[gpsShapeId],
								stringSplitted[gpsRouteFrequency], stringSplitted[gpsShapeSequence], stringSplitted[gpsLatShape], stringSplitted[gpsLonShape], 
								stringSplitted[gpsDistanceTraveled], stringSplitted[gpsBusCode], stringSplitted[gpsPointId], stringSplitted[gpsLat], 
								stringSplitted[gpsLon], stringSplitted[gpsDistanceToShapePoint], stringSplitted[gpsTimestamp], stringSplitted[gpsStopID], 
								stringSplitted[gpsTripProblem], stringSplitted[gpsPrecipitation], stringSplitted[gpsPrecipitationTime], stringSplitted[gpsPrecipitationStationDistance],
								alert, jam);

						String stopID = integratedData.getStopID();
						String routeStopIDKey = integratedData.getRoute() + ":" + integratedData.getShapeId() + ":" + stopID;

						return new Tuple2<String, OutputString>(routeStopIDKey, integratedData);
					}
				}).groupByKey(minPartitions);

		
		JavaRDD<String> busStopsString = context.textFile(stopTimesShapesPath, minPartitions)
				.mapPartitionsWithIndex(removeHeader, false);
		
		// Bus stops grouped by route and stopID
		JavaPairRDD<String, Iterable<String>> rddBusStops = busStopsString.mapToPair(new PairFunction<String, String, String>() {

			public Tuple2<String, String> call(String busStopsString) throws Exception {
				String[] splittedEntry = busStopsString.split(SEPARATOR);
				String route = splittedEntry[6].replace(" ", "");
				String shapeID = splittedEntry[7].replace(" ", "");
				String stopID = splittedEntry[2].replace(" ", "");
				String arrivalTime = splittedEntry[0].replace(" ", "");
				
				// route:stopId
				return new Tuple2<String, String>(route + ":" + shapeID + ":" + stopID, arrivalTime);
			}
		}).groupByKey(minPartitions);
		
		
		/**
		 * Calculate headway of bus stops and grouped by route
		 * 
		 * @return Grouped route-shape, stops - arrivals - headway
		 * 
		 */
		JavaPairRDD<String, Iterable<Tuple2<String, List<Tuple2<String, Long>>>>> rddBusStopsGrouped = rddBusStops.mapToPair(new 
				PairFunction<Tuple2<String,Iterable<String>>, String, Tuple2<String, List<Tuple2<String, Long>>>>() {

			@Override
			public Tuple2<String, Tuple2<String, List<Tuple2<String, Long>>>> call(Tuple2<String, Iterable<String>> routeStopID_arrivalTimes) 
					throws Exception {
				String routeShapeStopIDKey = routeStopID_arrivalTimes._1; // route:stopId
				
				//arrivalTimes-headway
				List<Tuple2<String, Long>> arrivalTimesHeadwayMap = new ArrayList<>();
				
				// GTFS of Recife is out of date, there is not 2018 date in calendar file
				// So we will remove duplicate times
				// if some scheduled headway is too small, it is because probably they belongs to different days
				if (city.equals("Recife")) {
					Set<String> arrivalTimesSet = new HashSet<String>();
					for (String arrivalTime : routeStopID_arrivalTimes._2) {
						arrivalTimesSet.add(arrivalTime);
					}
					List<String> arrivalTimesList =  new ArrayList<String>(arrivalTimesSet);
					Collections.sort(arrivalTimesList);
					
					for (int i = 0; i < arrivalTimesList.size()-2; i++) {
						String firstArrival = arrivalTimesList.get(i);
						String secondArrival = arrivalTimesList.get(i+1);
						long headway = GeoPoint.getTimeDifference(firstArrival, secondArrival); //in minutes
						String firstArrivalSecondArrivalKey = firstArrival.replaceAll(":", "") + "_" + secondArrival.replaceAll(":", "");
						
						arrivalTimesHeadwayMap.add(new Tuple2<String, Long>(firstArrivalSecondArrivalKey, headway));
						
						if (!scheduledHeadwaysMap.containsKey(routeShapeStopIDKey)) {
							scheduledHeadwaysMap.put(routeShapeStopIDKey, new HashMap<String,Long>());
						}
						scheduledHeadwaysMap.get(routeShapeStopIDKey).put(firstArrivalSecondArrivalKey, headway); //firstArrivalTime_secondArrivalTime
					}
					
					if (arrivalTimesList.size() == 2) {
						String firstArrival = arrivalTimesList.get(0);
						String secondArrival = arrivalTimesList.get(1);
						long headway = GeoPoint.getTimeDifference(firstArrival, secondArrival);
						String firstArrivalSecondArrivalKey = firstArrival.replaceAll(":", "") + "_" + secondArrival.replaceAll(":", "");
						
						arrivalTimesHeadwayMap.add(new Tuple2<String, Long>(firstArrivalSecondArrivalKey, headway));
						
						if (!scheduledHeadwaysMap.containsKey(routeShapeStopIDKey)) {
							scheduledHeadwaysMap.put(routeShapeStopIDKey, new HashMap<String,Long>());
						}
						scheduledHeadwaysMap.get(routeShapeStopIDKey).put(firstArrivalSecondArrivalKey, headway); //firstArrivalTime_secondArrivalTime
					}
					
					String routeShapeKey = routeShapeStopIDKey.split(":")[0] + ":" + routeShapeStopIDKey.split(":")[1];
					
					return new Tuple2<String, Tuple2<String,List<Tuple2<String, Long>>>>(routeShapeKey, 
							new Tuple2<String,List<Tuple2<String, Long>>>(routeShapeStopIDKey.split(":")[2],  arrivalTimesHeadwayMap));
					
				} else {
					//TODO update code to deal with service (same arrival times for different days)
					//manter o trip_id e service em stop_t_s (merge file)
					//pegar o service and trip, olhar em calendar e add a data em stop_t_s (merge file)
					//adicionar a data na chave (aqui)
					//calcular o headway com mesma data
					//ao comparar, ver se está na data
					
					//List<String> arrivalTimesList =  Lists.newArrayList(routeStopID_arrivalTimes._2);
					
					return null;
				}
			}
		}).groupByKey(minPartitions);
		
		 rddBusStopsGrouped.saveAsTextFile(outputPath + SLASH + "scheduled_hd_" + stringDate);
		
		// Calculate the headway between the buses, considering same route, same stop and same day
		// Headway: time difference for the bus that is in front
		JavaRDD<String> rddLabeledIntegratedOutput = rddIntegratedDataGrouped.flatMap(
				new FlatMapFunction<Tuple2<String, Iterable<OutputString>>, String>() {

					public Iterator<String> call(
							Tuple2<String, Iterable<OutputString>> routeStopID_BulmaBusteOutput) throws Exception {
						
						String routeShapeStopID = routeStopID_BulmaBusteOutput._1;
						List<OutputString> listBusteOutput = Lists.newArrayList(routeStopID_BulmaBusteOutput._2);
						Collections.sort(listBusteOutput);

						List<String> labeledIntegratedData = new ArrayList<>();

						for (int i = 0; i < listBusteOutput.size()-2; i++) {// buses of the same route in a same stop
							OutputString currentBusteOutput = listBusteOutput.get(i); // avoid comparison with the same row
							String currentBusCode = currentBusteOutput.getBusCode();
							
							OutputString closestNextBus = null;
							long closestHeadway = Long.MAX_VALUE;

							for (int j = i+1; j < listBusteOutput.size()-1; j++) {
								OutputString nextBusteOutput = listBusteOutput.get(j);
								
								if (nextBusteOutput.getBusCode().equals(currentBusCode)) { // discard buses when the next one is the same (avoid higher headways)
									break;

								} else { // calculate headways just for different buscode
									long currentHeadway = GeoPoint.getTimeDifference(currentBusteOutput.getTimestamp(), nextBusteOutput.getTimestamp());
									if (currentHeadway < closestHeadway) {
										closestNextBus = nextBusteOutput;
										closestHeadway = currentHeadway;
										break; // stop the search, because the list is sorted
									}
								}
							}
							
							// when there is only one bus for the route, we discard it because the bus bunching definition
							if (closestNextBus != null) {
								// closestNextBus = listBusteOutput.get(i+1);
								// closestHeadway = GeoPoint.getTimeDifference(currentBusteOutput.getTimestamp(), closestNextBus.getTimestamp());
							
								//checking bus bunching with scheduled headway
								Long scheduledHeadway = null;
								String[] firstBusTimeSplit = currentBusteOutput.getTimestamp().split(":");
								int firstBusTime =  Integer.valueOf(firstBusTimeSplit[0] + firstBusTimeSplit[1] 
										+ firstBusTimeSplit[2]);
								
								String[] secondBusTimeSplit = closestNextBus.getTimestamp().split(":");
								int secondBusTime =  Integer.valueOf(secondBusTimeSplit[0] + secondBusTimeSplit[1] 
										+ secondBusTimeSplit[2]);
	
								HashMap<String, Long> arrivalTimesHeadwayMap = scheduledHeadwaysMap.get(routeShapeStopID);
								
								// When there is only one bus in the route (specially)
								if (arrivalTimesHeadwayMap != null) {
									
									for (Entry<String, Long> arrivalTimesHeadway : arrivalTimesHeadwayMap.entrySet()) {
										String[] arrivalTimes = arrivalTimesHeadway.getKey().split("_");
										Long headway = arrivalTimesHeadway.getValue();
										
										int firstArrivalTime = Integer.valueOf(arrivalTimes[0]);
										int secondArrivalTime = Integer.valueOf(arrivalTimes[1]);
										
										//TODO se esse mapa tiver ordenado, flexibilizar os ifs pq pega o primeiro
										//TODO e para horários adiantados do primeiro ônibus e atrasados do segundo ônibus?
										if (firstBusTime >= firstArrivalTime && firstBusTime <= secondArrivalTime
												&& secondBusTime >= firstArrivalTime && secondBusTime <= secondArrivalTime) {// match only when the real times are between the scheduled times
											scheduledHeadway = headway;
											break;
										}
									}
								}
								
								boolean busBunching = true;
								int headwayThreshold = BB_THRESHOLD; // if there is no data, consider a threshold
								
								if (scheduledHeadway != null) {
									headwayThreshold = (int) (scheduledHeadway/4);
								}
								
								if (closestHeadway > headwayThreshold) {
									busBunching = false;
								}
								
								//saving
								currentBusteOutput.setHeadway(closestHeadway);
								currentBusteOutput.setHeadwayThreshold(headwayThreshold);
								currentBusteOutput.setNextBus(closestNextBus);
								currentBusteOutput.setBusBunching(busBunching);
								
								labeledIntegratedData.add(currentBusteOutput.getLabeledIntegratedDataString(true)); //true to replace "-" by ""
							}
						}

						return labeledIntegratedData.iterator();
					}
				});

		return rddLabeledIntegratedOutput;
	}
}
