package lib;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

public class ApiClient {
    private static final Logger log = LoggerFactory.getLogger(ApiClient.class);

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

    public static <T> ApiRequestBuilder<T> get(Class<T> responseClass) {
        return new ApiRequestBuilder<>(ApiRequestBuilder.Method.GET, responseClass);
    }
    public static <T> ApiRequestBuilder<T> post(Class<T> responseClass) {
        return new ApiRequestBuilder<>(ApiRequestBuilder.Method.POST, responseClass);
    }
    public static <T> ApiRequestBuilder<T> put(Class<T> responseClass) {
        return new ApiRequestBuilder<>(ApiRequestBuilder.Method.PUT, responseClass);
    }
    public static <T> ApiRequestBuilder<T> delete(Class<T> responseClass) {
        return new ApiRequestBuilder<>(ApiRequestBuilder.Method.DELETE, responseClass);
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

    private static URL buildTarget(Node node, String resource, String query) throws MalformedURLException {
        final User user = User.current();
        String name = null;
        String passwordHash = null;
        if (user != null) {
            name = user.getName();
            passwordHash = user.getPasswordHash();
        }
        return buildTarget(node, resource, query, name, passwordHash);
    }

    private static URL buildTarget(Node node, String resource, String queryParams, String username, String password) throws MalformedURLException {
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
                log.warn("Use queryParam() to add query parameters, do not append them to the path, because that screws up escaping.");
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


    public static class ApiRequestBuilder<T> {
        public static enum Method {
            GET,
            POST,
            PUT,
            DELETE
        }
        private String pathTemplate;
        private Node node;
        private String username;
        private String password;
        private final Method method;
        private final Class<T> responseClass;
        private final ArrayList<Object> pathParams = Lists.newArrayList();
        private final ArrayList<String> queryParams = Lists.newArrayList();

        private AsyncHttpClient.BoundRequestBuilder requestBuilder;

        public ApiRequestBuilder(Method method, Class<T> responseClass) {
            this.method = method;
            this.responseClass = responseClass;
        }

        public ApiRequestBuilder<T> path(String pathTemplate) {
            this.pathTemplate = pathTemplate;
            return this;
        }

        // convenience
        public ApiRequestBuilder<T> path(String pathTemplate, Object... params) {
            path(pathTemplate);
            pathParams(params);
            return this;
        }

        public ApiRequestBuilder<T> pathParams(Object... params) {
            Collections.addAll(pathParams, params);
            return this;
        }

        public ApiRequestBuilder<T> pathParam(Object param) {
            return pathParams(param);
        }

        public ApiRequestBuilder<T> node(Node node) {
            this.node = node;
            return this;
        }

        public ApiRequestBuilder<T> queryParam(String name, String value) {
            queryParams.add(name + "=" + value);
            return this;
        }

        public ApiRequestBuilder<T> queryParam(String name, int value) {
            return queryParam(name, Integer.toString(value));
        }

        public ApiRequestBuilder<T> credentials(String username, String password) {
            this.username = username;
            this.password = password;
            return this;
        }

        public T execute() throws APIException, IOException {
            if (node == null) {
                try {
                    node(Node.random());
                } catch (IOException e) {
                    // TODO
                    log.error("Could not get random node", e);
                } catch (APIException e) {
                    // TODO
                }
            }
            final URL url = prepareUrl();

            // *sigh* the generic requestBuilder methods are protected/private making this verbose :(
            switch (method) {
                case GET:
                    requestBuilder = client.prepareGet(url.toString());
                    break;
                case POST:
                    requestBuilder = client.preparePost(url.toString());
                    break;
                case PUT:
                    requestBuilder = client.preparePut(url.toString());
                    break;
                case DELETE:
                    requestBuilder = client.prepareDelete(url.toString());
                    break;
            }

            applyBasicAuthentication(requestBuilder, url.getUserInfo());

            // TODO: should we always insist on things being wrapped in json?
            if (!responseClass.equals(String.class)) {
                requestBuilder.addHeader("Accept", "application/json");
            }

            if (log.isDebugEnabled()) {
                log.debug("API Request: {}", requestBuilder.build().toString());
            }
            try {
                Response response = requestBuilder.execute().get();

                // TODO this is wrong
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
            } catch (MalformedURLException e) {
                log.error("Malformed URL", e);
                throw new RuntimeException("Malformed URL.", e);
            } catch (ExecutionException e) {
                log.error("REST call failed", e);
                throw new APIException(-1, "REST call GET [" + url + "] failed: " + e.getMessage());
            } catch (IOException e) {
                // TODO
                log.error("unhandled IOException", e);
                throw e;
            }
            // TODO should this throw an exception instead?
            return null;
        }

        private URL prepareUrl() {
            // if this is null there's not much we can do anyway...
            Preconditions.checkNotNull(pathTemplate, "pathTemplate() needs to be set to a non-null value.");

            final URL builtUrl;
            try {
                String path = MessageFormat.format(pathTemplate, pathParams.toArray());
                String query = null;
                if (!queryParams.isEmpty()) {
                    query = Joiner.on('&').join(queryParams);
                }
                if (username != null && password != null) {
                    builtUrl = buildTarget(node, path, query, username, password);
                } else {
                    builtUrl = buildTarget(node, path, query);
                }
            } catch (MalformedURLException e) {
                // TODO handle this properly
                log.error("Could not build target URL", e);
                throw new RuntimeException(e);
            }
            return builtUrl;
        }
    }
}
