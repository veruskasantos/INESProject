package integration;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
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
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.api.java.function.PairFunction;

import com.clearspring.analytics.util.Lists;

import BULMADependences.AlertData;
import BULMADependences.JamData;
import BULMADependences.OutputString;
import BULMADependences.WeatherData;
import PointDependencies.GeoPoint;
import scala.Tuple2;

/**
 * SECOND CODE:
 * Matching GPS, Weather and Waze data.
 * To match gps and weather data is considered the closest weather data, without threshold.
 * To match gps and waze data is considered the closest waze data not exceeding 1km. 
 * 
 * @input route, trip_number/no_shape_code, shape_id/-, route_frequency/-, shape_sequence/-, shape_lat/-, shape_lon/-, 
 *		distance_traveled, bus_code, gps_id, gps_lat, gps_lon, distance_to_shape_point/-, gps_timestamp,  stop_id, street_name, trip_problem_code
 *
 * @output route, trip_number/no_shape_code, shape_id/-, route_frequency/-, shape_sequence/-, shape_lat/-, shape_lon/-, 
 *		distance_traveled, bus_code, gps_id, gps_lat, gps_lon, distance_to_shape_point/-, gps_timestamp,  stop_id, trip_problem_code,
 *		<weather_data>, <waze_data>
 * 
 * @author veruska
 *
 */
public class MatchingGPSWeatherWaze {

	private static final String SEPARATOR = ",";
	private static final String SEPARATOR_WEATHER = ";";
	private static final String SEPARATOR_WAZE = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";
	private static final String SLASH = "/";
	private static final int ONE_HOUR = 60; //60 minutes
	private static final int TEN_MINUTES = 10; 
	//TODO 1 - take only CLOSEST waze data from the streets where buses pass
	private static Map<String, Tuple2<String, String>> stationCoordinatesMap;
	private static Map<String, List<Tuple2<String, Double>>> stationDataMap;
	private static final String OUTPUT_HEADER = "route,tripNum,shapeId,routeFrequency,shapeSequence,shapeLat,shapeLon,distanceTraveledShape,"
			+ "busCode,gpsPointId,gpsLat,gpsLon,distanceToShapePoint,gps_datetime,stopPointId,problem,precipitation,precipitationTime,precipitationStationDistance,alertDateTime,"
			+ "alertSubtype,alertType,alertRoadType,alertConfidence,alertNComments,alertNImages,alertNThumbsUp,alertReliability,alertReportMood,alertReportRating,alertSpeed,alertLatitude,"
			+ "alertLongitude,alertDistanceToClosestShapePoint,alertIsJamUnifiedAlert,alertInScale,jamUpdateDateTime,jamExpirationDateTime,jamBlockType,"
			+ "jamDelay,jamLength,jamLevel,jamSeverity,jamSpeedKM";
	
	//input variables index
	private static int wazeId = 0;
	private static int wazeConfidence = 3;
	private static int wazeInScale = 6;
	private static int wazeIsJamUnifiedAlert = 7;
	private static int wazeLocation = 8;
	private static int wazeNComments = 10;
	private static int wazeNImages = 11;
	private static int wazeNThumbsUp = 12;
	private static int wazePublicationTime = 13;
	private static int wazeReliability = 14;
	private static int wazeReportDescription = 16;
	private static int wazeReportMood = 17;
	private static int wazeReportRating = 18;
	private static int wazeSpeed = 20;
	private static int wazeStreet = 21;
	private static int wazeSubtype = 22;
	private static int wazeType = 23;
	private static int wazeRoadType = 26;
	
	private static int jamId = 0;
	private static int jamDelay = 3;
	private static int jamLength = 6;
	private static int jamLevel = 7;
	private static int jamLineCoordinates = 8;
	private static int jamSeverity = 12;
	private static int jamSpeedKMH = 14;
	private static int jamStreet = 15;
	private static int jamUpdateDateTime = 18;
	private static int jamBlockDescription = 20;
	private static int jamExpirationDateTime = 21;
	private static int jamBlockType = 23;
	
	public final static void main(String[] args) throws IOException, URISyntaxException, ParseException {

		if (args.length < 6) {
			System.err.println("Usage: <city> <output matchingGSS directory> <precipitation path> <waze path> <output path> <number of partitions>");
			System.exit(1);
		}

		Long initialTime = System.currentTimeMillis();

		String city = args[0];
		String matchingGSSOutputPath = args[1] + city + "/";
		String precipitationPath = args[2] + city + "/";
		String wazePath = args[3] + city + "/";
		String outputPath = args[4] + city + "/";
		final Integer minPartitions = Integer.valueOf(args[5]);

		SparkConf sparkConf = new SparkConf().setAppName("MatchingGPSWeatherWaze").setMaster("local");
		// SparkConf sparkConf = new SparkConf().setAppName("MatchingGPSWeatherWaze"); // to run on cluster
		JavaSparkContext context = new JavaSparkContext(sparkConf);

		generateOutputFilesHDFS(context, matchingGSSOutputPath, outputPath, city, precipitationPath, wazePath, minPartitions);

		context.stop();
		context.close();
		System.out.println("MatchingGPS2W - Execution time: " + TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - initialTime) + " min");
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

				JavaRDD<String> result = execute(context, matchingGSSOutputString, city, precipitationPath, wazePath, 
						output, stringDate, minPartitions);

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
				result.mapPartitionsWithIndex(insertHeader, false).saveAsTextFile(output + SLASH + "Integrated_Data_" + stringDate);
			}
		}
	}
	
	@SuppressWarnings("serial")
	private static JavaRDD<String> execute(JavaSparkContext context, JavaRDD<String> busteOutputString,  final String city,
			String precipitationPath, String wazePath, final String outputPath, final String stringDate, int minPartitions) {
		
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
				
				String stationTimeCode = stationCode + "_" + Integer.valueOf(weatherData.getTime().substring(0, 2));
				if (!stationDataMap.containsKey(stationTimeCode)) {//map to search closest time
					stationDataMap.put(stationTimeCode, new ArrayList());
				}
				
				//stationCode_time(0,2), list<time, precipitation>
				stationDataMap.get(stationTimeCode).add(
						new Tuple2<String, Double>(weatherData.getDateTime(), weatherData.getPrecipitation()));
				
				// hour
				String timeKey = weatherData.getTime().substring(0, 2);
				
				return new Tuple2<String, Object>(timeKey, weatherData);
			}
		});
		
		rddPrecipitations.saveAsTextFile(outputPath + SLASH + "precipitation_aux_" + stringDate);
		
		/**
		 * Matching precipitation with gps and gtfs data
		 */
		// Grouping MatchingGSS output by route-stopID
		JavaPairRDD<String, Object> rddMatchedPrecipitation = busteOutputString
				.mapToPair(new PairFunction<String, String, Object>() {

					public Tuple2<String, Object> call(String bulmaOutputString) throws Exception {
						StringTokenizer st = new StringTokenizer(bulmaOutputString, SEPARATOR);
						OutputString matchingGP3S = new OutputString(st.nextToken(), st.nextToken(),
								st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken(),
								st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken(),
								st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken());

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
								closestDistance = currentDistance;
							}
						}
						
						//finding closest time data from closest station
						int currentHour = Integer.valueOf(timeGP3S.substring(0, 2));
						String stationGPSTimeCurrent = closestStation + "_" + currentHour;
						
						Long closestTime = Long.MAX_VALUE;
						Tuple2<String, Double> closestTimePrecipitation = null;
						
						//Comparing just with the same hour, because possible border errors are at most one
						List<Tuple2<String, Double>> listTimes = stationDataMap.get(stationGPSTimeCurrent);
						int otherTime = currentHour;
						while (listTimes == null) { //when there is no data for current time, consider next time or before time
							otherTime++;
							if (otherTime == 24) {
								otherTime--;
							}
							String stationGPSNextTime = closestStation + "_" + otherTime;
							listTimes = stationDataMap.get(stationGPSNextTime);
						}
						
						for (Tuple2<String, Double> stationData : listTimes) {
							String timeKey = stationData._1;
							Double precipitation = stationData._2;
							
							long currentDifferenceTime = Math.abs(GeoPoint.getTimeDifference(timeGP3S, timeKey.split(" ")[1])); //check the abs value
							if (currentDifferenceTime < closestTime) {
								closestTimePrecipitation = new Tuple2<String, Double>(timeKey, precipitation);
								closestTime = currentDifferenceTime;
							}
						}
						
						matchingGP3S.setPrecipitation(closestTimePrecipitation._2);
						matchingGP3S.setPrecipitationTime(closestTimePrecipitation._1.split(" ")[1]);
						matchingGP3S.setPrecipitationDateTime(closestTimePrecipitation._1);
						matchingGP3S.setPrecStationDistanceToGPS(closestDistance);
						
						String latLonKey = String.valueOf(matchingGP3S.getLatShape()).replace(" ",  "").substring(0, 4) + ":" + String.valueOf(matchingGP3S.getLonShape()).replace(" ",  "").substring(0, 5);

						return new Tuple2<String, Object>(latLonKey, matchingGP3S);
					}
				});

		
		// ---------------------Integration of waze data---------------------
		String dayWazePath = wazePath + SLASH + "waze_" + city + "_";
		
		JavaRDD<String> alertsString = context.textFile(dayWazePath + "alerts_" + stringDate + ".csv", minPartitions)
				.mapPartitionsWithIndex(removeHeader, false);
		
		// Grouped by lat:lon 
		JavaPairRDD<String, Object> rddAlertsData = alertsString.mapToPair(new PairFunction<String, String, Object>() {

			public Tuple2<String, Object> call(String alertsString) throws Exception {
				String[] splittedEntry = alertsString.split(SEPARATOR_WAZE); //to deal with comma inside fields
				
				String roadType = "-";
				try {
					roadType = splittedEntry[wazeRoadType];
				} catch (ArrayIndexOutOfBoundsException e) {
					//road type empty
				}
				
				AlertData alert = new AlertData(splittedEntry[wazeId], splittedEntry[wazeConfidence], splittedEntry[wazeInScale], 
						splittedEntry[wazeIsJamUnifiedAlert], splittedEntry[wazeLocation], splittedEntry[wazeNComments], splittedEntry[wazeNImages], 
						splittedEntry[wazeNThumbsUp], splittedEntry[wazePublicationTime], splittedEntry[wazeReliability], splittedEntry[wazeReportDescription], 
						splittedEntry[wazeReportMood], splittedEntry[wazeReportRating], splittedEntry[wazeSpeed], splittedEntry[wazeSubtype], splittedEntry[wazeType], 
						splittedEntry[wazeStreet], roadType);

				// lat:lon:data
				String latLonKey = String.valueOf(alert.getAlertLatitude()).substring(0, 4) + ":" + String.valueOf(alert.getAlertLongitude()).substring(0, 5);

				return new Tuple2<String, Object>(latLonKey, alert);
			}
		});
		
		//Union of precipitation matching with alert data
		JavaPairRDD<String, Iterable<Object>> rddGroupedPrecipitationAlert = rddMatchedPrecipitation.union(rddAlertsData)
				.groupByKey(minPartitions);
		
		/*
		 * Matching between precipitation output and alert data
		 * 
		 * @return each data grouped by hour:day
		 */
		JavaPairRDD<String, Object> rddMatchedAlert = rddGroupedPrecipitationAlert.flatMapToPair(
				new PairFlatMapFunction<Tuple2<String,Iterable<Object>>, String, Object>() {

			List<AlertData> wazeData;
			List<OutputString> precipitationOutput;
			List<Tuple2<String, Object>> alertMatchingOutput;
					
			@Override
			public Iterator<Tuple2<String, Object>> call(Tuple2<String, Iterable<Object>> latLonKey_objects) throws Exception {
				wazeData = new ArrayList<>();
				precipitationOutput = new ArrayList<>();
				alertMatchingOutput = new ArrayList<>();
				
				List<Object> listInput = Lists.newArrayList(latLonKey_objects._2);
				for (Object obj : listInput) {
					if (obj instanceof OutputString) {
						precipitationOutput.add((OutputString)obj);
					} else {
						wazeData.add((AlertData)obj);
					}
				}
				
				// Find the closest alert for each gps data
				for (OutputString matchingGP3SP : precipitationOutput) {
					String gpsStreet = matchingGP3SP.getStreetName();
					
					List<AlertData> streetAlerts = new ArrayList<>();
					AlertData closestAlert = null;
					
					for (AlertData alert : wazeData) { // Get the closest alert by street name
						
						if (!alert.getAlertStreet().isEmpty() && !gpsStreet.equals("-") && alert.getAlertStreet().contains(gpsStreet)) { // add only alerts of the same street 
							streetAlerts.add(alert);
						}
					}
					
					if (!streetAlerts.isEmpty()) { // sort alert list by date to get the most recent
						Collections.sort(streetAlerts);

						//Get only events that happening at most 1 hour after/before the gps time
						Long closestTime = Long.MAX_VALUE;
						for (AlertData alertSameStreet : streetAlerts) {
							
							//positivo - alerta enviado depois
							//negativo - alerta enviado antes
							long currentDifferenceTime = GeoPoint.getTimeDifference(matchingGP3SP.getTimestamp(), alertSameStreet.getAlertTime()); //check the abs value
							if (Math.abs(currentDifferenceTime) < closestTime && ((currentDifferenceTime <= 0 && Math.abs(currentDifferenceTime) < ONE_HOUR) ||
									(currentDifferenceTime > 0 && Math.abs(currentDifferenceTime) < TEN_MINUTES))) {
								closestTime = Math.abs(currentDifferenceTime);
								closestAlert = alertSameStreet;
							}
						}
						//
						
//						closestAlert = streetAlerts.get(streetAlerts.size()-1);
						
						if (closestAlert != null) {
							double closestDistanceAlert = GeoPoint.getDistanceInMeters(Double.valueOf(matchingGP3SP.getLatShape()), Double.valueOf(matchingGP3SP.getLonShape()), 
									Double.valueOf(closestAlert.getAlertLatitude()), Double.valueOf(closestAlert.getAlertLongitude()));
							closestAlert.setDistanceToClosestShapePoint(String.valueOf(closestDistanceAlert));
							matchingGP3SP.setAlertData(closestAlert);
						}
					}
					
					//hour:date
					String hourDateKey = matchingGP3SP.getTimestamp().substring(0, 3) + stringDate;
					
					alertMatchingOutput.add(new Tuple2<String, Object>(hourDateKey, matchingGP3SP));
				}
				return alertMatchingOutput.iterator();
			}
		});
		
		
		rddMatchedAlert.saveAsTextFile(outputPath + SLASH + "alert_aux_" + stringDate);
		
		JavaRDD<String> jamsString = context.textFile(dayWazePath + "jams_" + stringDate + ".csv", minPartitions)
				.mapPartitionsWithIndex(removeHeader, false);
		
		// Jam data grouped by hour:date, jam
		JavaPairRDD<String, Object> rddJamsData = jamsString.mapToPair(new PairFunction<String, String, Object>() {

			public Tuple2<String, Object> call(String string) throws Exception {
				String jamString = string.replaceAll(",,", ",-,"); //to fix empty fields in the middle
				String[] splittedEntry = jamString.split(SEPARATOR_WAZE);
				
				String blockDescription = "-";
				try {
					blockDescription = splittedEntry[jamBlockDescription];
				} catch (ArrayIndexOutOfBoundsException e) {
					//block description empty
				}
				
				String blockExpiration = "-";
				try {
					blockExpiration = splittedEntry[jamExpirationDateTime];
				} catch (ArrayIndexOutOfBoundsException e) {
					//block expiration empty
					System.out.println(blockExpiration);
				}
				
				JamData jams = new JamData(splittedEntry[jamId], splittedEntry[jamDelay], splittedEntry[jamLength], splittedEntry[jamLevel],
						splittedEntry[jamLineCoordinates], splittedEntry[jamSeverity], splittedEntry[jamSpeedKMH], splittedEntry[jamUpdateDateTime], 
						blockDescription, blockExpiration, splittedEntry[jamBlockType], splittedEntry[jamStreet]);
				
				//hour:date
				String hourDateKey = jams.getJamUpdateTime().substring(0, 3) + stringDate;
				return new Tuple2<String, Object>(hourDateKey, jams);
			}
		});
		
		//Union of precipitation matching with jam data
		JavaPairRDD<String, Iterable<Object>> rddGroupedAlertJam = rddMatchedAlert.union(rddJamsData)
				.groupByKey(minPartitions);
		
		/*
		 * Matching between alerts output and jam data
		 * 
		 * @return the output: precipitation, alerts and jams grouped
		 */
		JavaRDD<String> rddAllDataMatched = rddGroupedAlertJam.flatMap(new FlatMapFunction<Tuple2<String,Iterable<Object>>,
				String>() {

			List<JamData> wazeData;
			List<OutputString> alertOutput;
			List<String> jamMatchingOutput;
				
			@Override
			public Iterator<String> call(Tuple2<String, Iterable<Object>> hourDateKey_objects) throws Exception {
				wazeData = new ArrayList<>();
				alertOutput = new ArrayList<>();
				jamMatchingOutput = new ArrayList<>();
				
				List<Object> listInput = Lists.newArrayList(hourDateKey_objects._2);
				for (Object obj : listInput) {
					if (obj instanceof OutputString) {
						alertOutput.add((OutputString)obj);
					} else {
						wazeData.add((JamData)obj);
					}
				}
				
				// Find the closest jam
				for (OutputString matchingGP3SP : alertOutput) {
					String output;
					String gpsStreet = matchingGP3SP.getStreetName();
					
					List<JamData> streetJams = new ArrayList<>();
					JamData closestJam = null;
					
					for (JamData jam : wazeData) { //check each jam alert of the same hour
						
						if (!jam.getJamStreet().isEmpty() && !gpsStreet.equals("-") && jam.getJamStreet().contains(gpsStreet)) { // add only jams of the same street 
							streetJams.add(jam);
						}
					}
					
					if (!streetJams.isEmpty()) { // sort alert list by date to get the most recent
						Collections.sort(streetJams);
						
						//Get only events that happening at most 1 hour after/before the gps time
						Long closestTime = Long.MAX_VALUE;
						for (JamData jamSameStreet : streetJams) {
							long currentDifferenceTime = Math.abs(GeoPoint.getTimeDifference(matchingGP3SP.getTimestamp(), jamSameStreet.getJamUpdateTime())); //check the abs value
							if (currentDifferenceTime < closestTime && currentDifferenceTime < ONE_HOUR) {
								closestTime = currentDifferenceTime;
								closestJam = jamSameStreet;
							}
						}
						//
					
//							closestJam = streetJams.get(streetJams.size()-1);
						
						if (closestJam != null) {
							matchingGP3SP.setJamData(closestJam);
						}
					}
					
					output = matchingGP3SP.getIntegratedOutputString();
					jamMatchingOutput.add(output);
				}
				
				return jamMatchingOutput.iterator();
			}
		});
		
		return rddAllDataMatched;
	}
}
