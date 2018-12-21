import java.io.IOException;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mashape.unirest.http.exceptions.UnirestException;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Veruska
 * Crawler to get CG GPS data each time interval from website and to save on MongoDB.
 *
 */

public class CGDataCollect {

//	private static final String[] GPS_HEADER = { "bus.code", "latitude",
//			"longitude", "timestamp", "line.code", "gps.id" };
	private static final String[] ROUTES = { "\"m63\"", "\"hbg\"", "\"hc5\"", "\"hbt\"", "\"hc3\"", "\"hcd\"", "\"hc6\"", "\"hb9\"", 
		"\"hbu\"", "\"hbn\"", "\"hc1\"", "\"hbv\"", "\"hb8\"", "\"hbo\"", "\"hbb\"", "\"hbl\"", "\"hbr\"", "\"ns5\"", "\"hc9\"", 
		"\"hca\"", "\"hba\"", "\"hc4\"", "\"hbd\"", "\"hce\"", "\"hcb\"", "\"hc8\"", "\"hc7\"", "\"hbs\"", "\"hbp\"", "\"hbm\"", 
		"\"hbc\"", "\"rps\"", "\"hcf\"", "\"hcg\"", "\"hbe\"", "\"hbi\"", "\"hcc\"", "\"m5r\"", "\"hbj\"", "\"m66\"", "\"hbk\"", "\"hc2\""};

	public static void main(String[] args) throws IOException, UnirestException, InterruptedException {

		if (args.length != 1) {
			System.err.print("Usage: java -jar <fileName.jar> <interval_in_sec>");
			System.exit(1);
		}
		
		Integer interval = Integer.valueOf(args[0]) * 1000; // convert sec to ms
		
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger rootLogger = loggerContext.getLogger("org.mongodb.driver");
		rootLogger.setLevel(Level.OFF);
		
		DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
		Date currentDate;
		
		OkHttpClient client = new OkHttpClient();
		MediaType mediaType = MediaType.parse("application/json");
		
		String date;
		boolean execute = true;
		
		while (execute) {
			MongoClientURI connectionString = new MongoClientURI("mongodb://127.0.0.1:27017");
			MongoClient mongoClient = new MongoClient(connectionString);
			
			Request request = null;
			Response response = null;
			
			currentDate = new Date();
			date = dateFormat.format(currentDate);
			System.out.println("Collecting Campina Grande GPS data - " + date);
			
			MongoDatabase database = mongoClient.getDatabase("gps_data");
			
			for (String route : ROUTES) { //each route
				RequestBody body = RequestBody.create(mediaType,
						"{\"route\":" + route + ",\"project\": \"54byr\"}");
				request = new Request.Builder()
						.url("https://editor.mobilibus.com/web/refresh-vehicle-in-route/")
						.post(body)
						.addHeader("content-type", "application/json")
						.addHeader("cache-control", "no-cache")
						.addHeader("postman-token",
								"37c3ff3f-6fe4-c533-9559-6dccdead2970").build();

				response = client.newCall(request).execute();
				String dados = response.body().string();
			
				if (dados != null && dados.startsWith("{")) {
					MongoCollection<Document> cgCollection = database.getCollection("GPS-CampinaGrande-" + date);

					try {
						JSONObject json = new JSONObject(dados);
						String[] shapes = JSONObject.getNames(json);

						if (shapes != null && shapes.length > 0) {
							for (int i = 0; i < shapes.length; i++) { //each shape
								JSONArray jsonArrayBus = json.getJSONArray(shapes[i]);

								for (int j = 0; j < jsonArrayBus.length(); j++) { // each bus
									JSONObject vehicle = (JSONObject) jsonArrayBus.get(j);
									
									cgCollection.insertOne(Document.parse(vehicle.toString()));
								}
							}
						}
					} catch (JSONException e) {
						System.err.print(e.getMessage());
					}
				}
			}
			
			mongoClient.close();
			Thread.sleep(interval);
		}
		System.out.println("Done!");
	}
}