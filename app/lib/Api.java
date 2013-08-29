package lib;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Realm;
import com.ning.http.client.Response;
import models.Node;
import models.User;
import models.api.requests.ApiRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class Api {
	private static final Logger log = LoggerFactory.getLogger(Api.class);

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

	public static <T> T get(URL url, Class<T> responseClass, String username, String password) throws APIException, IOException {
		try {
			AsyncHttpClient.BoundRequestBuilder requestBuilder = client.prepareGet(url.toString());

            // TODO: better make this better bro
            if (!responseClass.equals(String.class)) {
			    requestBuilder.addHeader("Accept", "application/json");
            }
            // explicit username and password have priority if they are set, to be able to perform the login
            // otherwise we take them from the node url
            if (username == null && url.getUserInfo() != null) {
                final String[] userPass = url.getUserInfo().split(":", 2);
                username = userPass[0];
                password = userPass[1];
            }
            if (username != null && password != null) {
                requestBuilder.setRealm(new Realm.RealmBuilder()
                        .setPrincipal(username)
                        .setPassword(password)
                        .setUsePreemptiveAuth(true)
                        .setScheme(Realm.AuthScheme.BASIC)
                        .build());
            }
            if (log.isDebugEnabled()) {
                log.debug("API Request: {}", requestBuilder.build().toString());
            }
			final Response response = requestBuilder.execute().get();

			if (response.getStatusCode() != 200) {
				throw new APIException(response.getStatusCode(), "REST call GET [" + url + "] returned " + response.getStatusText());
			}

            // TODO: better make this better bro
            if (responseClass.equals(String.class)) {
                return (T) response.getResponseBody("UTF-8");
            }

			Gson gson = new Gson();
			return gson.fromJson(response.getResponseBody("UTF-8"), responseClass);
		} catch (InterruptedException e) {
			// TODO
		} catch (ExecutionException e) {
			throw new APIException(-1, "REST call GET [" + url + "] failed: " + e.getMessage());
		} catch (MalformedURLException e) {
			throw new RuntimeException("Malformed URL.", e);
		}

		return (T) null;
	}

    public static <T> T put(URL url, Class<T> responseClass) throws APIException, IOException {
        try {
            AsyncHttpClient.BoundRequestBuilder requestBuilder = client.preparePut(url.toString());
            requestBuilder.addHeader("Accept", "application/json");

            if (url.getUserInfo() != null) {
                final String[] userPass = url.getUserInfo().split(":", 2);
                if (userPass[0] != null && userPass[1] != null) {
                    requestBuilder.setRealm(new Realm.RealmBuilder()
                            .setPrincipal(userPass[0])
                            .setPassword(userPass[1])
                            .setUsePreemptiveAuth(true)
                            .setScheme(Realm.AuthScheme.BASIC)
                            .build());
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("API Request: {}", requestBuilder.build().toString());
            }

            final Response response = requestBuilder.execute().get();

            if (response.getStatusCode() != 200) {
                throw new APIException(response.getStatusCode(), "REST call PUT [" + url + "] returned " + response.getStatusText());
            }

            Gson gson = new Gson();
            return gson.fromJson(response.getResponseBody("UTF-8"), responseClass);
        } catch (InterruptedException e) {
            // TODO
        } catch (ExecutionException e) {
            throw new APIException(-1, "REST call PUT [" + url + "] failed: " + e.getMessage());
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

            if (url.getUserInfo() != null) {
                final String[] userPass = url.getUserInfo().split(":", 2);
                if (userPass[0] != null && userPass[1] != null) {
                    requestBuilder.setRealm(new Realm.RealmBuilder()
                            .setPrincipal(userPass[0])
                            .setPassword(userPass[1])
                            .setUsePreemptiveAuth(true)
                            .setScheme(Realm.AuthScheme.BASIC)
                            .build());
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("API Request: {}", requestBuilder.build().toString());
            }

            final Response response = requestBuilder.execute().get();

            if (response.getStatusCode() != expectedResponseCode) {
                throw new APIException(response.getStatusCode(), "REST call POST [" + url + "] returned " + response.getStatusText());
            }

            Gson gson = new Gson();
            return gson.fromJson(response.getResponseBody("UTF-8"), responseClass);
        } catch (InterruptedException e) {
            // TODO
        } catch (ExecutionException e) {
            throw new APIException(-1, "REST call POST [" + url + "] failed: " + e.getMessage());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Malformed URL.", e);
        }

        return (T) null;
    }

    public static <T> T delete(URL url, int expectedResponseCode, Class<T> responseClass) throws APIException, IOException {
        try {
            AsyncHttpClient.BoundRequestBuilder requestBuilder = client.prepareDelete(url.toString());
            requestBuilder.addHeader("Accept", "application/json");

            if (url.getUserInfo() != null) {
                final String[] userPass = url.getUserInfo().split(":", 2);
                if (userPass[0] != null && userPass[1] != null) {
                    requestBuilder.setRealm(new Realm.RealmBuilder()
                            .setPrincipal(userPass[0])
                            .setPassword(userPass[1])
                            .setUsePreemptiveAuth(true)
                            .setScheme(Realm.AuthScheme.BASIC)
                            .build());
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("API Request: {}", requestBuilder.build().toString());
            }

            final Response response = requestBuilder.execute().get();

            if (response.getStatusCode() != expectedResponseCode) {
                throw new APIException(response.getStatusCode(), "REST call DELETE [" + url + "] returned " + response.getStatusText());
            }

            Gson gson = new Gson();
            return gson.fromJson(response.getResponseBody("UTF-8"), responseClass);
        } catch (InterruptedException e) {
            // TODO
        } catch (ExecutionException e) {
            throw new APIException(-1, "REST call DELETE [" + url + "] failed: " + e.getMessage());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Malformed URL.", e);
        }

        return (T) null;
    }



    public static <T> List<T> getFromAllNodes(String resource, Class<T> responseClass) throws APIException, IOException {
        List<T> result = Lists.newArrayList();

        for (Node node : Node.all()) {
            URL url = buildTarget(node, resource);
            try {
                AsyncHttpClient.BoundRequestBuilder requestBuilder = client.prepareGet(url.toString());
                requestBuilder.addHeader("Accept", "application/json");

                if (url.getUserInfo() != null) {
                    final String[] userPass = url.getUserInfo().split(":", 2);
                    if (userPass[0] != null && userPass[1] != null) {
                        requestBuilder.setRealm(new Realm.RealmBuilder()
                                .setPrincipal(userPass[0])
                                .setPassword(userPass[1])
                                .setUsePreemptiveAuth(true)
                                .setScheme(Realm.AuthScheme.BASIC)
                                .build());
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug("API Request: {}", requestBuilder.build().toString());
                }

                final Response response = requestBuilder.execute().get();

                if (response.getStatusCode() != 200) {
                    throw new APIException(response.getStatusCode(), "REST call [" + url + "] returned " + response.getStatusText());
                }

                Gson gson = new Gson();
                result.add(gson.fromJson(response.getResponseBody("UTF-8"), responseClass));
            } catch (InterruptedException e) {
                // TODO
            } catch (ExecutionException e) {
                throw new APIException(-1, "REST call [" + url + "] failed." + e);
            } catch (MalformedURLException e) {
                throw new RuntimeException("Malformed URL.", e);
            }
        }

        return result;
    }




    ////////

    public static <T> T get(String part, Class<T> responseClass) throws IOException, APIException {
        return get(buildTarget(Node.random(), part), responseClass, null, null);
    }

	public static <T> T get(Node node, String part, Class<T> responseClass) throws IOException, APIException {
        return get(buildTarget(node, part), responseClass, null, null);
    }

    public static <T> T post(String part, ApiRequest body, Class<T> responseClass) throws IOException, APIException {
		return post(buildTarget(Node.random(), part), body.toJson(), 200, responseClass);
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

    public static <T> T delete(Node node, String part, Class<T> responseClass) throws IOException, APIException {
        return delete(buildTarget(node, part), 204, responseClass);
    }

    public static <T> T delete(Node node, String part, int expectedResponseCode, Class<T> responseClass) throws IOException, APIException {
        return delete(buildTarget(node, part), expectedResponseCode, responseClass);
    }

    ////////

    public static URL buildTarget(Node node, String resource) throws MalformedURLException {
        final User user = User.current();
        return buildTarget(node, resource, user.getName(), user.getPasswordHash());
    }

    public static URL buildTarget(Node node, String resource, String username, String password) throws MalformedURLException {
        final URI targetAddress;
        try {
            final URI transportAddress = new URI(node.getTransportAddress());
            final String userInfo = username + ":" + password;
            String path = resource;
            if (! resource.startsWith("/")) {
                path = "/" + resource;
            }
            // TODO hack until we separate out the query parameters
            String query = null;
            if (path.contains("?")) {
                final int pos = path.indexOf("?");
                query = path.substring(pos + 1);
                path = path.substring(0, pos);
            }
            targetAddress = new URI(transportAddress.getScheme(), userInfo, transportAddress.getHost(), transportAddress.getPort(), path, query, null);

        } catch (URISyntaxException e) {
            log.error("Could not create target URI", e);
            return null;
        }
        return new URL(targetAddress.toASCIIString());
    }

    @Deprecated
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

    public static <T> T get(String part, Class<T> responseClass, String username, String password) throws IOException, APIException {
		return get(buildTarget(Node.random(), part), responseClass, username, password);
	}
}
