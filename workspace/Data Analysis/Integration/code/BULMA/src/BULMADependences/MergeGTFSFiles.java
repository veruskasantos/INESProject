package BULMADependences;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import PointDependencies.GeoPoint;
import PointDependencies.ShapePoint;

public class MergeGTFSFiles {

	private static final String FILE_SEPARATOR = ",";
	private static final String HEADER_STOP_TIME_FILE = "service_id,arrival_time,departure_time,stop_id,stop_sequence,lat_stop,lng_stop,route_id,"
			+ "shape_id,closest_shape_point";

	private static Map<String, String> mapTripRoute = new HashMap<String, String>();
	private static Map<String, String> mapStopLatLng = new HashMap<String, String>();
	private static Map<String, String> mapShapeRouteId = new HashMap<String, String>();
	private static Map<String, String> mapRouteIdRouteCode = new HashMap<String, String>();
	private static Map<String, ShapeLine> mapShapeLines = new HashMap<String, ShapeLine>();

	public static void main(String[] args) {
		
		if (args.length != 3) {
			System.err.println("Usage: <city> <gtfs path> <output path>");
			System.exit(1);
		}
		
		String city = args[0];
		String GTFSPath = args[1] + city + "/";
		
		String stopTimes = GTFSPath + "stop_times.txt"; // trip_id,arrival_time,departure_time,stop_id,stop_sequence
		String trips = GTFSPath + "trips.txt"; // route_id
		String stops = GTFSPath + "stops.txt"; // lat_stop, lng_stop
		String routes = GTFSPath + "routes_label.txt";
		String shapes = GTFSPath + "shapes.csv";

//		Uncomment the lines below to generate Shape File
//		System.out.println("Running - Creating new Shape File");
//		String shapeOutputPath = args[2]  + city + "/shapesSTREET.csv";
//		readRoutesFile(routes);
//		readTripFileGetRoute(trips, city);
//		updateShapeFile(shapes, shapeOutputPath);
		
//	    Uncomment the lines below to generate stops times file
		System.out.println("Running - Creating new Stops Times File");
		String stopOutputPath = args[2]  + city + "/stop_times_shapes1.txt";
		readRoutesFile(routes);
		readTripFile(trips, city);
		readStopsFile(stops);
		createShapePoints(shapes);
		createNewFile(stopOutputPath, stopTimes, city);
		
		System.out.println("Done!");
	}
	
	private static void createShapePoints(String shapes) {
		BufferedReader brShapes = null;
		String lineShapes = "";
		try {
			brShapes = new BufferedReader(new FileReader(shapes));
			
			String previousId = null;
			brShapes.readLine();	
			List<GeoPoint> listPoints = new ArrayList<GeoPoint>();
			while ((lineShapes = brShapes.readLine()) != null) {

				String[] data = lineShapes.replace("\"", "").split(FILE_SEPARATOR);
				String shapeId = data[1];
				String route = mapShapeRouteId.get(shapeId);
				if (route == null) {
					route = "-";
				}
				
				String lat = data[2];
				String lng = data[3];
				String pointSequence = data[4];
				
				ShapePoint currentShapePoint = new ShapePoint(shapeId, lat, lng, pointSequence, null);
				
				if (previousId != null && !previousId.equals(shapeId)) {
					createNewShapeLine(listPoints);
					listPoints = new ArrayList<GeoPoint>();
				}
				listPoints.add(currentShapePoint);
				previousId = shapeId;
				
			}
			createNewShapeLine(listPoints);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void createNewShapeLine(List<GeoPoint> listGeoPoint) {
		@SuppressWarnings("deprecation")
		GeometryFactory geometryFactory = JtsSpatialContext.GEO.getGeometryFactory();
		
		List<Coordinate> coordinates = new ArrayList<Coordinate>();
		
		GeoPoint currentGeoPoint = null;
		for (int i = 0; i < listGeoPoint.size(); i++) {
			currentGeoPoint = listGeoPoint.get(i);
			
			Double latitude = Double.valueOf(currentGeoPoint.getLatitude());
			Double longitude = Double.valueOf(currentGeoPoint.getLongitude());
			coordinates.add(new Coordinate(latitude, longitude));
		}

		Coordinate[] array = new Coordinate[coordinates.size()];

		LineString lineString = geometryFactory.createLineString(coordinates.toArray(array));
		ShapeLine shapeLine = new ShapeLine(((ShapePoint)currentGeoPoint).getId(), lineString, null, null,
				listGeoPoint, null, 0);
		
		mapShapeLines.put(((ShapePoint)currentGeoPoint).getId(), shapeLine);
		
	}

	/**
	 * Gets the closest shape point of the stop point
	 * @param stopPoint
	 * @param shapeLine the shape line matched 
	 * @return tuple with the closest shape point and the distance between it and the stop point
	 */
	private static ShapePoint getClosestShapePoint(GeoPoint geoPoint, ShapeLine shapeLine) {
	
		Float smallerDistance = Float.MAX_VALUE;
		ShapePoint closestShapePoint = null;
		
		Float currentDistance;
		for (GeoPoint shapePoint : shapeLine.getListGeoPoints()) {
			currentDistance = GeoPoint.getDistanceInMeters(geoPoint, shapePoint);
			if (currentDistance < smallerDistance) {
				smallerDistance = currentDistance;
				closestShapePoint = (ShapePoint) shapePoint;
			}
		}
		
		return closestShapePoint;
	}

	// add route_id and street_name to shape file (TODO: update to run before)
	private static void updateShapeFile(String shapes, String newFilePath) {
		BufferedReader brShapes = null;
		String lineShapes = "";
		try {
			brShapes = new BufferedReader(new FileReader(shapes));
			FileWriter output = new FileWriter(newFilePath);
			PrintWriter printWriter = new PrintWriter(output);

			printWriter.println("route_id" + FILE_SEPARATOR + brShapes.readLine().replace("\"", "")
					+ FILE_SEPARATOR + "street_name");
			
			while ((lineShapes = brShapes.readLine()) != null) {

				String[] data = lineShapes.replace("\"", "").split(FILE_SEPARATOR);
				String shapeId = data[1];
				String routeId = mapShapeRouteId.get(shapeId);
				String routeCode = null;
				
				double lat = Double.valueOf(data[2]);
				double lng = Double.valueOf(data[3]);
				String streetName = getStreetNameHERE(lat, lng);
				
				if (routeId == null) {
					routeCode = "-";
				} else {
					routeCode = mapRouteIdRouteCode.get(routeId);
					if (routeCode == null) {
						routeCode = "-";
					} 
				}
				
				printWriter.println(routeCode + FILE_SEPARATOR + lineShapes.replace("\"", "") + FILE_SEPARATOR + streetName);
			}
			output.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static String getStreetNameHERE(double lat, double lng) {
		String urlQueryString = "https://reverse.geocoder.api.here.com/6.2/reversegeocode.json?prox=" + lat + "," + lng
				+ ",250&mode=retrieveAddresses&maxresults=1&gen=9&app_id=KMr9OUaKNVSNVeorfjy1&app_code=m04i3MJhV2gk-8VzUukETw";
		
		JSONObject json = null;
		String street = "";
		try {
			URL url = new URL(urlQueryString);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setInstanceFollowRedirects(false);
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.addRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64)");
			connection.setRequestProperty("charset", "utf-8");
			connection.connect();
			InputStream inStream = connection.getInputStream();

			BufferedReader rd = new BufferedReader(new InputStreamReader(inStream, Charset.forName("UTF-8")));
			StringBuilder sb = new StringBuilder();
			int cp;
			while ((cp = rd.read()) != -1) {
				sb.append((char) cp);
			}
			json = new JSONObject(sb.toString());

			// System.out.println(i++ + sb.toString());

			try { // when there are multiple roads, get the first one
				street = (String) (json.getJSONObject("Response").getJSONArray("View").getJSONObject(0)
						.getJSONArray("Result").getJSONObject(0).getJSONObject("Location").getJSONObject("Address")
						.get("Street"));
			} catch (Exception e) {
				// there is no road name for this lat/lng
			}

			connection.disconnect();

		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return street;
	}
	
	
	private static void readTripFileGetRoute(String trips, String city) {
		
		
		BufferedReader brTrips = null;
		String lineTrips = "";
		
		try {
			brTrips = new BufferedReader(new FileReader(trips));
			brTrips.readLine();
			while ((lineTrips = brTrips.readLine()) != null) {

				String[] data = lineTrips.replace("\"", "").split(FILE_SEPARATOR);
				String route = data[0];
				String shapeId;
				
				if (city.equals("Recife")) {
					shapeId = data[6];
					
					if (shapeId.equals("")) { // some lines have some wrong empty fields
						shapeId = data[7];
					}
				} else {
					shapeId = data[7];
				}

				if (!mapShapeRouteId.containsKey(shapeId)) {
					mapShapeRouteId.put(shapeId, route);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void readRoutesFile(String routes) {
		
		BufferedReader brRoute = null;
		String lineRoute = "";
		
		try {
			brRoute = new BufferedReader(new FileReader(routes));
			brRoute.readLine();
			while ((lineRoute = brRoute.readLine()) != null) {

				String[] data = lineRoute.replace("\"", "").split(FILE_SEPARATOR);
				String routeId = data[0];
				String routeCode = data[2];
				
				if (!mapRouteIdRouteCode.containsKey(routeId)) {
					mapRouteIdRouteCode.put(routeId, routeCode);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void readTripFile(String trips, String city) {
		BufferedReader brTrips = null;
		String lineTrips = "";
		
		try {
			brTrips = new BufferedReader(new FileReader(trips));
			brTrips.readLine();
			while ((lineTrips = brTrips.readLine()) != null) {

				String[] data = lineTrips.replace("\"", "").split(FILE_SEPARATOR);
				String route = data[0];
				String serviceId = data[1];
				String tripId = data[2];
				String shapeId;
				
				if (city.equals("Recife")) {
					shapeId = data[6];
					
					if (shapeId.equals("")) { // some lines have some wrong empty fields
						shapeId = data[7];
					}
				} else {
					shapeId = data[7];
				}
				

				if (!mapTripRoute.containsKey(tripId)) {
					mapTripRoute.put(tripId, route + FILE_SEPARATOR + serviceId + FILE_SEPARATOR + shapeId);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void readStopsFile(String stops) {
		BufferedReader brStops = null;
		String lineStops = "";
		try {
			brStops = new BufferedReader(new FileReader(stops));
			brStops.readLine();
			while ((lineStops = brStops.readLine()) != null) {

				String[] data = lineStops.replace("\"", "").split(FILE_SEPARATOR);
				String stopId = data[0];
				String lat = data[4];
				String lng = data[5];
				
				// when the separator is comma and some address has comma
				if (!lat.startsWith("-")) {
					lat = data[5];
					lng = data[6];
					
					if (!lat.startsWith("-")) {
						lat = data[6];
						lng = data[7];
					}
				}
				
				if (!mapStopLatLng.containsKey(stopId)) {
					mapStopLatLng.put(stopId, lat + FILE_SEPARATOR + lng);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void createNewFile(String newFilePath, String stopTimes, String city) {
		BufferedReader brStopTimes = null;
		
		String lineStopTime = "";

		try {

			FileWriter output = new FileWriter(newFilePath);
			PrintWriter printWriter = new PrintWriter(output);

			printWriter.println(HEADER_STOP_TIME_FILE);
			
			brStopTimes = new BufferedReader(new FileReader(stopTimes));
			brStopTimes.readLine();
			while ((lineStopTime = brStopTimes.readLine()) != null) {

				String[] data = lineStopTime.replace("\"", "").split(FILE_SEPARATOR);
				String tripId = data[0];
				String arrivalTime = data[1];
				String departureTime = data[2];
				String stopId = data[3];
				String stopSequence = data[4];
				String routeServiceShapeId = mapTripRoute.get(tripId);
				String latlng = mapStopLatLng.get(stopId);

				String lat = latlng.split(FILE_SEPARATOR)[0];
				String lng = latlng.split(FILE_SEPARATOR)[1];
				String shapeId = routeServiceShapeId.split(FILE_SEPARATOR)[2];
				String serviceId = routeServiceShapeId.split(FILE_SEPARATOR)[1];
				String routeId = routeServiceShapeId.split(FILE_SEPARATOR)[0];
				
				String routeCode = mapRouteIdRouteCode.get(routeId);
				if (routeCode == null) {
					routeCode = "-";
				}
				ShapePoint closestPoint = getClosestShapePoint(new ShapePoint(null, lat, lng , null, null), mapShapeLines.get(shapeId));
				
				printWriter.print(serviceId + FILE_SEPARATOR); //service_id added
				printWriter.print(arrivalTime + FILE_SEPARATOR);
				printWriter.print(departureTime + FILE_SEPARATOR);
				printWriter.print(stopId + FILE_SEPARATOR);
				printWriter.print(stopSequence + FILE_SEPARATOR);
				printWriter.print(latlng + FILE_SEPARATOR);
				
				if (city.equals("Recife")) { //GPS has route id, not route code
					printWriter.print(routeId + FILE_SEPARATOR);
				} else {
					printWriter.print(routeCode + FILE_SEPARATOR);
				}
				printWriter.print(shapeId + FILE_SEPARATOR);
				printWriter.println(closestPoint.getPointSequence());
			}
			
			if (mapStopLatLng.size() > 0) {
				System.out.println("Size: " + mapStopLatLng.size());
			}
			
			output.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (brStopTimes != null) {
				try {
					brStopTimes.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
