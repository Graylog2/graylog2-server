/*
 * Copyright 2013 TORCH UG
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package lib;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ning.http.client.*;
import models.Node;
import models.api.requests.ApiRequest;
import models.api.responses.EmptyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.F;
import play.mvc.Http;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Singleton
public class ApiClient {
    private static final Logger log = LoggerFactory.getLogger(ApiClient.class);

    public static final String ERROR_MSG_IO = "Could not connect to graylog2-server. Please make sure that it is running and you configured the correct REST URI.";

    private AsyncHttpClient client;
    private final ServerNodes serverNodes;
    private Thread shutdownHook;

    @Inject
    private ApiClient(ServerNodes serverNodes) {
        this.serverNodes = serverNodes;
    }

    public void start() {
        AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();
        builder.setAllowPoolingConnection(false);
        client = new AsyncHttpClient(builder.build());

        shutdownHook = new Thread(new Runnable() {
            @Override
            public void run() {
                client.close();
            }
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    public void stop() {
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
        client.close();
    }

    // default visibility for access from tests (overrides the effects of initialize())
    public void setHttpClient(AsyncHttpClient client) {
        this.client = client;
    }

    public <T> ApiRequestBuilder<T> get(Class<T> responseClass) {
        return new ApiRequestBuilder<>(Method.GET, responseClass);
    }
    public <T> ApiRequestBuilder<T> post(Class<T> responseClass) {
        return new ApiRequestBuilder<>(Method.POST, responseClass);
    }

    public ApiRequestBuilder<EmptyResponse> post() {
        return post(EmptyResponse.class);
    }

    public <T> ApiRequestBuilder<T> put(Class<T> responseClass) {
        return new ApiRequestBuilder<>(Method.PUT, responseClass);
    }

    public ApiRequestBuilder<EmptyResponse> put() {
        return put(EmptyResponse.class);
    }

    public <T> ApiRequestBuilder<T> delete(Class<T> responseClass) {
        return new ApiRequestBuilder<>(Method.DELETE, responseClass);
    }

    public ApiRequestBuilder<EmptyResponse> delete() {
        return delete(EmptyResponse.class);
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


    public enum Method {
        GET,
        POST,
        PUT,
        DELETE
    }

    public class ApiRequestBuilder<T> {
        private String pathTemplate;
        private Node node;
        private List<Node> nodes;
        private String username;
        private String password;
        private final Method method;
        private ApiRequest body;
        private final Class<T> responseClass;
        private final ArrayList<Object> pathParams = Lists.newArrayList();
        private final ArrayList<F.Tuple<String,String>> queryParams = Lists.newArrayList();
        private int httpStatusCode = Http.Status.OK;
        private TimeUnit timeoutUnit = TimeUnit.SECONDS;
        private int timeoutValue = 5;

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
        public ApiRequestBuilder<T> nodes(Node... nodes) {
            if (this.nodes != null) {
                // TODO makes this sane
                throw new IllegalStateException();
            }
            this.nodes = Lists.newArrayList(nodes);
            return this;
        }

        public ApiRequestBuilder<T> fromAllNodes() {
            this.nodes = serverNodes.all();
            return this;
        }

        public ApiRequestBuilder<T> queryParam(String name, String value) {
            queryParams.add(F.Tuple(name, value));
            return this;
        }

        public ApiRequestBuilder<T> queryParam(String name, int value) {
            return queryParam(name, Integer.toString(value));
        }

        public ApiRequestBuilder<T> queryParams(Map<String, String> params) {
            for(Map.Entry<String, String> p : params.entrySet()) {
                queryParam(p.getKey(), p.getValue());
            }

            return this;
        }

        public ApiRequestBuilder<T> credentials(String username, String password) {
            this.username = username;
            this.password = password;
            return this;
        }

        public ApiRequestBuilder<T> body(ApiRequest body) {
            this.body = body;
            return this;
        }

        public ApiRequestBuilder<T> expect(int httpStatusCode) {
            this.httpStatusCode = httpStatusCode;
            return this;
        }

        public ApiRequestBuilder<T> timeout(int value, TimeUnit unit) {
            this.timeoutValue = value;
            this.timeoutUnit = unit;
            return this;
        }

        public T execute() throws APIException, IOException {
            if (node == null) {
                if (nodes != null) {
                    log.error("Multiple nodes are set, but execute() was called. This is most likely a bug and you meant to call executeOnAll()!");
                }
                node(serverNodes.any());
            }
            final URL url = prepareUrl(node);
            final AsyncHttpClient.BoundRequestBuilder requestBuilder = requestBuilderForUrl(url);

            final Request request = requestBuilder.build();
            if (log.isDebugEnabled()) {
                log.debug("API Request: {}", request.toString());
            }
            try {
                Response response = requestBuilder.execute().get(timeoutValue, timeoutUnit);

                // TODO this is wrong, shouldn't it accept some callback instead of throwing an exception?
                if (response.getStatusCode() != httpStatusCode) {
                    throw new APIException(request, response);
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
                throw new APIException(request, e);
            } catch (IOException e) {
                // TODO
                log.error("unhandled IOException", e);
                throw e;
            } catch (TimeoutException e) {
                log.warn("Timed out requesting {}", request);
            }
            // TODO should this throw an exception instead?
            return null;
        }

        public Collection<T> executeOnAll() {
            HashSet<T> results = Sets.newHashSet();
            if (node == null && nodes == null) {
                nodes = serverNodes.all();
            }

            Collection<ListenableFuture<Response>> requests = Lists.newArrayList();
            final Collection<Response> responses = Lists.newArrayList();
            for (Node currentNode : nodes) {
                final URL url = prepareUrl(currentNode);
                try {
                    final AsyncHttpClient.BoundRequestBuilder requestBuilder = requestBuilderForUrl(url);
                    if (log.isDebugEnabled()) {
                        log.debug("API Request: {}", requestBuilder.build().toString());
                    }
                    requests.add(requestBuilder.execute(new AsyncCompletionHandler<Response>() {
                        @Override
                        public Response onCompleted(Response response) throws Exception {
                            responses.add(response);
                            return response;
                        }
                    }));
                } catch (IOException e) {
                    log.error("Cannot execute request", e);
                }
            }
            for (ListenableFuture<Response> request : requests) {
                try {
                    final Response response = request.get(timeoutValue, timeoutUnit);
                    results.add(deserializeJson(response, responseClass));
                } catch (InterruptedException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (ExecutionException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (TimeoutException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }


            return results;
        }

        private AsyncHttpClient.BoundRequestBuilder requestBuilderForUrl(URL url) {
            // *sigh* the generic requestBuilder methods are protected/private making this verbose :(
            final AsyncHttpClient.BoundRequestBuilder requestBuilder;
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
                default:
                    throw new IllegalStateException("Illegal method " + method.toString());
            }

            applyBasicAuthentication(requestBuilder, url.getUserInfo());
            requestBuilder.setPerRequestConfig(new PerRequestConfig(null, (int)timeoutUnit.toMillis(timeoutValue)));

            if (body != null) {
                if (method != Method.PUT && method != Method.POST) {
                    throw new IllegalArgumentException("Cannot set request body on non-PUT or POST requests.");
                }
                requestBuilder.addHeader("Content-Type", "application/json");
                requestBuilder.setBody(body.toJson());
            } else if(method == Method.POST) {
                log.warn("POST without body, this doesn't make sense,", new IllegalStateException());
            }
            // TODO: should we always insist on things being wrapped in json?
            if (!responseClass.equals(String.class)) {
                requestBuilder.addHeader("Accept", "application/json");
            }
            return requestBuilder;
        }

        URL prepareUrl(Node node) {
            // if this is null there's not much we can do anyway...
            Preconditions.checkNotNull(pathTemplate, "path() needs to be set to a non-null value.");

            URI builtUrl;
            try {
                String path = MessageFormat.format(pathTemplate, pathParams.toArray());
                final UriBuilder uriBuilder = UriBuilder.fromUri(node.getTransportAddress());
                uriBuilder.path(path);
                for (F.Tuple<String, String> queryParam : queryParams) {
                    uriBuilder.queryParam(queryParam._1, queryParam._2);
                }

                if (username != null && password != null) {
                    uriBuilder.userInfo(username + ":" + password);
                }
                builtUrl = uriBuilder.build();
                return builtUrl.toURL();
            } catch (MalformedURLException e) {
                // TODO handle this properly
                log.error("Could not build target URL", e);
                throw new RuntimeException(e);
            }
        }
    }
}
