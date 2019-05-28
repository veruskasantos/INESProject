package integration;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
import BULMADependences.WeatherData;
import scala.Tuple2;

public class MatchingGPSWeatherWaze {

	private static final String SEPARATOR = ",";
	private static final String SLASH = "/";
	private static final String OUTPUT_HEADER = "route,tripNum,shapeId,routeFrequency,shapeSequence,shapeLat,shapeLon,distanceTraveledShape,"
			+ "busCode,gpsPointId,gpsLat,gpsLon,distanceToShapePoint,gps_datetime,stopPointId,problem,headway,busBunching,nextBusCode";
	
	public static void main(String[] args) throws IOException, URISyntaxException, ParseException {

		if (args.length < 6) {
			System.err.println("Usage: <city> <output matchingGSS directory> <precipitation path> <waze path> <output path> <number of partitions>");
			System.exit(1);
		}

		Long initialTime = System.currentTimeMillis();

		String city = args[0];
		String matchingGSSOutputPath = args[1] + city + "/";
		String precipitationPath = args[2];
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
					
					if (!line.isEmpty()) { //skip empty line
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
						removeEmptyLinesAndHeader, minPartitions);

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
			String precipitationPath, String wazePath, Function2<Integer, Iterator<String>, Iterator<String>> removeEmptyLinesAndHeader, 
			int minPartitions) {
		
		// ---------------------Integration of weather data---------------------
		
		JavaRDD<String> precipitationString = context.textFile(precipitationPath, minPartitions)
				.mapPartitionsWithIndex(removeEmptyLinesAndHeader, false);
		
		// Bus stops grouped by route and stopID
		JavaPairRDD<String, Object> rddPrecipitations = precipitationString.mapToPair(new PairFunction<String, String, Object>() {

			public Tuple2<String, Object> call(String busStopsString) throws Exception {
				String[] splittedEntry = busStopsString.split(SEPARATOR);
				WeatherData weatherData = new WeatherData(splittedEntry[0], splittedEntry[0], splittedEntry[0],
						splittedEntry[0], splittedEntry[0]);
				String latLonKey = weatherData.getLatitude().substring(0, 3) + ":" + weatherData.getLatitude().substring(0, 2);
				
				// lat:lon,data
				return new Tuple2<String, Object>(latLonKey, weatherData);
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

						String latLongKey = busteOutput.getLatShape().substring(0, 3) + ":" + busteOutput.getLonShape().substring(0, 2);

						return new Tuple2<String, Object>(latLongKey, busteOutput);
					}
				});
				
		JavaPairRDD<String, Iterable<Object>> rddGroupedPrecipitation = rddPrecipitations.union(rddBusteOutputGrouped)
						.groupByKey(minPartitions);
		
		//to do the comparisons
		JavaRDD<String> output = rddGroupedPrecipitation.flatMap(new FlatMapFunction<Tuple2<String,Iterable<Object>>, String>() {

			private List<WeatherData> weatherDataList;
			private List<BulmaBusteOutput> bulmaBusteOutputList;
			
			@Override
			public Iterator<String> call(Tuple2<String, Iterable<Object>> latLonKey_objects)
					throws Exception {
				
				weatherDataList = new ArrayList<WeatherData>();
				bulmaBusteOutputList = new ArrayList<BulmaBusteOutput>();
				
				List<Object> listInput = Lists.newArrayList(latLonKey_objects._2);
				for (Object obj : listInput) {
					if (obj instanceof BulmaBusteOutput) {
						bulmaBusteOutputList.add((BulmaBusteOutput)obj);
					} else {
						weatherDataList.add((WeatherData)obj);
					}
				}
				
				
				
				return null;
			}
		});
		
		
		// ---------------------Integration of waze data---------------------
		
		JavaRDD<String> wazeString = context.textFile(wazePath, minPartitions)
				.mapPartitionsWithIndex(removeEmptyLinesAndHeader, false);
		
		// Bus stops grouped by route and stopID
		JavaPairRDD<String, Iterable<String>> rddStreetConditions = wazeString.mapToPair(new PairFunction<String, String, String>() {

			public Tuple2<String, String> call(String busStopsString) throws Exception {
				String[] splittedEntry = busStopsString.split(SEPARATOR);
				String route = splittedEntry[6].replace(" ", "");
				String stopID = splittedEntry[2].replace(" ", "");
				String arrivalTime = splittedEntry[0].replace(" ", "");
				
				// route:stopId
				return new Tuple2<String, String>(route + ":" + stopID, arrivalTime);
			}
		}).groupByKey(minPartitions);
		
		return null;
		
	}
	
}
