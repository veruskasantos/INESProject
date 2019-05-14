package BULMADependences;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Filter GPS data to bus.code, latitude, longitude, timestamp, line.code, gps.id
 * Add route data to shapes.txt file
 * 
 * @author Veruska
 * 
 * @input GPS file, shapes.txt, trips.txt, routes.txt
 * @output GPS: bus.code, latitude, longitude, timestamp, line.code, gps.id
 * 		   Shape: route_id, route_frequency, shape_id, shape_pt_lat, shape_pt_lon, shape_pt_sequence, shape_dist_traveled
 *
 */
public class PreProcessingBULMAInput {
	
	private static final String DELIMITER = ",";
	
	/**
	 * Filter columns, generate id for each gps point and sort data
	 */
	public static void filterGPSData(String filePath, String city) {
		List<String> gpsData = readDataFromCSV(filePath);
		List<String> filteredGPSData;
		
		if (city.equals("Recife")) {
			filteredGPSData = filterGPSDataRecife(gpsData);
		} else if (city.equals("Curitiba")) {
			filteredGPSData = filterGPSDataCuritiba(gpsData);
		} else {
			filteredGPSData = filterGPSDataCG(gpsData);
		}
		
		String newFilePath = filePath.substring(0, filePath.lastIndexOf("/")) + "/preprocessed_" + filePath.substring(filePath.lastIndexOf("/")+1, filePath.length());
		String header = "bus_code,latitude,longitude,timestamp,line_code,gps_id";
		
		saveData2CSV(newFilePath, filteredGPSData, header);
	}
	
	
	/**
	 * Add route id and frequency to shapes.txt file
	 * TODO: write this code in jupyter notebook
	 */
	public static void updateShapeFile(String filePath, String city) {
		String shapePath = filePath + "shapes.txt";
		String tripsPath = filePath + "trips.txt";
		String routesPath = filePath + "routes_label.txt";
		
		List<String> newShapeFile = new ArrayList<>();
		
		// Create a map {route: shapes} from trips.txt
		Map<String, String> routeShapeMap = new HashMap<String, String>();
		
		List<String> tripsData = readDataFromCSV(tripsPath);
		for (String trip : tripsData) {
			String[] attributes = trip.split(DELIMITER);
			String route = attributes[0];
			String shapeID = attributes[7];
			
			if (city.equals("Recife")) {
				shapeID = attributes[6];
			}
			
			if (!routeShapeMap.containsKey(shapeID)) {
				routeShapeMap.put(shapeID, route); // assuming one route to one shape
			}
		}
	
		// Create a map {route: frequency} from routes.txt
		Map<String, String> routeFrequencyMap = new HashMap<String, String>();
		
		List<String> routesData = readDataFromCSV(routesPath);
		for (String routes : routesData) {
			String[] attributes = routes.split(DELIMITER);
			String route = attributes[0];
			String frequency = attributes[8];
			
			if (city.equals("CampinaGrande")) { //the gps data route is route_short_name
				String route_gps = attributes[2]; //route_short_name
				
				if (!routeFrequencyMap.containsKey(route)) {
					routeFrequencyMap.put(route, frequency + "-" + route_gps);
				}
				
			} else {
				if (!routeFrequencyMap.containsKey(route)) {
					routeFrequencyMap.put(route, frequency);
				}
			}
		}
				
		
		List<String> shapesData = readDataFromCSV(shapePath);
		for (String shape : shapesData) {
			String[] attributes = shape.split(DELIMITER);
			String shapeID = attributes[0];
			
			String route = "-";
			String frequency = "-";
			if (routeShapeMap.containsKey(shapeID)) {
				route = routeShapeMap.get(shapeID);
			}
			
			String newLine = route + DELIMITER + shape + DELIMITER + frequency;
			
			if (routeFrequencyMap.containsKey(route)) {
				
				if (city.equals("CampinaGrande")) {
					String[] frequency_route = routeFrequencyMap.get(route).split("-");
					frequency = frequency_route[0];
					String route_id = frequency_route[1];
					
					newLine = route_id + DELIMITER + shape + DELIMITER + frequency;
					
				} else {
					frequency = routeFrequencyMap.get(route);
					
					newLine = route + DELIMITER + shape + DELIMITER + frequency;
				}
			}
			
			newShapeFile.add(newLine);
		}
		
		String header = "route,shape_id,shape_pt_lat,shape_pt_lon,shape_pt_sequence,shape_dist_traveled,route_frequency";
		
		saveData2CSV(filePath + "shapes.csv", newShapeFile, header);
		
	}
	
	private static List<String> filterGPSDataCG(List<String> gpsData) {
//		_id, stopSequence, diffLastUpdate, bearing, vehicleLabel, stopId, lon, tripId, positionTime, 
//		routeId, delay, arrivalTime, vehicleId, tripStartTime, tripHash, percTravel, lat

		List<String> filteredGPSData = new ArrayList<String>();
		int gpsIDCount = 0;
		
		for (String gps : gpsData) {
			String[] attributes = gps.split(DELIMITER);
			String busCode = attributes[12]; //vehicleId
			String latitude = attributes[16];
			String longitude = attributes[6];
			String timestamp = attributes[8].split(" ")[1]; // positionTime: to get just the time
			String route = attributes[9]; //routeId
			int gpsID = ++gpsIDCount;
			
			String newLine = busCode + DELIMITER + latitude + DELIMITER + longitude + DELIMITER + 
					timestamp + DELIMITER + route + DELIMITER + gpsID;
			
			filteredGPSData.add(newLine);
		}
		
		return filteredGPSData;
	}
	
	private static List<String> filterGPSDataCuritiba(List<String> gpsData) {
//		bus_code, lat, lon, timestamp, route

		List<String> filteredGPSData = new ArrayList<String>();
		int gpsIDCount = 0;
		
		for (String gpsLine : gpsData) {
			String[] attributes = gpsLine.split(DELIMITER);
			String busCode = attributes[0];
			String latitude = attributes[1];
			String longitude = attributes[2];
			String timestamp = attributes[3].split(" ")[1]; // to get just the time
			String route = attributes[4];
			int gpsID = ++gpsIDCount;
			
			String newLine = busCode + DELIMITER + latitude + DELIMITER + longitude + DELIMITER + 
					timestamp + DELIMITER + route + DELIMITER + gpsID;
			filteredGPSData.add(newLine);
		}
		
		return filteredGPSData;
	}
	
	private static List<String> filterGPSDataRecife(List<String> gpsData) {
//		Unidad, Instante, Estado, Comunica, CoordX, CoordY, Linea, Ruta, Posicion, Viaje, Velocidad

		List<String> filteredGPSData = new ArrayList<String>();
		
		int gpsIDCount = 0;
		
		for (String gps : gpsData) {
			String[] attributes = gps.split(DELIMITER);
			String busCode = attributes[0]; //Unidad
			String latitude = attributes[4];
			String longitude = attributes[5];
			String timestamp = attributes[2]; //Instante
			String route = attributes[6]; //Linea
			int gpsID = ++gpsIDCount;
			
			String newLine = busCode + DELIMITER + latitude + DELIMITER + longitude + DELIMITER + 
					timestamp + DELIMITER + route + DELIMITER + gpsID;
			
			filteredGPSData.add(newLine);
		}
		
		return filteredGPSData;
	}
	
	
	private static List<String> readDataFromCSV(String filePath) { 
		List<String> data = new ArrayList<String>(); 

		try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
			br.readLine(); //skip header
			
			String line = br.readLine();
			while (line != null) { 
				data.add(line);
				
				line = br.readLine(); 
			}
			
		} catch (IOException ioe) {
			ioe.printStackTrace(); 
		} 
		
		return data; 
	}
	
	
	private static void saveData2CSV(String filePath, List<String> data, String header) {
		BufferedWriter br;
		
		try {
			br = new BufferedWriter(new FileWriter(filePath));
			StringBuilder sb = new StringBuilder();

			sb.append(header);
			sb.append("\n");
			
			for (String line : data) {
				sb.append(line);
				sb.append("\n");
			}
			
			br.write(sb.toString());
			br.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
		System.out.println("Saving new file. Lines: " + data.size());
	}
}