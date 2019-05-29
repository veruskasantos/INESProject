package integration;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import org.apache.avro.mapred.tether.OutputProtocol;
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
import BULMADependences.WazeData;
import BULMADependences.WeatherData;
import PointDependencies.GeoPoint;
import scala.Tuple2;

public class MatchingGPSWeatherWaze {

	private static final String SEPARATOR = ",";
	private static final String SEPARATOR_WEATHER = ";";
	private static final String SLASH = "/";
	private static Map<String, Tuple2<String, String>> stationCoordinatesMap;
	private static Map<String, List<Tuple2<String, Double>>> stationDataMap;
	private static final String OUTPUT_HEADER = "route,tripNum,shapeId,routeFrequency,shapeSequence,shapeLat,shapeLon,distanceTraveledShape,"
			+ "busCode,gpsPointId,gpsLat,gpsLon,distanceToShapePoint,gps_datetime,stopPointId,problem,precipitationTime,precipitation";
	
	public static void main(String[] args) throws IOException, URISyntaxException, ParseException {

		if (args.length < 6) {
			System.err.println("Usage: <city> <output matchingGSS directory> <precipitation path> <waze path> <output path> <number of partitions>");
			System.exit(1);
		}

		Long initialTime = System.currentTimeMillis();

		String city = args[0];
		String matchingGSSOutputPath = args[1] + city + "/";
		String precipitationPath = args[2] + city + "/";
		String wazePath = args[3];
		String outputPath = args[4] + city + "/";
		final Integer minPartitions = Integer.valueOf(args[5]);

		SparkConf sparkConf = new SparkConf().setAppName("MatchingGPSWeatherWaze").setMaster("local");
		// SparkConf sparkConf = new SparkConf().setAppName("MatchingGPSWeatherWaze"); // to run on cluster
		JavaSparkContext context = new JavaSparkContext(sparkConf);

		generateOutputFilesHDFS(context, matchingGSSOutputPath, outputPath, city, precipitationPath, wazePath, minPartitions);

		context.stop();
		context.close();
		System.out.println("Execution time: " + TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - initialTime) + " min");
	}
	
	private static void generateOutputFilesHDFS(JavaSparkContext context, String matchingGSSOutputPath,
			String output, String city, String precipitationPath, String wazePath, int minPartitions) throws IOException, URISyntaxException, ParseException {

		/**
		 * Removes empty lines and header from
		 *  file
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
					String timestamp = line.split(SEPARATOR)[13];
					
					if (!line.isEmpty() && !timestamp.equals("-")) { //skip empty line and line/shape without gps (the bus did not go to that stop)
						output.add(line);
					}
				}
				
				return output.iterator();
			}
		};

		Configuration conf = new Configuration();
		FileSystem fs = FileSystem.get(new URI(matchingGSSOutputPath), conf);
		FileStatus[] fileStatus = fs.listStatus(new Path(matchingGSSOutputPath));
		
		// For each folder (day), get the outputs
		for (FileStatus file : fileStatus) {

			String dirName = file.getPath().getName();
			if (dirName.contains("BuLMABusTE")) {

				String dailyPathDir = matchingGSSOutputPath + SLASH + dirName;
				FileStatus[] fileStatusDaily = fs.listStatus(new Path(dailyPathDir));

				JavaRDD<String> matchingGSSOutputString = context.textFile(dailyPathDir + SLASH + "part-00000");

				// Join all the output in the same RDD of the same day
				for (FileStatus filePart : fileStatusDaily) {
					if (!filePart.getPath().getName().equals("_SUCCESS")
							&& !filePart.getPath().getName().equals("part-00000")) {
						matchingGSSOutputString = matchingGSSOutputString
								.union(context.textFile(dailyPathDir + SLASH + filePart.getPath().getName()));

						System.out.println(dailyPathDir + SLASH + filePart.getPath().getName());
					}
				}
				matchingGSSOutputString = matchingGSSOutputString.mapPartitionsWithIndex(removeEmptyLinesAndHeader, false);

				String stringDate = dirName.substring(dirName.lastIndexOf("_") + 1, dirName.length());

				JavaRDD<String> result = execute(context, matchingGSSOutputString, city, precipitationPath, wazePath, stringDate, minPartitions);

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
				result.mapPartitionsWithIndex(insertHeader, false).saveAsTextFile(output + SLASH + stringDate);
			}
		}
	}
	
	@SuppressWarnings("serial")
	private static JavaRDD<String> execute(JavaSparkContext context, JavaRDD<String> busteOutputString,  final String city,
			String precipitationPath, String wazePath, String stringDate, int minPartitions) {
		
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
		
		// ---------------------Integration of weather data---------------------
		
		stationCoordinatesMap = new HashMap<>();
		stationDataMap = new HashMap<>();
		String dayPrecipitationPath = precipitationPath + SLASH + city + "_" + stringDate + ".csv";
		
		JavaRDD<String> precipitationString = context.textFile(dayPrecipitationPath, minPartitions)
				.mapPartitionsWithIndex(removeHeader, false);
		
		// Stations data grouped by coordinates
		JavaPairRDD<String, Object> rddPrecipitations = precipitationString.mapToPair(new PairFunction<String, String, Object>() {

			@SuppressWarnings({ "rawtypes", "unchecked" })
			public Tuple2<String, Object> call(String busStopsString) throws Exception {
				String[] splittedEntry = busStopsString.split(SEPARATOR_WEATHER);
				
				WeatherData weatherData = new WeatherData(splittedEntry[1], splittedEntry[4], splittedEntry[5], splittedEntry[6],
						splittedEntry[7]);
				String latitude = weatherData.getLatitude();
				String longitude = weatherData.getLongitude();
				String stationCode = weatherData.getStationCode();
				
				if (!stationCoordinatesMap.containsKey(weatherData.getStationCode())) { //map to search closest station
					stationCoordinatesMap.put(stationCode, new Tuple2<String, String>(latitude, longitude));
				}
				
				String stationTimeCode = stationCode + "_" + weatherData.getTime().substring(0, 2);
				if (!stationDataMap.containsKey(stationTimeCode)) {//map to search closest time
					stationDataMap.put(stationTimeCode, new ArrayList());
				}
				
				//stationCode_time(0,2), list<time, precipitation>
				stationDataMap.get(stationTimeCode).add(
						new Tuple2<String, Double>(weatherData.getTime(), weatherData.getPrecipitation()));
				
				// hour
				String timeKey = weatherData.getTime().substring(0, 2);
				System.out.println(timeKey);
				return new Tuple2<String, Object>(timeKey, weatherData);
			}
		});
		
		// Grouping MatchingGSS output by route-stopID
		JavaPairRDD<String, Object> rddBusteOutputGrouped = busteOutputString
				.mapToPair(new PairFunction<String, String, Object>() {

					public Tuple2<String, Object> call(String bulmaOutputString) throws Exception {
						StringTokenizer st = new StringTokenizer(bulmaOutputString, SEPARATOR);
						BulmaBusteOutput busteOutput = new BulmaBusteOutput(st.nextToken(), st.nextToken(),
								st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken(),
								st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken(),
								st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken(), "buste");

						String timeKey = busteOutput.getTimestamp().substring(0, 2);
						System.out.println(timeKey);
						return new Tuple2<String, Object>(timeKey, busteOutput);
					}
				});
				
		JavaPairRDD<String, Iterable<Object>> rddGroupedPrecipitation = rddPrecipitations.union(rddBusteOutputGrouped)
						.groupByKey(minPartitions);
		
		/**
		 * Matching precipitation with gps and gtfs data
		 */
		JavaRDD<String> precipitationOutput = rddGroupedPrecipitation.flatMap(new FlatMapFunction<Tuple2<String,Iterable<Object>>, String>() {

			private List<BulmaBusteOutput> bulmaBusteOutputList;
			private List<String> output;
			
			@Override
			public Iterator<String> call(Tuple2<String, Iterable<Object>> latLonKey_objects)
					throws Exception {
				
				bulmaBusteOutputList = new ArrayList<BulmaBusteOutput>();
				output = new ArrayList<>();
				
				List<Object> listInput = Lists.newArrayList(latLonKey_objects._2);
				for (Object obj : listInput) {
					if (obj instanceof BulmaBusteOutput) {
						bulmaBusteOutputList.add((BulmaBusteOutput)obj);
					}
				}
				
				for (BulmaBusteOutput matchingGP3S : bulmaBusteOutputList) {
					
					Double latGP3S = Double.valueOf(matchingGP3S.getLatShape());
					Double lonGP3S = Double.valueOf(matchingGP3S.getLonShape());
					String timeGP3S = matchingGP3S.getTimestamp();
					
					String closestStation = null;
					double closestDistance = Double.MAX_VALUE;
					
					// finding closest station
					for (Entry<String, Tuple2<String, String>> stationCoordinates : stationCoordinatesMap.entrySet()) {
						String stationCode = stationCoordinates.getKey();
						Double stationLat = Double.valueOf(stationCoordinates.getValue()._1);
						Double stationLon = Double.valueOf(stationCoordinates.getValue()._2);
						
						double currentDistance = GeoPoint.getDistanceInMeters(latGP3S, lonGP3S, stationLat, stationLon);
						if (currentDistance < closestDistance) {
							closestStation = stationCode;
						}
					}
					
					//finding closest time data from closest station
					String stationGPSTimeCurrent = closestStation + "_" + timeGP3S.substring(0, 2);
					
					Long closestTime = Long.MAX_VALUE;
					Tuple2<String, Double> closestTimePrecipitation = null;
					//Comparing just with the same hour, because possible border errors are at most one
					for (Tuple2<String, Double> stationData : stationDataMap.get(stationGPSTimeCurrent)) {
						String timeKey = stationData._1;
						Double precipitation = stationData._2;
						
						long currentDifferenceTime = Math.abs(GeoPoint.getTimeDifference(timeGP3S, timeKey)); //check the abs value
						if (currentDifferenceTime < closestTime) {
							closestTimePrecipitation = new Tuple2<String, Double>(timeKey, precipitation);
						}
					}
					
					//add to output
					String outputString = matchingGP3S.getRoute() + SEPARATOR + matchingGP3S.getTripNum() + SEPARATOR + matchingGP3S.getShapeId() + 
							SEPARATOR + matchingGP3S.getRouteFrequency() + SEPARATOR + matchingGP3S.getShapeSequence() + SEPARATOR + matchingGP3S.getLatShape() + 
							SEPARATOR + matchingGP3S.getLonShape() + SEPARATOR + matchingGP3S.getDistance() + SEPARATOR + matchingGP3S.getBusCode() + SEPARATOR + 
							matchingGP3S.getGpsPointId() + SEPARATOR + matchingGP3S.getLatGPS() + SEPARATOR + matchingGP3S.getLonGPS() + SEPARATOR + 
							matchingGP3S.getDistanceToShapePoint() + SEPARATOR + matchingGP3S.getGps_datetime() + SEPARATOR + matchingGP3S.getStopID() + SEPARATOR +
							matchingGP3S.getTripProblem() + SEPARATOR + closestTimePrecipitation._1 + SEPARATOR + String.valueOf(closestTimePrecipitation._2);
					
					output.add(outputString);
					
				}
				return output.iterator(); //TODO SEND TO NEXT RDD, WAZE DATA
			}
		});
//		precipitationOutput.saveAsTextFile("D:/Desktop/UFCG/Projeto INES/INESProject/workspace/Data Analysis/Integration/code/BULMA/data/output/Recife/weather_output");
		
//		precipitationOutput.saveAsTextFile("/home/veruska/Documentos/Projeto_INES/INESProject/workspace/Data Analysis/Integration/code/BULMA/data/output/Recife/weather_output");
		
		// ---------------------Integration of waze data---------------------
		String dayWazePath = wazePath + SLASH + "waze_" + city + "_";
		
		//
		JavaRDD<String> alertsString = context.textFile(dayWazePath + "alerts_" + stringDate + ".csv", minPartitions)
				.mapPartitionsWithIndex(removeHeader, false);
		
		//1km
		JavaRDD<String> jamsString = context.textFile(dayWazePath + "jams_" + stringDate + ".csv", minPartitions)
				.mapPartitionsWithIndex(removeHeader, false);
		
		// 
		JavaPairRDD<String, Iterable<Object>> rddAlertsData = alertsString.mapToPair(new PairFunction<String, String, Object>() {

			public Tuple2<String, Object> call(String alertsString) throws Exception {
				String[] splittedEntry = alertsString.split(SEPARATOR);
				WazeData alerts = new WazeData(splittedEntry[0], splittedEntry[3], splittedEntry[6], splittedEntry[7],
						splittedEntry[8], splittedEntry[10], splittedEntry[11], splittedEntry[12], splittedEntry[13], 
						splittedEntry[14], splittedEntry[16], splittedEntry[17], splittedEntry[18], splittedEntry[10], 
						splittedEntry[22], splittedEntry[23], splittedEntry[26]);

				String hourKey = null;
				
				// route:stopId
				return new Tuple2<String, Object>(hourKey, alerts);
			}
		}).groupByKey(minPartitions); 
		
		// 
		JavaPairRDD<String, Iterable<Object>> rddJamsData = jamsString.mapToPair(new PairFunction<String, String, Object>() {

			public Tuple2<String, Object> call(String jamsString) throws Exception {
				String[] splittedEntry = jamsString.split(SEPARATOR);
				WazeData jams = new WazeData(splittedEntry[0], splittedEntry[3], splittedEntry[6], splittedEntry[7],
						splittedEntry[10], splittedEntry[11], splittedEntry[13], splittedEntry[17], splittedEntry[19], 
						splittedEntry[20], splittedEntry[21]);
				
				String hourKey = null;
				
				// route:stopId
				return new Tuple2<String, Object>(hourKey, jams);
			}
		}).groupByKey(minPartitions); 
		
		//read each one by date
		//group both by hour
		
		return null;
		
	}
}
