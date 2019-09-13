package BULMADependences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import scala.Tuple2;

public class JamData extends WazeData {

	private static final long serialVersionUID = 1L;
	private static String DELIMITER = ",";
	
	//Jam attributes
	private String jamID, jamUpdateDateTime, jamUpdateTime, jamUpdateDate, jamBlockDesc, jamExpirationDateTime, jamExpirationTime, jamExpirationDate, jamBlockType, jamStreet;
	private ArrayList<String> jamCoordinates;
	private int jamDelay, jamLength, jamLevel, jamSeverity;
	private double jamSpeedKM, distanceToClosestShapePoint;

	public JamData(String id, String delay, String length, String level, String lineCoordinates, String severity,
			String speedKMH, String updateDateTime, String blockDescription, String expirationDateTime,
			String blockType, String streetName) {
		
		this.jamID = id;
		this.jamDelay = Integer.valueOf(delay);
		this.jamLength = Integer.valueOf(length);
		this.jamLevel = Integer.valueOf(level);
		this.jamCoordinates = new ArrayList<String>(Arrays.asList(lineCoordinates.replace("[", "").replaceAll("\\},\\{", "};{").split(";"))); // [{"x":-34.906513,"y":-8.139684},{"x":-34.905742,"y":-8.139735},{"x":-34.905688,"y":-8.139741},{"x":-34.904807,"y":-8.13983}]
		this.jamSeverity = Integer.valueOf(severity);
		this.jamSpeedKM = Double.valueOf(speedKMH);
		this.jamBlockDesc = blockDescription;
		
		if (blockType != null && blockType != "NONE" && !blockType.equals("-") && !blockType.isEmpty()) { 
			this.jamBlockType = blockType;
		} else {
			this.jamBlockType = "-";
		}
		
		this.jamUpdateDateTime = getDateTimeFromMillis(updateDateTime);
		this.jamUpdateTime = jamUpdateDateTime.split(" ")[1];
		this.jamUpdateDate = jamUpdateDateTime.split(" ")[0];
		
		if (expirationDateTime != null && expirationDateTime != "NONE" && !expirationDateTime.equals("-") && !expirationDateTime.isEmpty()) { 
			this.jamExpirationDateTime = getDateTimeFromMillis(expirationDateTime);
			this.jamExpirationTime = jamUpdateDateTime.split(" ")[1];
			this.jamExpirationDate = jamUpdateDateTime.split(" ")[0];
		} else {
			this.jamExpirationDateTime = "-";
		}
		
		this.jamStreet = streetName;
	}
	
	//jamUpdateDate,jamExpirationDateTime,jamBlockType,jamDelay,jamLength,jamLevel,jamSeverity,jamSpeedKM,jamDistanceToClosestShapePoint
	public JamData(String updateDateTime, String expirationDateTime, String blockType, String delay, String length, 
			String level, String severity, String speedKMH, String distanceToClosestShapePoint) {
		
		this.jamDelay = Integer.valueOf(delay);
		this.jamLength = Integer.valueOf(length);
		this.jamLevel = Integer.valueOf(level);
		this.jamSeverity = Integer.valueOf(severity);
		this.jamSpeedKM = Double.valueOf(speedKMH);

		if (blockType != null && blockType != "NONE" && !blockType.equals("-") && !blockType.isEmpty()) { 
			this.jamBlockType = blockType;
		} else {
			this.jamBlockType = "-";
		}
		
		this.jamUpdateDateTime = updateDateTime;
		this.jamUpdateTime = jamUpdateDateTime.split(" ")[1];
		this.jamUpdateDate = jamUpdateDateTime.split(" ")[0];
		
		if (expirationDateTime != null && !expirationDateTime.equals("-") && !expirationDateTime.isEmpty()) { 
			this.jamExpirationDateTime = expirationDateTime;
			this.jamExpirationTime = jamUpdateDateTime.split(" ")[1];
			this.jamExpirationDate = jamUpdateDateTime.split(" ")[0];
		} else {
			this.jamExpirationDateTime = "-";
		}
		this.distanceToClosestShapePoint = Double.valueOf(distanceToClosestShapePoint);
		
	}
	
	public List<Tuple2<Double, Double>> getJamLatLon() {
		List<Tuple2<Double, Double>> outputLatLonList = new ArrayList<Tuple2<Double,Double>>();
		
		for (String coordinates : this.jamCoordinates) {
			List<Double> latLon = getLatLon(coordinates);
			Tuple2<Double,Double> latLonTuple = new Tuple2<Double, Double>(latLon.get(0), latLon.get(1));
			outputLatLonList.add(latLonTuple);
		}
		return outputLatLonList;
	}
	
	public String getJamID() {
		return jamID;
	}

	public void setJamID(String jamID) {
		this.jamID = jamID;
	}

	public ArrayList<String> getJamCoordinates() {
		return jamCoordinates;
	}

	public void setJamCoordinates(ArrayList<String> jamCoordinates) {
		this.jamCoordinates = jamCoordinates;
	}

	public String getJamDateTime() {
		return jamUpdateDateTime;
	}

	public void setJamDateTime(String jamTime) {
		this.jamUpdateDateTime = jamTime;
	}

	public String getJamBlockDesc() {
		return jamBlockDesc;
	}

	public void setJamBlockDesc(String jamBlockDesc) {
		this.jamBlockDesc = jamBlockDesc;
	}

	public String getJamExpirationDateTime() {
		return jamExpirationDateTime;
	}

	public void setJamExpirationDateTime(String jamExpirationTime) {
		this.jamExpirationDateTime = jamExpirationTime;
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
	
	public String getJamUpdateDateTime() {
		return jamUpdateDateTime;
	}

	public void setJamUpdateDateTime(String jamUpdateDateTime) {
		this.jamUpdateDateTime = jamUpdateDateTime;
	}

	public String getJamUpdateTime() {
		return jamUpdateTime;
	}

	public void setJamUpdateTime(String jamUpdateTime) {
		this.jamUpdateTime = jamUpdateTime;
	}

	public String getJamUpdateDate() {
		return jamUpdateDate;
	}

	public void setJamUpdateDate(String jamUpdateDate) {
		this.jamUpdateDate = jamUpdateDate;
	}

	public String getJamExpirationTime() {
		return jamExpirationTime;
	}

	public void setJamExpirationTime(String jamExpirationTime) {
		this.jamExpirationTime = jamExpirationTime;
	}

	public String getJamExpirationDate() {
		return jamExpirationDate;
	}

	public void setJamExpirationDate(String jamExpirationDate) {
		this.jamExpirationDate = jamExpirationDate;
	}

	public double getDistanceToClosestShapePoint() {
		return distanceToClosestShapePoint;
	}

	public void setDistanceToClosestShapePoint(double distanceToClosestShapePoint) {
		this.distanceToClosestShapePoint = distanceToClosestShapePoint;
	}

	public String getJamStreet() {
		return jamStreet;
	}

	public void setJamStreet(String jamStreet) {
		this.jamStreet = jamStreet;
	}

//	jamUpdateDateTime,jamExpirationDateTime,jamBlockType,"
//+ "jamDelay,jamLength,jamLevel,jamSeverity,jamSpeedKM,jamDistanceToClosestShapePoint"
	public static String getDefaultJam(String gps_datetime, String distanceToShapePoint) {
		String jamUpdateDateTime = gps_datetime;
		String jamExpirationDateTime = "-";
		String jamBlockType = "NORMAL";
		int jamDelay = 0;
		int jamLength = 0;
		int jamLevel = 0; 
		int jamSeverity = 0;
		String jamSpeedKM = "-";
		String jamDistanceToClosestShapePoint = distanceToShapePoint;
		
		String newJamData = jamUpdateDateTime + DELIMITER + jamExpirationDateTime + DELIMITER + jamBlockType + DELIMITER + jamDelay + 
				DELIMITER + jamLength + DELIMITER + jamLevel + DELIMITER + jamSeverity + DELIMITER + jamSpeedKM + DELIMITER + jamDistanceToClosestShapePoint;
		return newJamData;
	}
	
	//jamUpdateDate,jamExpirationDateTime,jamBlockType,jamDelay,jamLength,jamLevel,jamSeverity,jamSpeedKM,distanceToClosestShapePoint
	public String getDataString() {
		return jamUpdateDateTime + SEPARATOR + jamExpirationDateTime + SEPARATOR + jamBlockType + SEPARATOR + 
				jamDelay + SEPARATOR + jamLength + SEPARATOR + jamLevel + SEPARATOR + jamSeverity + SEPARATOR + 
				jamSpeedKM + SEPARATOR + distanceToClosestShapePoint;
	}
	
	@Override
	public String toString() {
		return "JamData [jamUpdateDate=" + jamUpdateDate + ", jamExpirationDateTime=" + jamExpirationDateTime
				+ ", jamBlockType=" + jamBlockType + ", jamCoordinates=" + jamCoordinates + ", jamDelay=" + jamDelay
				+ ", jamLength=" + jamLength + ", jamLevel=" + jamLevel + ", jamSeverity=" + jamSeverity
				+ ", jamSpeedKM=" + jamSpeedKM + ", distanceToClosestShapePoint=" + distanceToClosestShapePoint + "]";
	}
}