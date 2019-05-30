package BULMADependences;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class WazeData implements Serializable{

	private static final long serialVersionUID = 1L;
	public static final String SEPARATOR = ",";
	
	public WazeData() {}
	
	public List<Double> getLatLon(String coordinates) {
		List<Double> latLonList = new ArrayList<>();
		String[] coordinatesSplitted = coordinates.split(":");
		
		latLonList.add(Double.valueOf(coordinatesSplitted[2].replace("}\"", ""))); //lat = y
		latLonList.add(Double.valueOf(coordinatesSplitted[1].substring(0, 
				coordinatesSplitted[1].indexOf(",")))); //lon = x
		
		return latLonList;
	}
	
	// Creating Date from millisecond
	public String getDateTimeFromMillis(String timeMillis) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(Long.valueOf(timeMillis));
		
		DateFormat df = new SimpleDateFormat("Y-MM-dd HH:mm:ss");
		String timeDate = df.format(calendar.getTime());
		return timeDate;
	}
}
