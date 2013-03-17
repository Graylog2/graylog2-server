package lib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Api {

	public static <T> T get(URL url, T t) throws IOException, APIException {
		try {
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			
			if (conn.getResponseCode() != 200) {
				conn.disconnect();
				throw new APIException(conn.getResponseCode(), "REST call [" + url + "] returned " + conn.getResponseCode());
			}
			
			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			
			StringBuilder sb = new StringBuilder();
			String output;
			while ((output = br.readLine()) != null) {
				sb.append(output);
			}

			conn.disconnect();
			Gson gson = new Gson();
			return (T) gson.fromJson(sb.toString(), t.getClass());
		} catch (MalformedURLException e) {
			throw new RuntimeException("Malformed URL.", e);
		}
	}
	
	public static URL buildTarget(String part) throws MalformedURLException {
		return new URL(Configuration.getServerRestUri() + part);
	}
	
	public static String urlEncode(String x) {
		try {
			return URLEncoder.encode(x, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Unsupported Encoding");
		}
	}

}
