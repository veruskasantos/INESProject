package BULMADependences;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class WazeData implements Serializable, Comparable<WazeData> {

	private static final long serialVersionUID = 1L;
	public static final String SEPARATOR = ",";
	
	private Long timeInMillis;
	
	public WazeData() {}
	
	public List<Double> getLatLonDouble(String coordinates) {
		List<Double> latLonList = new ArrayList<>();
		String[] coordinatesSplitted = coordinates.split(":");
		
		latLonList.add(Double.valueOf(coordinatesSplitted[2].replace("}", "").replace("\"", "").replace("]", ""))); //lat = y
		latLonList.add(Double.valueOf(coordinatesSplitted[1].substring(0, 
				coordinatesSplitted[1].indexOf(",")))); //lon = x
		
		return latLonList;
	}
	
	public List<String> getLatLon(String coordinates) {
		List<String> latLonList = new ArrayList<>();
		String[] coordinatesSplitted = coordinates.split(":");
		
		latLonList.add(coordinatesSplitted[2].replace("}", "").replace("\"", "").replace("]", "")); //lat = y
		latLonList.add(coordinatesSplitted[1].substring(0, 
				coordinatesSplitted[1].indexOf(","))); //lon = x
		
		return latLonList;
	}
	
	// Creating Date from millisecond
	public String getDateTimeFromMillis(String timeMillis) {
		setTimeInMillis(Double.valueOf(timeMillis).longValue());
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(Double.valueOf(timeMillis).longValue());
		
		DateFormat df = new SimpleDateFormat("Y-MM-dd HH:mm:ss");
		String timeDate = df.format(calendar.getTime());
		return timeDate;
	}

	public Long getTimeInMillis() {
		return timeInMillis;
	}

	public void setTimeInMillis(Long timeMillis) {
		this.timeInMillis = timeMillis;
	}

	@Override
	public int compareTo(WazeData other) { // compare date
		
		if (this.getTimeInMillis() < other.getTimeInMillis()) { 
			return -1;
		} else if (this.getTimeInMillis() > other.getTimeInMillis()) { 
			return 1;
		}
		
		return 0;
	}
}
