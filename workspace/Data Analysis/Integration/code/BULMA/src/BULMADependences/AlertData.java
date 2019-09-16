package BULMADependences;

import java.util.List;

public class AlertData extends WazeData {

	private static final long serialVersionUID = 1L;
	private static String DELIMITER = ",";
	
	//Alerts attributes
	private String alertID, alertDateTime, alertTime, alertDate, alertReportDescription, alertSubtype, alertType, alertRoadType, alertStreet, 
	alertLatitude, alertLongitude, alertNComments, alertNImages, alertNThumbsUp, alertReportMood, alertSpeed, distanceToClosestShapePoint;
	private int alertConfidence, alertReliability;
	private double alertReportRating;
	private boolean alertIsJamUnifiedAlert, alertInScale;
	private List<String> alertLocation;

	public AlertData(String id, String confidence, String inScale, String isJamUnifiedAlert, String location,
			String nComments, String nImages, String nThumbsUp, String publicationTime, String reliability,
			String reportDescription, String reportMood, String reportRating, String speed, String subtype, String type,
			String alertStreet, String roadType) {
		
		this.alertID = id;
		this.alertConfidence = Integer.valueOf(confidence);
		this.alertInScale = Boolean.valueOf(inScale);
		this.alertIsJamUnifiedAlert = Boolean.valueOf(isJamUnifiedAlert);
		this.alertLocation = getLatLon(location); // {"x":-34.945077,"y":-8.13114}
		this.alertLatitude = this.alertLocation.get(0); 
		this.alertLongitude = this.alertLocation.get(1);
		this.alertNComments = nComments;
		this.alertNImages = nImages;
		this.alertNThumbsUp = nThumbsUp;
		this.alertDateTime = getDateTimeFromMillis(publicationTime);
		this.alertTime = alertDateTime.split(" ")[1];
		this.alertDate = alertDateTime.split(" ")[0];
		this.alertReliability = Integer.valueOf(reliability);
		this.alertReportDescription = reportDescription;
		this.alertReportMood = reportMood;
		this.alertReportRating = Double.valueOf(reportRating);
		this.alertSpeed = speed;
		this.alertSubtype = subtype;
		this.alertType = type;
		this.alertRoadType = roadType;
		this.alertStreet = alertStreet;
	}
	
//	alertDate,alertSubtype,alertType,alertRoadType,alertConfidence,alertNComments,alertNImages,alertNThumbsUp,alertReliability,
//	alertReportMood,alertReportRating,alertSpeed,alertLatitude,alertLongitude,alertDistanceToClosestShapePoint,alertIsJamUnifiedAlert,
//	alertInScale,alertLocation
	
	//After integrated data: variables of interest
	public AlertData(String publicationTime, String subtype, String type, String roadType, String confidence, String nComments,
			String nImages, String nThumbsUp, String reliability, String reportMood, String reportRating, String speed, 
			String latitude, String longitude, String distanceToClosestShapePoint, String isJamUnifiedAlert, String inScale) {
		
		this.alertConfidence = Integer.valueOf(confidence);
		this.alertInScale = Boolean.valueOf(inScale);
		this.alertIsJamUnifiedAlert = Boolean.valueOf(isJamUnifiedAlert);
		this.alertLatitude = latitude; 
		this.alertLongitude = longitude;
		this.alertNComments = nComments;
		this.alertNImages = nImages;
		this.alertNThumbsUp = nThumbsUp;
		this.alertDateTime = publicationTime;
		this.alertTime = alertDateTime.split(" ")[1];
		this.alertDate = alertDateTime.split(" ")[0];
		this.alertReliability = Integer.valueOf(reliability);
		this.alertReportMood = reportMood;
		this.alertReportRating = Double.valueOf(reportRating);
		this.alertSpeed = speed;
		this.alertSubtype = subtype;
		this.alertType = type;
		this.alertRoadType = roadType;
		this.distanceToClosestShapePoint = distanceToClosestShapePoint;
	}
	
	public List<String> getAlertLocation() {
		return alertLocation;
	}

	public void setAlertLocation(List<String> alertLocation) {
		this.alertLocation = alertLocation;
	}
	
	public String getAlertID() {
		return alertID;
	}

	public void setAlertID(String alertID) {
		this.alertID = alertID;
	}

	public String getAlertLatitude() {
		return alertLatitude;
	}

	public void setAlertLatitude(String alertLatitude) {
		this.alertLatitude = alertLatitude;
	}

	public String getAlertLongitude() {
		return alertLongitude;
	}

	public void setAlertLongitude(String alertLongitude) {
		this.alertLongitude = alertLongitude;
	}

	public String getAlertDateTime() {
		return alertDateTime;
	}

	public void setAlertDateTime(String alertDateTime) {
		this.alertDateTime = alertDateTime;
	}
	
	public String getAlertReportDescription() {
		return alertReportDescription;
	}

	public void setAlertReportDescription(String alertReportDescription) {
		this.alertReportDescription = alertReportDescription;
	}

	public String getAlertSubtype() {
		return alertSubtype;
	}

	public void setAlertSubtype(String alertSubtype) {
		this.alertSubtype = alertSubtype;
	}

	public String getAlertType() {
		return alertType;
	}

	public void setAlertType(String alertType) {
		this.alertType = alertType;
	}

	public String getAlertRoadType() {
		return alertRoadType;
	}

	public void setAlertRoadType(String alertRoadType) {
		this.alertRoadType = alertRoadType;
	}

	public int getAlertConfidence() {
		return alertConfidence;
	}

	public void setAlertConfidence(int alertConfidence) {
		this.alertConfidence = alertConfidence;
	}

	public boolean isAlertInScale() {
		return alertInScale;
	}

	public void setAlertInScale(boolean alertInScale) {
		this.alertInScale = alertInScale;
	}

	public String getAlertNComments() {
		return alertNComments;
	}

	public void setAlertNComments(String alertNComments) {
		this.alertNComments = alertNComments;
	}

	public String getAlertNImages() {
		return alertNImages;
	}

	public void setAlertNImages(String alertNImages) {
		this.alertNImages = alertNImages;
	}

	public String getAlertNThumbsUp() {
		return alertNThumbsUp;
	}

	public void setAlertNThumbsUp(String alertNThumbsUp) {
		this.alertNThumbsUp = alertNThumbsUp;
	}

	public int getAlertReliability() {
		return alertReliability;
	}

	public void setAlertReliability(int alertReliability) {
		this.alertReliability = alertReliability;
	}

	public String getAlertReportMood() {
		return alertReportMood;
	}

	public void setAlertReportMood(String alertReportMood) {
		this.alertReportMood = alertReportMood;
	}

	public double getAlertReportRating() {
		return alertReportRating;
	}

	public void setAlertReportRating(double alertReportRating) {
		this.alertReportRating = alertReportRating;
	}

	public String getAlertSpeed() {
		return alertSpeed;
	}

	public void setAlertSpeed(String alertSpeed) {
		this.alertSpeed = alertSpeed;
	}

	public boolean isAlertIsJamUnifiedAlert() {
		return alertIsJamUnifiedAlert;
	}

	public void setAlertIsJamUnifiedAlert(boolean alertIsJamUnifiedAlert) {
		this.alertIsJamUnifiedAlert = alertIsJamUnifiedAlert;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public String getAlertTime() {
		return alertTime;
	}

	public void setAlertTime(String alertTime) {
		this.alertTime = alertTime;
	}

	public String getAlertDate() {
		return alertDate;
	}

	public void setAlertDate(String alertDate) {
		this.alertDate = alertDate;
	}

	public String getDistanceToClosestShapePoint() {
		return distanceToClosestShapePoint;
	}

	public void setDistanceToClosestShapePoint(String distanceToClosestShapePoint) {
		this.distanceToClosestShapePoint = distanceToClosestShapePoint;
	}

	public String getAlertStreet() {
		return alertStreet;
	}

	public void setAlertStreet(String alertStreet) {
		this.alertStreet = alertStreet;
	}

//	alertDateTime,"
//	+ "alertSubtype,alertType,alertRoadType,alertConfidence,alertNComments,alertNImages,alertNThumbsUp,alertReliability,alertReportMood,alertReportRating,alertSpeed,alertLatitude,"
//	+ "alertLongitude,alertDistanceToClosestShapePoint,alertIsJamUnifiedAlert,alertInScale
	public static String getDefaultAlert(String gpsDateTime, String gpsLat, String gpsLon, String gps2ShapeDistance) {
		String alertDateTime = gpsDateTime;
		String alertSubtype = "NORMAL";
		String alertType = "NORMAL";
		String alertRoadType = "-";
		int alertConfidence = 5;
		String alertNComments = "-";
		String alertNImages = "-";
		String alertNThumbsUp = "-";
		int alertReliability = 10;
		String alertReportMood = "-";
		int alertReportRating = 5;
		String alertSpeed = "-";
		String alertLatitude = gpsLat;
		String alertLongitude = gpsLon;
		String alertDistanceToClosestShapePoint = gps2ShapeDistance;
		String alertIsJamUnifiedAlert = "-";
		String alertInScale = "-";
		String newAlertData = alertDateTime + DELIMITER + alertSubtype + DELIMITER + alertType  + DELIMITER + alertRoadType 
				+ DELIMITER + alertConfidence + DELIMITER + alertNComments + DELIMITER + alertNImages + DELIMITER + alertNThumbsUp
				 + DELIMITER +  alertReliability + DELIMITER + alertReportMood + DELIMITER + alertReportRating + DELIMITER +  alertSpeed 
				 + DELIMITER + alertLatitude + DELIMITER + alertLongitude + DELIMITER + alertDistanceToClosestShapePoint + DELIMITER +
				 alertIsJamUnifiedAlert + DELIMITER + alertInScale;
		
		return newAlertData;
	}
	
	//alertDate,alertSubtype,alertType,alertRoadType,alertConfidence,alertNComments,alertNImages,alertNThumbsUp,alertReliability,alertReportMood,alertReportRating,alertSpeed,alertLatitude,alertLongitude,distanceToClosestShapePoint,alertIsJamUnifiedAlert,alertInScale
	public String getDataString() {
		return  alertDateTime + SEPARATOR + alertSubtype + SEPARATOR + alertType + SEPARATOR + alertRoadType + SEPARATOR + 
				alertConfidence + SEPARATOR + alertNComments + SEPARATOR + alertNImages + SEPARATOR + alertNThumbsUp + 
				SEPARATOR + alertReliability + SEPARATOR + alertReportMood + SEPARATOR + alertReportRating + SEPARATOR +
				alertSpeed + SEPARATOR + alertLatitude + SEPARATOR + alertLongitude + SEPARATOR + distanceToClosestShapePoint +
				SEPARATOR + alertIsJamUnifiedAlert + SEPARATOR + alertInScale;
	}
	
	@Override
	public String toString() {
		return "AlertData [alertDate=" + alertDateTime + ", alertSubtype=" + alertSubtype + ", alertType=" + alertType
				+ ", alertRoadType=" + alertRoadType + ", alertCondidence=" + alertConfidence + ", alertNComments="
				+ alertNComments + ", alertNImages=" + alertNImages + ", alertNThumbsUp=" + alertNThumbsUp
				+ ", alertReliability=" + alertReliability + ", alertReportMood=" + alertReportMood
				+ ", alertReportRating=" + alertReportRating + ", alertSpeed=" + alertSpeed + ", alertLatitude="
				+ alertLatitude + ", alertLongitude=" + alertLongitude + ", distanceToClosestShapePoint="
				+ distanceToClosestShapePoint + ", alertIsJamUnifiedAlert=" + alertIsJamUnifiedAlert + ", alertInScale="
				+ alertInScale + ", alertLocation=" + alertLocation + "]";
	}
}