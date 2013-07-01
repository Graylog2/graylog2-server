package lib;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Response;
import com.ning.http.client.StringPart;
import models.Node;
import models.api.requests.ApiRequest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class Api {

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

    public static <T> T put(URL url, Class<T> responseClass) throws APIException, IOException {
        try {
            AsyncHttpClient.BoundRequestBuilder requestBuilder = client.preparePut(url.toString());
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

    public static <T> T post(URL url, String body, int expectedResponseCode, Class<T> responseClass) throws APIException, IOException {
        try {
            AsyncHttpClient.BoundRequestBuilder requestBuilder = client.preparePost(url.toString());
            requestBuilder.addHeader("Accept", "application/json");
            requestBuilder.setBody(body);
            final Response response = requestBuilder.execute().get();

            if (response.getStatusCode() != expectedResponseCode) {
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





    public static <T> List<T> getFromAllNodes(String resource, Class<T> responseClass) throws APIException, IOException {
        List<T> result = Lists.newArrayList();

        for (String node : Configuration.getServerRestUris()) {
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




    ////////

    public static <T> T get(String part, Class<T> responseClass) throws IOException, APIException {
        return get(buildTarget(Node.random(), part), responseClass);
    }

	public static <T> T get(Node node, String part, Class<T> responseClass) throws IOException, APIException {
        return get(buildTarget(node, part), responseClass);
    }

    public static <T> T get(String host, String part, Class<T> responseClass) throws IOException, APIException {
        return get(buildTarget(host, part), responseClass);
    }

    public static <T> T post(Node node, String part, ApiRequest body, Class<T> responseClass) throws IOException, APIException {
        return post(buildTarget(node, part), body.toJson(), 201, responseClass);
    }

    public static <T> T post(Node node, String part, ApiRequest body, int expectedResponseCode, Class<T> responseClass) throws IOException, APIException {
        return post(buildTarget(node, part), body.toJson(), expectedResponseCode, responseClass);
    }

    public static <T> T put(Node node, String part, Class<T> responseClass) throws IOException, APIException {
        return put(buildTarget(node, part), responseClass);
    }

    public static <T> T put(String host, String part, Class<T> responseClass) throws IOException, APIException {
        return put(buildTarget(host, part), responseClass);
    }


    ////////


	public static URL buildTarget(String host, String resource) throws MalformedURLException {
		return new URL(host + prepareResource(resource));
	}

    public static URL buildTarget(Node node, String resource) throws MalformedURLException {
        return new URL(node.getTransportAddress() + prepareResource(resource));
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

    private static String prepareResource(String resource) {
        if (resource == null) {
            return null;
        }

        if (resource.startsWith("/")) {
            resource = resource.substring(1, resource.length());
        }

        return resource;
    }

}
