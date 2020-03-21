package BULMADependences;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import PointDependencies.GPSPoint;

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
	private static Map<String, List<GPSPoint>> mapBusData;
	
	/**
	 * Filter columns, generate id for each gps point and sort data
	 * @throws ParseException 
	 */
	public static void filterGPSData(String filePath, String city) throws ParseException {
		List<String> gpsData = readDataFromCSV(filePath);
		List<String> filteredGPSData;
		String newFilePath; // the final gps name should be preprocessed_<date>.csv
		
		if (city.equals("Recife")) {
			filteredGPSData = filterGPSDataRecife(gpsData);
			newFilePath = filePath.substring(0, filePath.lastIndexOf("/")) + "/preprocessed" + filePath.substring(filePath.lastIndexOf("_"), filePath.length());
			
		} else if (city.equals("Curitiba")) {
			filteredGPSData = filterGPSDataCuritiba(gpsData);
			
			String fileName = filePath.substring(filePath.lastIndexOf("/")+1);
			String stringFileDate = subtractDay(fileName.substring(0, fileName.lastIndexOf("_veiculos")));
			
			newFilePath = filePath.substring(0, filePath.lastIndexOf("/")) + "/preprocessed_" + stringFileDate.replace("_", "-") + ".csv";
			//newFilePath = filePath.substring(0, filePath.lastIndexOf("/")) + "/preprocessed_" + filePath.substring(filePath.lastIndexOf("/")+1, filePath.lastIndexOf("_veiculos")).replace("_", "-") + ".csv";
			
		} else {
			filteredGPSData = filterGPSDataCG(gpsData);
			newFilePath = filePath.substring(0, filePath.lastIndexOf("/")) + "/preprocessed_" + filePath.substring(filePath.lastIndexOf("/")+19, filePath.length());
		}
		
		String header = "bus_code,latitude,longitude,timestamp,line_code,gps_id";
		
		saveData2CSV(newFilePath, filteredGPSData, header);
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
	
	/*private static List<String> filterGPSDataCuritiba(List<String> gpsData) {
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
	}*/
	
	private static List<String> filterGPSDataCuritiba(List<String> gpsData) {
//		bus_code, lat, lon, timestamp, route

		mapBusData = new HashMap<>();
		
		for (String gpsLine : gpsData) {
			String[] attributes = gpsLine.split(DELIMITER);
			String busCode = attributes[0];
			String latitude = attributes[1];
			String longitude = attributes[2];
			String timestamp = attributes[3].split(" ")[1]; // to get just the time
			String route = attributes[4];
			
			GPSPoint gpsPoint = new GPSPoint(busCode, latitude, longitude, timestamp, route);
			
			if (!mapBusData.containsKey(busCode)) {
				mapBusData.put(busCode, new ArrayList<GPSPoint>());
			}
			
			mapBusData.get(busCode).add(gpsPoint); // grouping by busCode
		}
		
		return sortGPSdataCuritiba();
	}
	
	private static List<String> sortGPSdataCuritiba() {
		
		List<String> filteredGPSData = new ArrayList<String>();
		
		for (Entry<String, List<GPSPoint>> busCode_data : mapBusData.entrySet()) {
			List<GPSPoint> busData = busCode_data.getValue();
			
			Collections.sort(busData); // Curitiba data is in descendent order
			
			int gpsIDCount = 0;
			for (GPSPoint gpsLine : busData) {
				int gpsID = ++gpsIDCount;
				
				String newLine = gpsLine.getBusCode() + DELIMITER + gpsLine.getLatitude() + DELIMITER + gpsLine.getLongitude() + 
						DELIMITER +	gpsLine.getTimeStamp() + DELIMITER + gpsLine.getLineCode() + DELIMITER + gpsID;
				filteredGPSData.add(newLine);
			}
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
			String timestamp = attributes[1].split(" ")[1]; // to get just the time; //Instante
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
	    String formattedDate = targetFormat.format(newDate).replace("_", "-"); 
	    
	    return formattedDate;
	}
}