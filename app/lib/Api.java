package lib;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class Api {

    // TODO XXX this is temporary.
    private static Set<String> nodes = new HashSet() {{
        add("http://127.0.0.1:12910/");
        add("http://127.0.0.1:12900/");
    }};

	public static final String ERROR_MSG_IO = "Could not connect to graylog2-server. Please make sure that it is running and you configured the correct REST URI.";

	private static AsyncHttpClient client;
	static {
		AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();
		builder.setAllowPoolingConnection(false);
		client = new AsyncHttpClient(builder.build());

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				client.close();
			}
		}));
	}

	public static <T> T get(URL url, Class<T> responseClass) throws APIException, IOException {
		try {
			AsyncHttpClient.BoundRequestBuilder requestBuilder = client.prepareGet(url.toString());
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

    public static <T> Set<T> getFromAllNodes(String resource, Class<T> responseClass) throws APIException, IOException {
        Set<T> result = Sets.newHashSet();

        for (String node : nodes) {
            URL url = buildTarget(node, resource);
            try {
                AsyncHttpClient.BoundRequestBuilder requestBuilder = client.prepareGet(url.toString());
                requestBuilder.addHeader("Accept", "application/json");
                final Response response = requestBuilder.execute().get();

                if (response.getStatusCode() != 200) {
                    throw new APIException(response.getStatusCode(), "REST call [" + url + "] returned " + response.getStatusText());
                }

                Gson gson = new Gson();
                result.add(gson.fromJson(response.getResponseBody("UTF-8"), responseClass));
            } catch (InterruptedException e) {
                // TODO
            } catch (ExecutionException e) {
                throw new APIException(-1, "REST call [" + url + "] failed: " + e.getMessage());
            } catch (MalformedURLException e) {
                throw new RuntimeException("Malformed URL.", e);
            }
        }

        return result;
    }

	public static <T> T get(String target, Class<T> responseClass) throws IOException, APIException {
		return get(buildTarget(Configuration.getServerRestUri(), target), responseClass);
	}

	public static URL buildTarget(String host, String part) throws MalformedURLException {
		return new URL(host + part);
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

}
