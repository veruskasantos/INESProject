package integration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

import org.json.JSONObject;
import org.json.JSONArray;

public class ConvertLatLng {

	public static void main(String[] args) {
		try {
			double lat = -8.02199367118403;
			double lng = -34.9068620022944;
			// String out = getStreetNameOSM(
			// "https://nominatim.openstreetmap.org/reverse?format=geojson&lat=" + lat +
			// "&lon=" + lng);

			String out = getStreetNameHERE("https://reverse.geocoder.api.here.com/6.2/reversegeocode.json?prox=" + lat
					+ "," + lng
					+ ",250&mode=retrieveAddresses&maxresults=1&gen=9&app_id=KMr9OUaKNVSNVeorfjy1&app_code=m04i3MJhV2gk-8VzUukETw");

			System.out.println(out);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String getStreetNameHERE(String urlQueryString) {
		JSONObject json = null;
		String street = "";
		try {
			URL url = new URL(urlQueryString);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setInstanceFollowRedirects(false);
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.addRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64)");
			connection.setRequestProperty("charset", "utf-8");
			connection.connect();
			InputStream inStream = connection.getInputStream();

			BufferedReader rd = new BufferedReader(new InputStreamReader(inStream, Charset.forName("UTF-8")));
			StringBuilder sb = new StringBuilder();
			int cp;
			while ((cp = rd.read()) != -1) {
				sb.append((char) cp);
			}
			json = new JSONObject(sb.toString());

			// System.out.println(i++ + sb.toString());

			try { // when there are multiple roads, get the first one
				street = (String) (json.getJSONObject("Response").getJSONArray("View").getJSONObject(0)
						.getJSONArray("Result").getJSONObject(0).getJSONObject("Location").getJSONObject("Address")
						.get("Street"));
			} catch (Exception e) {
				// there is no road name for this lat/lng
			}

			connection.disconnect();

		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return street;
	}

	public static String getStreetNameOSM(String urlQueryString) {
		JSONObject json = null;
		String street = null;
		try {
			URL url = new URL(urlQueryString);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setInstanceFollowRedirects(false);
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
			connection.setRequestProperty("charset", "utf-8");
			connection.connect();
			InputStream inStream = connection.getInputStream();

			BufferedReader rd = new BufferedReader(new InputStreamReader(inStream, Charset.forName("UTF-8")));
			StringBuilder sb = new StringBuilder();
			int cp;
			while ((cp = rd.read()) != -1) {
				sb.append((char) cp);
			}
			json = new JSONObject(sb.toString());
			street = (String) ((JSONObject) ((JSONObject) ((JSONObject) ((JSONArray) json.get("features")).get(0))
					.get("properties")).get("address")).get("road");

		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return street;
	}
}
