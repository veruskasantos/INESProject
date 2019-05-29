package BULMADependences;

import java.io.Serializable;

public class WazeData implements Serializable{

	private static final long serialVersionUID = 1L;
	
	//Alerts attributes
	private String alertID, alertLatitude, alertLongitude, alertTime, alertReportDescription, alertSubtype, alertType, alertRoadType;
	private int alertCondidence, alertInScale, alertNComments, alertNImages, alertNThumbsUp, alertReliability, alertReportMood;
	private double alertReportRating, alertSpeed;
	private boolean alertIsJamUnifiedAlert;

	//Jam attributes
	private String jamID, jamCoordinates, jamTime, jamBlockDesc, jamExpirationTime, jamBlockType;
	private int jamDelay, jamLength, jamLevel, jamSeverity;
	private double jamSpeedKM;
	
	//Alerts
	public WazeData(String id, String confidence, String inScale, String isJamUnifiedAlert, String location, String nComments, String nImages, 
			String nThumbsUp, String publicationTime, String reliability, String reportDescription, String reportMood, String reportRating, 
			String speed, String subtype, String type, String roadType) {
		this.alertID = id;
		this.alertCondidence = Integer.valueOf(confidence);
		this.alertInScale = Integer.valueOf(inScale);
		this.alertIsJamUnifiedAlert = Boolean.valueOf(isJamUnifiedAlert);
		this.alertLatitude = location; //todo
		this.alertLongitude = location;
		this.alertNComments = Integer.valueOf(nComments);
		this.alertNImages = Integer.valueOf(nImages);
		this.alertNThumbsUp = Integer.valueOf(nThumbsUp);
		this.alertTime = publicationTime; //convert
		this.alertReliability = Integer.valueOf(reliability);
		this.alertReportDescription = reportDescription;
		this.alertReportMood = Integer.valueOf(reportMood);
		this.alertReportRating = Double.valueOf(reportRating);
		this.alertSpeed = Double.valueOf(speed);
		this.alertSubtype = subtype;
		this.alertType = type;
		this.alertRoadType = roadType;
		
	}
	
	//Jams
	public WazeData(String id, String delay, String length, String level, String lineCoordinates, String severity, 
			String speedKMH, String updateTime, String blockDescription, String expirationTime, String blockType) {
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

	public String getAlertTime() {
		return alertTime;
	}

	public void setAlertTime(String alertTime) {
		this.alertTime = alertTime;
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

	public int getAlertCondidence() {
		return alertCondidence;
	}

	public void setAlertCondidence(int alertCondidence) {
		this.alertCondidence = alertCondidence;
	}

	public int getAlertInScale() {
		return alertInScale;
	}

	public void setAlertInScale(int alertInScale) {
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

	public String getJamID() {
		return jamID;
	}

	public void setJamID(String jamID) {
		this.jamID = jamID;
	}

	public String getJamCoordinates() {
		return jamCoordinates;
	}

	public void setJamCoordinates(String jamCoordinates) {
		this.jamCoordinates = jamCoordinates;
	}

	public String getJamTime() {
		return jamTime;
	}

	public void setJamTime(String jamTime) {
		this.jamTime = jamTime;
	}

	public String getJamBlockDesc() {
		return jamBlockDesc;
	}

	public void setJamBlockDesc(String jamBlockDesc) {
		this.jamBlockDesc = jamBlockDesc;
	}

	public String getJamExpirationTime() {
		return jamExpirationTime;
	}

	public void setJamExpirationTime(String jamExpirationTime) {
		this.jamExpirationTime = jamExpirationTime;
	}

	public String getJamBlockType() {
		return jamBlockType;
	}

	public void setJamBlockType(String jamBlockType) {
		this.jamBlockType = jamBlockType;
	}

	public int getJamDelay() {
		return jamDelay;
	}

	public void setJamDelay(int jamDelay) {
		this.jamDelay = jamDelay;
	}

	public int getJamLength() {
		return jamLength;
	}

	public void setJamLength(int jamLength) {
		this.jamLength = jamLength;
	}

	public int getJamLevel() {
		return jamLevel;
	}

	public void setJamLevel(int jamLevel) {
		this.jamLevel = jamLevel;
	}

	public int getJamSeverity() {
		return jamSeverity;
	}

	public void setJamSeverity(int jamSeverity) {
		this.jamSeverity = jamSeverity;
	}

	public double getJamSpeedKM() {
		return jamSpeedKM;
	}

	public void setJamSpeedKM(double jamSpeedKM) {
		this.jamSpeedKM = jamSpeedKM;
	}


}
