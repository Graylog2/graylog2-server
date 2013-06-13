package lib;

import com.google.gson.Gson;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutionException;

public class Api {

	public static final String ERROR_MSG_IO = "Could not connect to graylog2-server. Please make sure that it is running and you configured the correct REST URI.";

	public static <T> T get(URL url, Class<T> responseClass) throws APIException, IOException {
		try {
			AsyncHttpClient.BoundRequestBuilder requestBuilder = getClient().prepareGet(url.toString());
			requestBuilder.addHeader("Accept", "application/json");
			final Response response = requestBuilder.execute().get();

			if (response.getStatusCode() != 200) {
				throw new APIException(response.getStatusCode(), "REST call [" + url + "] returned " + response.getStatusText());
			}

			Gson gson = new Gson();
			return gson.fromJson(response.getResponseBody("UTF-8"), responseClass);
		} catch (InterruptedException e) {
			// TODO
		} catch (ExecutionException e) {
			throw new APIException(-1, "REST call [" + url + "] failed: " + e.getMessage());
		} catch (MalformedURLException e) {
			throw new RuntimeException("Malformed URL.", e);
		}
		return (T) null;
	}

	public static <T> T get(String target, Class<T> responseClass) throws IOException, APIException {
		return get(buildTarget(target), responseClass);
	}

	public static URL buildTarget(String part) throws MalformedURLException {
		return new URL(Configuration.getServerRestUri() + part);
	}

	public static String urlEncode(String x) {
		if (x == null || x.isEmpty()) {
			return "";
		}

		try {
			return URLEncoder.encode(x, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Unsupported Encoding");
		}
	}

	// TODO make cached
	private static AsyncHttpClient getClient() {
		AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();
		builder.setAllowPoolingConnection(true);
		return new AsyncHttpClient(builder.build());
	}
}
