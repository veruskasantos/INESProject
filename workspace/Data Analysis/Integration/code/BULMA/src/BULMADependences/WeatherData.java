package BULMADependences;

import java.io.Serializable;

public class WeatherData implements Serializable{
	
	private static final long serialVersionUID = 1L;
	private String latitude;
	private String longitude;
	private String time;
	private Double precipitation;
	private String stationCode;
	
	// It seems to be on right time
	public WeatherData(String stationCode, String latitude, String longitude, String dateTime, String precipitation) {
		this.latitude = latitude.replace(",", ".");
		this.longitude = longitude.replace(",", ".");
		this.time = dateTime.split(" ")[1];
		this.precipitation = Double.valueOf(precipitation.replace(",", "."));
		this.stationCode = stationCode;
	}

	public String getLatitude() {
		return latitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public String getLongitude() {
		return longitude;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public Double getPrecipitation() {
		return precipitation;
	}

	public void setPrecipitation(Double precipitation) {
		this.precipitation = precipitation;
	}

	public String getStationCode() {
		return stationCode;
	}

	public void setStationCode(String stationCode) {
		this.stationCode = stationCode;
	}
	
	@Override
	public String toString() {
		return "Weather data: " + stationCode + ", " + time + ", " + precipitation;
	}
}