package lib;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Realm;
import com.ning.http.client.Response;
import models.Node;
import models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class Api {
	private static final Logger log = LoggerFactory.getLogger(Api.class);

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

            // TODO: should we always insist on things being wrapped in json?
            if (!responseClass.equals(String.class)) {
			    requestBuilder.addHeader("Accept", "application/json");
            }
            // explicit username and password have priority if they are set, to be able to perform the login
            // otherwise we take them from the node url
            String userInfo = url.getUserInfo();
            if (username != null && password != null) {
                userInfo = username + ":" + password;
            }
            applyBasicAuthentication(requestBuilder, userInfo);

            if (log.isDebugEnabled()) {
                log.debug("API Request: {}", requestBuilder.build().toString());
            }
            final Response response = requestBuilder.execute().get();

            if (response.getStatusCode() != 200) {
                throw new APIException(response.getStatusCode(), "REST call GET [" + url + "] returned " + response.getStatusText());
            }

            // TODO: should we always insist on things being wrapped in json?
            if (responseClass.equals(String.class)) {
                return responseClass.cast(response.getResponseBody("UTF-8"));
            }

            return deserializeJson(response, responseClass);
		} catch (InterruptedException e) {
			// TODO
		} catch (ExecutionException e) {
			throw new APIException(-1, "REST call GET [" + url + "] failed: " + e.getMessage());
		} catch (MalformedURLException e) {
			throw new RuntimeException("Malformed URL.", e);
		}

		return null;
	}

    public static <T> T put(URL url, Class<T> responseClass) throws APIException, IOException {
        try {
            AsyncHttpClient.BoundRequestBuilder requestBuilder = client.preparePut(url.toString());
            requestBuilder.addHeader("Accept", "application/json");

            applyBasicAuthentication(requestBuilder, url.getUserInfo());
            if (log.isDebugEnabled()) {
                log.debug("API Request: {}", requestBuilder.build().toString());
            }

            final Response response = requestBuilder.execute().get();

            if (response.getStatusCode() != 200) {
                throw new APIException(response.getStatusCode(), "REST call PUT [" + url + "] returned " + response.getStatusText());
            }

            return deserializeJson(response, responseClass);
        } catch (InterruptedException e) {
            // TODO
        } catch (ExecutionException e) {
            throw new APIException(-1, "REST call PUT [" + url + "] failed: " + e.getMessage());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Malformed URL.", e);
        }

        return null;
    }

    public static <T> T post(URL url, String body, int expectedResponseCode, Class<T> responseClass) throws APIException, IOException {
        try {
            AsyncHttpClient.BoundRequestBuilder requestBuilder = client.preparePost(url.toString());
            requestBuilder.addHeader("Accept", "application/json");
            requestBuilder.setBody(body);

            applyBasicAuthentication(requestBuilder, url.getUserInfo());
            if (log.isDebugEnabled()) {
                log.debug("API Request: {}", requestBuilder.build().toString());
            }

            final Response response = requestBuilder.execute().get();

            if (response.getStatusCode() != expectedResponseCode) {
                throw new APIException(response.getStatusCode(), "REST call POST [" + url + "] returned " + response.getStatusText());
            }

            return deserializeJson(response, responseClass);
        } catch (InterruptedException e) {
            // TODO
        } catch (ExecutionException e) {
            throw new APIException(-1, "REST call POST [" + url + "] failed: " + e.getMessage());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Malformed URL.", e);
        }

        return null;
    }

    public static <T> T delete(URL url, int expectedResponseCode, Class<T> responseClass) throws APIException, IOException {
        try {
            AsyncHttpClient.BoundRequestBuilder requestBuilder = client.prepareDelete(url.toString());
            requestBuilder.addHeader("Accept", "application/json");

            applyBasicAuthentication(requestBuilder, url.getUserInfo());
            if (log.isDebugEnabled()) {
                log.debug("API Request: {}", requestBuilder.build().toString());
            }

            final Response response = requestBuilder.execute().get();

            if (response.getStatusCode() != expectedResponseCode) {
                throw new APIException(response.getStatusCode(), "REST call DELETE [" + url + "] returned " + response.getStatusText());
            }

            return deserializeJson(response, responseClass);
        } catch (InterruptedException e) {
            // TODO
        } catch (ExecutionException e) {
            throw new APIException(-1, "REST call DELETE [" + url + "] failed: " + e.getMessage());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Malformed URL.", e);
        }

        return null;
    }

    private static void applyBasicAuthentication(AsyncHttpClient.BoundRequestBuilder requestBuilder, String userInfo) {
        if (userInfo != null) {
            final String[] userPass = userInfo.split(":", 2);
            if (userPass[0] != null && userPass[1] != null) {
                requestBuilder.setRealm(new Realm.RealmBuilder()
                        .setPrincipal(userPass[0])
                        .setPassword(userPass[1])
                        .setUsePreemptiveAuth(true)
                        .setScheme(Realm.AuthScheme.BASIC)
                        .build());
            }
        }
    }


    private static <T> T deserializeJson(Response response, Class<T> responseClass) throws IOException {
        return new Gson().fromJson(response.getResponseBody("UTF-8"), responseClass);
    }

    public static <T> List<T> getFromAllNodes(String resource, Class<T> responseClass) throws APIException, IOException {
        List<T> result = Lists.newArrayList();

        for (Node node : Node.all()) {
            URL url = buildTarget(node, resource);
            try {
                AsyncHttpClient.BoundRequestBuilder requestBuilder = client.prepareGet(url.toString());
                requestBuilder.addHeader("Accept", "application/json");

                applyBasicAuthentication(requestBuilder, url.getUserInfo());
                if (log.isDebugEnabled()) {
                    log.debug("API Request: {}", requestBuilder.build().toString());
                }

                final Response response = requestBuilder.execute().get();

                if (response.getStatusCode() != 200) {
                    throw new APIException(response.getStatusCode(), "REST call [" + url + "] returned " + response.getStatusText());
                }

                result.add(deserializeJson(response, responseClass));
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

    public static URL buildTarget(Node node, String resource) throws MalformedURLException {
        return buildTarget(node, resource, null);
    }

    public static URL buildTarget(Node node, String resource, String query) throws MalformedURLException {
        final User user = User.current();
        String name = null;
        String passwordHash = null;
        if (user != null) {
            name = user.getName();
            passwordHash = user.getPasswordHash();
        }
        return buildTarget(node, resource, query, name, passwordHash);
    }

    public static URL buildTarget(Node node, String resource, String username, String password) throws MalformedURLException {
        return buildTarget(node, resource, null, username, password);
    }

    public static URL buildTarget(Node node, String resource, String queryParams, String username, String password) throws MalformedURLException {
        final URI targetAddress;
        try {
            final URI transportAddress = new URI(node.getTransportAddress());
            final String userInfo;
            if (username == null || password == null) {
                userInfo = null;
            }
            else {
                userInfo = username + ":" + password;
            }

            String path = resource;
            if (! resource.startsWith("/")) {
                path = "/" + resource;
            }
            String query = queryParams;
            if (queryParams == null && path.contains("?")) {
                // TODO hack until we separate out the query parameters
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

}
