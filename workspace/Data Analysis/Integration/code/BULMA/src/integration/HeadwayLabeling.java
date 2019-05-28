package integration;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
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
import org.apache.spark.api.java.function.PairFunction;

import com.clearspring.analytics.util.Lists;

import BULMADependences.BulmaBusteOutput;
import PointDependencies.GeoPoint;
import scala.Tuple2;

public class HeadwayLabeling {

	//TODO change the index to variable names
	private static final String SEPARATOR = ",";
	private static final String SLASH = "/";
	private static final int BB_THRESHOLD = 5; // headway = 5 is considered bb
	private static final String OUTPUT_HEADER = "route,tripNum,shapeId,routeFrequency,shapeSequence,shapeLat,shapeLon,distanceTraveledShape,"
			+ "busCode,gpsPointId,gpsLat,gpsLon,distanceToShapePoint,gps_datetime,stopPointId,problem,headway,busBunching,nextBusCode";

	private static HashMap<String, HashMap<String, Long>> scheduledHeadwaysMap = new HashMap<String, HashMap<String, Long>>();

	public static void main(String[] args) throws IOException, URISyntaxException, ParseException {

		if (args.length < 5) {
			System.err.println("Usage: <city> <output Buste directory> <GTFS path> <output path> <number of partitions>");
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
		System.out.println("Execution time: " + TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - initialTime) + " min");
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
					String timestamp = line.split(SEPARATOR)[13];
					
					if (!line.isEmpty() && !timestamp.equals("-")) { //skip empty line and line/shape without gps (the bus did not go to that stop)
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
			if (dirName.contains("BuLMABusTE")) {

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

				JavaRDD<String> result = execute(context, busteOutputString, stopTimesShapesPath, city, minPartitions);

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
	private static JavaRDD<String> execute(JavaSparkContext context, JavaRDD<String> busteOutputString,
			String stopTimesShapesPath, final String city, int minPartitions) {
		
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

		// Grouping BUSTE output by route-stopID
		JavaPairRDD<String, Iterable<BulmaBusteOutput>> rddBusteOutputGrouped = busteOutputString
				.mapToPair(new PairFunction<String, String, BulmaBusteOutput>() {

					public Tuple2<String, BulmaBusteOutput> call(String bulmaOutputString) throws Exception {
						StringTokenizer st = new StringTokenizer(bulmaOutputString, SEPARATOR);
						BulmaBusteOutput busteOutput = new BulmaBusteOutput(st.nextToken(), st.nextToken(),
								st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken(),
								st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken(),
								st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken(), "buste");

						String stopID = busteOutput.getStopID();

						// Create the stops-busteOutput map
						// if (!stopsBusteMap.containsKey(stopID)) {
						// stopsBusteMap.put(stopID, new ArrayList<BulmaOutput>());
						// }
						// stopsBusteMap.get(stopID).add(busteOutput);

						String routeStopIDKey = busteOutput.getRoute() + ":" + stopID;

						return new Tuple2<String, BulmaBusteOutput>(routeStopIDKey, busteOutput);
					}
				}).groupByKey(minPartitions);

		
		JavaRDD<String> busStopsString = context.textFile(stopTimesShapesPath, minPartitions)
				.mapPartitionsWithIndex(removeHeader, false);
		
		// Bus stops grouped by route and stopID
		JavaPairRDD<String, Iterable<String>> rddBusStops = busStopsString.mapToPair(new PairFunction<String, String, String>() {

			public Tuple2<String, String> call(String busStopsString) throws Exception {
				String[] splittedEntry = busStopsString.split(SEPARATOR);
				String route = splittedEntry[6].replace(" ", "");
				String stopID = splittedEntry[2].replace(" ", "");
				String arrivalTime = splittedEntry[0].replace(" ", "");
				
				// route:stopId
				return new Tuple2<String, String>(route + ":" + stopID, arrivalTime);
			}
		}).groupByKey(minPartitions);
		
		
		/**
		 * Calculate headway of bus stops and grouped by route
		 * 
		 * @return Grouped route, stops - arrivals - headway
		 * 
		 */
		JavaPairRDD<String, Iterable<Tuple2<String, List<Tuple2<String, Long>>>>> rddBusStopsGrouped = rddBusStops.mapToPair(new 
				PairFunction<Tuple2<String,Iterable<String>>, String, Tuple2<String, List<Tuple2<String, Long>>>>() {

			@Override
			public Tuple2<String, Tuple2<String, List<Tuple2<String, Long>>>> call(Tuple2<String, Iterable<String>> routeStopID_arrivalTimes) 
					throws Exception {
				String routeStopIDKey = routeStopID_arrivalTimes._1; // route:stopId
				
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
					//System.out.println("before: " +  ((Collection<?>)routeStopID_arrivalTimes._2).size()
					//		+ " after: " + arrivalTimesSet.size());
					List<String> arrivalTimesList =  new ArrayList<String>(arrivalTimesSet);
					Collections.sort(arrivalTimesList);
					
					for (int i = 0; i < arrivalTimesList.size()-2; i++) {
						String firstArrival = arrivalTimesList.get(i);
						String secondArrival = arrivalTimesList.get(i+1);
						long headway = GeoPoint.getTimeDifference(firstArrival, secondArrival);
						String firstArrivalSecondArrivalKey = firstArrival.replaceAll(":", "") + "_" + secondArrival.replaceAll(":", "");
						
						arrivalTimesHeadwayMap.add(new Tuple2<String, Long>(firstArrivalSecondArrivalKey, headway));
						
						if (!scheduledHeadwaysMap.containsKey(routeStopIDKey)) {
							scheduledHeadwaysMap.put(routeStopIDKey, new HashMap<String,Long>());
						}
						scheduledHeadwaysMap.get(routeStopIDKey).put(firstArrivalSecondArrivalKey, headway); //firstArrivalTime_secondArrivalTime
					}
					
					if (arrivalTimesList.size() == 2) {
						String firstArrival = arrivalTimesList.get(0);
						String secondArrival = arrivalTimesList.get(1);
						long headway = GeoPoint.getTimeDifference(firstArrival, secondArrival);
						String firstArrivalSecondArrivalKey = firstArrival.replaceAll(":", "") + "_" + secondArrival.replaceAll(":", "");
						
						arrivalTimesHeadwayMap.add(new Tuple2<String, Long>(firstArrivalSecondArrivalKey, headway));
						
						if (!scheduledHeadwaysMap.containsKey(routeStopIDKey)) {
							scheduledHeadwaysMap.put(routeStopIDKey, new HashMap<String,Long>());
						}
						scheduledHeadwaysMap.get(routeStopIDKey).put(firstArrivalSecondArrivalKey, headway); //firstArrivalTime_secondArrivalTime
					}
					
					return new Tuple2<String, Tuple2<String,List<Tuple2<String, Long>>>>(routeStopIDKey.split(":")[0], 
							new Tuple2<String,List<Tuple2<String, Long>>>(routeStopIDKey.split(":")[1],  arrivalTimesHeadwayMap));
					
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
		
		//TODO update this name, and test with qgis
		rddBusStopsGrouped.saveAsTextFile("D:/Desktop/UFCG/Projeto INES/INESProject/workspace/Data Analysis/Integration/code/BULMA/data/output/Recife/scheduled_hd");
		
		// rddBusStopsGrouped.saveAsTextFile("/home/veruska/Documentos/Projeto_INES/INESProject/workspace/Data Analysis/Integration/code/BULMA/data/output/Recife/scheduled_hd");
		
		// Calculate the headway between the buses, considering same route, same stop and same day
		// Headway: time difference for the bus that is in front
		JavaPairRDD<String, List<BulmaBusteOutput>> rddHeadwayLabeling = rddBusteOutputGrouped.mapToPair(
				new PairFunction<Tuple2<String, Iterable<BulmaBusteOutput>>, String, List<BulmaBusteOutput>>() {

					public Tuple2<String, List<BulmaBusteOutput>> call(
							Tuple2<String, Iterable<BulmaBusteOutput>> routeStopID_BulmaBusteOutput) throws Exception {
						
						String routeStopID = routeStopID_BulmaBusteOutput._1;
						List<BulmaBusteOutput> listBusteOutput = Lists.newArrayList(routeStopID_BulmaBusteOutput._2);
						Collections.sort(listBusteOutput);

						List<BulmaBusteOutput> listBusteOutputHeadway = new ArrayList<>();

						for (int i = 0; i < listBusteOutput.size()-2; i++) {// buses of the same route in a same stop
							BulmaBusteOutput currentBusteOutput = listBusteOutput.get(i); // avoid comparison with the same row
							String currentBusCode = currentBusteOutput.getBusCode();
							
							BulmaBusteOutput closestNextBus = null;
							long closestHeadway = Long.MAX_VALUE;

							for (int j = i+1; j < listBusteOutput.size()-1; j++) {
								BulmaBusteOutput nextBusteOutput = listBusteOutput.get(j);
								
								if (!nextBusteOutput.getBusCode().equals(currentBusCode)) { //calculate headways just for different buscode 

									long currentHeadway = GeoPoint.getTimeDifference(currentBusteOutput.getTimestamp(), nextBusteOutput.getTimestamp());
									if (currentHeadway < closestHeadway) {
										closestNextBus = nextBusteOutput;
										closestHeadway = currentHeadway;
										break; // stop the search, because the list is sorted
									}
								}
							}
							
							if (closestNextBus == null) { //when there is only one bus for the route
								closestNextBus = listBusteOutput.get(i+1);
								closestHeadway = GeoPoint.getTimeDifference(currentBusteOutput.getTimestamp(), closestNextBus.getTimestamp());
							}

							//checking bus bunching with scheduled headway
							Long scheduledHeadway = null;
							String[] firstBusTimeSplit = currentBusteOutput.getTimestamp().split(":");
							int firstBusTime =  Integer.valueOf(firstBusTimeSplit[0] + firstBusTimeSplit[1] 
									+ firstBusTimeSplit[2]);
							
							String[] secondBusTimeSplit = closestNextBus.getTimestamp().split(":");
							int secondBusTime =  Integer.valueOf(secondBusTimeSplit[0] + secondBusTimeSplit[1] 
									+ secondBusTimeSplit[2]);

							HashMap<String, Long> arrivalTimesHeadwayMap = scheduledHeadwaysMap.get(routeStopID);
							for (Entry<String, Long> arrivalTimesHeadway : arrivalTimesHeadwayMap.entrySet()) {
								String[] arrivalTimes = arrivalTimesHeadway.getKey().split("_");
								Long headway = arrivalTimesHeadway.getValue();
								
								int firstArrivalTime = Integer.valueOf(arrivalTimes[0]);
								int secondArrivalTime = Integer.valueOf(arrivalTimes[1]);
								
								//TODO se esse mapa tiver ordenado, flexibilizar os ifs pq pega o primeiro
								if (firstBusTime >= firstArrivalTime && firstBusTime <= secondArrivalTime
										&& secondBusTime >= firstArrivalTime && secondBusTime <= secondArrivalTime) {
									scheduledHeadway = headway;
									break;
								}
							}
							
							boolean busBunching = true;
							if (scheduledHeadway == null) {// if there is no data, consider a threshold
								if (closestHeadway > BB_THRESHOLD) {
									busBunching = false;
								}
							} else if (closestHeadway > (scheduledHeadway/4)) {
								busBunching = false;
							}
							
							//saving
							currentBusteOutput.setHeadway(closestHeadway);
							currentBusteOutput.setNextBusCode(closestNextBus.getBusCode());
							currentBusteOutput.setBusBunching(busBunching);
							
							listBusteOutputHeadway.add(currentBusteOutput);
						}

						return new Tuple2<String, List<BulmaBusteOutput>>(routeStopID, listBusteOutputHeadway);
					}
				});
		
		// route, trip_number/no_shape_code, shape_id/-, route_frequency/-,
		// shape_sequence/-, shape_lat/-, shape_lon/-,
		// distance_traveled, bus_code, gps_id, gps_lat, gps_lon,
		// distance_to_shape_point/-, gps_timestamp, stop_id, trip_problem_code,
		// headway, bus_bunching, next_bus_code

		JavaRDD<String> rddOutput = rddHeadwayLabeling
				.flatMap(new FlatMapFunction<Tuple2<String, List<BulmaBusteOutput>>, String>() {

					@Override
					public Iterator<String> call(Tuple2<String, List<BulmaBusteOutput>> routeStopID_rddHeadway)
							throws Exception {

						List<String> listOutput = new ArrayList<>();
						for (BulmaBusteOutput line : routeStopID_rddHeadway._2) {

							String newOutput = line.getRoute() + SEPARATOR + line.getTripNum() + SEPARATOR
									+ line.getShapeId() + SEPARATOR + line.getRouteFrequency() + SEPARATOR
									+ line.getShapeSequence() + SEPARATOR + line.getLatShape() + SEPARATOR
									+ line.getLonShape() + SEPARATOR + line.getDistance() + SEPARATOR
									+ line.getGpsPointId() + SEPARATOR + line.getLatGPS() + SEPARATOR 
									+ line.getLonGPS() + SEPARATOR + line.getDistanceToShapePoint() 
									+ SEPARATOR + line.getGps_datetime() + SEPARATOR + line.getStopID() 
									+ SEPARATOR + line.getTripProblem() + SEPARATOR + line.getBusCode() 
									+ SEPARATOR + line.getHeadway() + SEPARATOR + line.isBusBunching() 
									+ SEPARATOR + line.getNextBusCode();

							listOutput.add(newOutput);
						}

						return listOutput.iterator();
					}
				});

		return rddOutput;
	}
}
