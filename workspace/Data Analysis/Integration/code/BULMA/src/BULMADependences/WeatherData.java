package BULMADependences;

public class WeatherData {
	
	private String latitude;
	private String longitude;
	private String time;
	private String precipitation;
	private String stationCode;
	
	public WeatherData(String latitude, String longitude, String dateTime, String precipitation, String stationCode) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.time = dateTime;
		this.precipitation = precipitation;
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

	public String getPrecipitation() {
		return precipitation;
	}

	public void setPrecipitation(String precipitation) {
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