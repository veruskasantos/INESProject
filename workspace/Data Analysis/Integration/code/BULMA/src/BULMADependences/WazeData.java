package BULMADependences;

import java.io.Serializable;

public class WazeData implements Serializable{

	private static final long serialVersionUID = 1L;

	//Alerts
	public WazeData(String id, String confidence, String inScale, String isJamUnifiedAlert, String location, String nComments, String nImages, 
			String nThumbsUp, String publicationTime, String reliability, String reportDescription, String reportMood, String reportRating, 
			String speed, String subtype, String type, String roadType) {
		// TODO Auto-generated constructor stub
	}
	
	//Jams
	public WazeData(String id, String delay, String length, String level, String lineCoordinates, String updateTime, String roadType, String severity, 
			String speedKMH, String blockDescription, String expirationTime, String blockType) {
		// TODO Auto-generated constructor stub
	}

}
