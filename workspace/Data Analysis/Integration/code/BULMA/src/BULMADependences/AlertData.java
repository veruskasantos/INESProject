package BULMADependences;

import java.util.List;

public class AlertData extends WazeData {

	private static final long serialVersionUID = 1L;
	//Alerts attributes
	private String alertID, alertDateTime, alertTime, alertDate, alertReportDescription, alertSubtype, alertType, alertRoadType, alertStreet;
	private int alertConfidence, alertNComments, alertNImages, alertNThumbsUp, alertReliability, alertReportMood;
	private double alertReportRating, alertSpeed, alertLatitude, alertLongitude, distanceToClosestShapePoint;
	private boolean alertIsJamUnifiedAlert, alertInScale;
	private List<Double> alertLocation;

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
		this.alertNComments = Integer.valueOf(nComments);
		this.alertNImages = Integer.valueOf(nImages);
		this.alertNThumbsUp = Integer.valueOf(nThumbsUp);
		this.alertDateTime = getDateTimeFromMillis(publicationTime);
		this.alertTime = alertDateTime.split(" ")[1];
		this.alertDate = alertDateTime.split(" ")[0];
		this.alertReliability = Integer.valueOf(reliability);
		this.alertReportDescription = reportDescription;
		this.alertReportMood = Integer.valueOf(reportMood);
		this.alertReportRating = Double.valueOf(reportRating);
		this.alertSpeed = Double.valueOf(speed);
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
		this.alertLatitude = Double.valueOf(latitude); 
		this.alertLongitude = Double.valueOf(longitude);
		this.alertNComments = Integer.valueOf(nComments);
		this.alertNImages = Integer.valueOf(nImages);
		this.alertNThumbsUp = Integer.valueOf(nThumbsUp);
		this.alertDateTime = publicationTime;
		this.alertTime = alertDateTime.split(" ")[1];
		this.alertDate = alertDateTime.split(" ")[0];
		this.alertReliability = Integer.valueOf(reliability);
		this.alertReportMood = Integer.valueOf(reportMood);
		this.alertReportRating = Double.valueOf(reportRating);
		this.alertSpeed = Double.valueOf(speed);
		this.alertSubtype = subtype;
		this.alertType = type;
		this.alertRoadType = roadType;
		this.distanceToClosestShapePoint = Double.valueOf(distanceToClosestShapePoint);
	}
	
	public List<Double> getAlertLocation() {
		return alertLocation;
	}

	public void setAlertLocation(List<Double> alertLocation) {
		this.alertLocation = alertLocation;
	}
	
	public String getAlertID() {
		return alertID;
	}

	public void setAlertID(String alertID) {
		this.alertID = alertID;
	}

	public Double getAlertLatitude() {
		return alertLatitude;
	}

	public void setAlertLatitude(Double alertLatitude) {
		this.alertLatitude = alertLatitude;
	}

	public Double getAlertLongitude() {
		return alertLongitude;
	}

	public void setAlertLongitude(Double alertLongitude) {
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

	public int getAlertNComments() {
		return alertNComments;
	}

	public void setAlertNComments(int alertNComments) {
		this.alertNComments = alertNComments;
	}

	public int getAlertNImages() {
		return alertNImages;
	}

	public void setAlertNImages(int alertNImages) {
		this.alertNImages = alertNImages;
	}

	public int getAlertNThumbsUp() {
		return alertNThumbsUp;
	}

	public void setAlertNThumbsUp(int alertNThumbsUp) {
		this.alertNThumbsUp = alertNThumbsUp;
	}

	public int getAlertReliability() {
		return alertReliability;
	}

	public void setAlertReliability(int alertReliability) {
		this.alertReliability = alertReliability;
	}

	public int getAlertReportMood() {
		return alertReportMood;
	}

	public void setAlertReportMood(int alertReportMood) {
		this.alertReportMood = alertReportMood;
	}

	public double getAlertReportRating() {
		return alertReportRating;
	}

	public void setAlertReportRating(double alertReportRating) {
		this.alertReportRating = alertReportRating;
	}

	public double getAlertSpeed() {
		return alertSpeed;
	}

	public void setAlertSpeed(double alertSpeed) {
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

	public double getDistanceToClosestShapePoint() {
		return distanceToClosestShapePoint;
	}

	public void setDistanceToClosestShapePoint(double distanceToClosestShapePoint) {
		this.distanceToClosestShapePoint = distanceToClosestShapePoint;
	}

	public String getAlertStreet() {
		return alertStreet;
	}

	public void setAlertStreet(String alertStreet) {
		this.alertStreet = alertStreet;
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
