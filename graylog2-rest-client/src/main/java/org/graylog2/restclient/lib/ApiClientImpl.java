/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.restclient.lib;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Realm;
import com.ning.http.client.Request;
import com.ning.http.client.Response;
import com.ning.http.client.listener.TransferCompletionHandler;
import com.ning.http.client.listener.TransferListener;
import com.squareup.okhttp.HttpUrl;
import org.graylog2.restclient.models.ClusterEntity;
import org.graylog2.restclient.models.Node;
import org.graylog2.restclient.models.Radio;
import org.graylog2.restclient.models.User;
import org.graylog2.restclient.models.UserService;
import org.graylog2.restclient.models.api.responses.EmptyResponse;
import org.graylog2.restroutes.PathMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.graylog2.restclient.lib.Tools.rootCause;

@Singleton
class ApiClientImpl implements ApiClient {
    private static final Logger LOG = LoggerFactory.getLogger(ApiClient.class);

    private AsyncHttpClient client;
    private final ServerNodes serverNodes;
    private final Long defaultTimeout;
    private final boolean acceptAnyCertificate;
    private final ObjectMapper objectMapper;
    private Thread shutdownHook;

    @Inject
    private ApiClientImpl(ServerNodes serverNodes,
                          @Named("Default Timeout") Long defaultTimeout,
                          @Named("client.accept-any-certificate") boolean acceptAnyCertificate) {
        this(serverNodes, defaultTimeout, acceptAnyCertificate,
                new ObjectMapper()
                        .setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES)
                        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                        .registerModule(new GuavaModule())
                        .registerModule(new JodaModule()));
    }

    private ApiClientImpl(ServerNodes serverNodes, Long defaultTimeout, boolean acceptAnyCertificate, ObjectMapper objectMapper) {
        this.serverNodes = serverNodes;
        this.defaultTimeout = defaultTimeout;
        this.acceptAnyCertificate = acceptAnyCertificate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void start() {
        AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();
        builder.setAllowPoolingConnections(false);
        builder.setAllowPoolingSslConnections(false);
        builder.setAcceptAnyCertificate(acceptAnyCertificate);
        builder.setUserAgent("graylog2-web/" + Version.VERSION);
        client = new AsyncHttpClient(builder.build());

        shutdownHook = new Thread(new Runnable() {
            @Override
            public void run() {
                client.close();
            }
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    @Override
    public void stop() {
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (IllegalStateException e) {
            // ignore race at shutdown.
        }
        client.close();
    }

    // default visibility for access from tests (overrides the effects of initialize())
    @Override
    public void setHttpClient(AsyncHttpClient client) {
        this.client = client;
    }

    @Override
    public <T> org.graylog2.restclient.lib.ApiRequestBuilder<T> get(Class<T> responseClass) {
        return new ApiRequestBuilder<>(Method.GET, responseClass);
    }

    @Override
    public <T> org.graylog2.restclient.lib.ApiRequestBuilder<T> post(Class<T> responseClass) {
        return new ApiRequestBuilder<>(Method.POST, responseClass);
    }

    @Override
    public org.graylog2.restclient.lib.ApiRequestBuilder<EmptyResponse> post() {
        return post(EmptyResponse.class);
    }

    @Override
    public <T> org.graylog2.restclient.lib.ApiRequestBuilder<T> put(Class<T> responseClass) {
        return new ApiRequestBuilder<>(Method.PUT, responseClass);
    }

    @Override
    public org.graylog2.restclient.lib.ApiRequestBuilder<EmptyResponse> put() {
        return put(EmptyResponse.class);
    }

    @Override
    public <T> org.graylog2.restclient.lib.ApiRequestBuilder<T> delete(Class<T> responseClass) {
        return new ApiRequestBuilder<>(Method.DELETE, responseClass);
    }

    @Override
    public org.graylog2.restclient.lib.ApiRequestBuilder<EmptyResponse> delete() {
        return delete(EmptyResponse.class);
    }

    @Override
    public <T> org.graylog2.restclient.lib.ApiRequestBuilder<T> path(PathMethod pathMethod, Class<T> responseClasse) {
        Method httpMethod;
        switch (pathMethod.getMethod().toUpperCase(Locale.ENGLISH)) {
            case "GET":
                httpMethod = Method.GET;
                break;
            case "PUT":
                httpMethod = Method.PUT;
                break;
            case "POST":
                httpMethod = Method.POST;
                break;
            case "DELETE":
                httpMethod = Method.DELETE;
                break;
            default:
                httpMethod = Method.GET;
        }

        ApiRequestBuilder<T> builder = new ApiRequestBuilder<>(httpMethod, responseClasse);
        return builder.path(pathMethod.getPath());
    }

    @Override
    public org.graylog2.restclient.lib.ApiRequestBuilder<EmptyResponse> path(PathMethod pathMethod) {
        return path(pathMethod, EmptyResponse.class);
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

    private <T> T deserializeJson(Response response, Class<T> responseClass) throws IOException {
        return objectMapper.readValue(response.getResponseBody(StandardCharsets.UTF_8.name()), responseClass);
    }

    public class ApiRequestBuilder<T> implements org.graylog2.restclient.lib.ApiRequestBuilder<T> {
        private String pathTemplate;
        private Node node;
        private Radio radio;
        private Collection<Node> nodes;
        private final Method method;
        private Object body;
        private final Class<T> responseClass;
        private final ArrayList<Object> pathParams = Lists.newArrayList();
        private final ListMultimap<String, String> queryParams = ArrayListMultimap.create();
        private Set<Integer> expectedResponseCodes = Sets.newHashSet();
        private TimeUnit timeoutUnit = TimeUnit.MILLISECONDS;
        private long timeoutValue = defaultTimeout;
        private boolean unauthenticated = false;
        private MediaType mediaType = MediaType.JSON_UTF_8;
        private String sessionId;
        private Boolean extendSession;

        public ApiRequestBuilder(Method method, Class<T> responseClass) {
            this.method = method;
            this.responseClass = responseClass;
        }

        @Override
        public ApiRequestBuilder<T> path(String pathTemplate) {
            this.pathTemplate = pathTemplate;
            return this;
        }

        // convenience
        @Override
        public org.graylog2.restclient.lib.ApiRequestBuilder<T> path(String pathTemplate, Object... params) {
            path(pathTemplate);
            pathParams(params);
            return this;
        }

        @Override
        public org.graylog2.restclient.lib.ApiRequestBuilder<T> pathParams(Object... params) {
            Collections.addAll(pathParams, params);
            return this;
        }

        @Override
        public org.graylog2.restclient.lib.ApiRequestBuilder<T> pathParam(Object param) {
            return pathParams(param);
        }

        @Override
        public ApiRequestBuilder<T> node(Node node) {
            this.node = node;
            return this;
        }

        @Override
        public org.graylog2.restclient.lib.ApiRequestBuilder<T> radio(Radio radio) {
            this.radio = radio;
            return this;
        }

        @Override
        public org.graylog2.restclient.lib.ApiRequestBuilder<T> clusterEntity(ClusterEntity entity) {
            if (entity instanceof Radio) {
                this.radio = (Radio) entity;
            } else if (entity instanceof Node) {
                this.node = (Node) entity;
            } else {
                LOG.warn("You passed a ClusterEntity that is not of type Node or Radio. Selected nothing.");
            }
            return this;
        }

        @Override
        public org.graylog2.restclient.lib.ApiRequestBuilder<T> nodes(Node... nodes) {
            if (this.nodes != null) {
                // TODO makes this sane
                throw new IllegalStateException();
            }
            this.nodes = Lists.newArrayList(nodes);
            return this;
        }

        @Override
        public org.graylog2.restclient.lib.ApiRequestBuilder<T> nodes(Collection<Node> nodes) {
            if (this.nodes != null) {
                // TODO makes this sane
                throw new IllegalStateException();
            }
            this.nodes = Lists.newArrayList(nodes);
            return this;
        }

        @Override
        public org.graylog2.restclient.lib.ApiRequestBuilder<T> fromAllNodes() {
            this.nodes = serverNodes.all();
            return this;
        }

        @Override
        public org.graylog2.restclient.lib.ApiRequestBuilder<T> onlyMasterNode() {
            this.node = serverNodes.master();
            return this;
        }

        @Override
        public ApiRequestBuilder<T> queryParam(String name, String value) {
            queryParams.put(name, value);
            return this;
        }

        @Override
        public org.graylog2.restclient.lib.ApiRequestBuilder<T> queryParam(String name, int value) {
            return queryParam(name, Integer.toString(value));
        }

        @Override
        public org.graylog2.restclient.lib.ApiRequestBuilder<T> queryParams(Map<String, String> params) {
            for (Map.Entry<String, String> p : params.entrySet()) {
                queryParam(p.getKey(), p.getValue());
            }

            return this;
        }

        @Override
        public org.graylog2.restclient.lib.ApiRequestBuilder<T> session(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        @Override
        public org.graylog2.restclient.lib.ApiRequestBuilder<T> extendSession(boolean extend) {
            this.extendSession = extend;
            return this;
        }

        @Override
        public org.graylog2.restclient.lib.ApiRequestBuilder<T> unauthenticated() {
            this.unauthenticated = true;
            return this;
        }

        @Override
        public org.graylog2.restclient.lib.ApiRequestBuilder<T> body(Object body) {
            this.body = body;
            return this;
        }

        @Override
        public org.graylog2.restclient.lib.ApiRequestBuilder<T> expect(int... httpStatusCodes) {
            for (int code : httpStatusCodes) {
                this.expectedResponseCodes.add(code);
            }

            return this;
        }

        @Override
        public org.graylog2.restclient.lib.ApiRequestBuilder<T> timeout(long value) {
            this.timeoutValue = value;
            this.timeoutUnit = TimeUnit.MILLISECONDS;
            return this;
        }

        @Override
        public ApiRequestBuilder<T> timeout(long value, TimeUnit unit) {
            this.timeoutValue = value;
            this.timeoutUnit = unit;
            return this;
        }

        @Override
        public org.graylog2.restclient.lib.ApiRequestBuilder<T> accept(MediaType mediaType) {
            this.mediaType = mediaType;
            return this;
        }

        @Override
        public T execute() throws APIException, IOException {
            if (radio != null && (node != null || nodes != null)) {
                throw new RuntimeException("You set both and a Node and a Radio as target. This is not possible.");
            }

            final ClusterEntity target;

            if (radio == null) {
                if (node == null) {
                    if (nodes != null) {
                        LOG.error("Multiple nodes are set, but execute() was called. This is most likely a bug and you meant to call executeOnAll()!", new Throwable());
                    }
                    node(serverNodes.any());
                }

                target = node;
            } else {
                target = radio;
            }

            ensureAuthentication();
            final URL url = prepareUrl(target);
            final AsyncHttpClient.BoundRequestBuilder requestBuilder = requestBuilderForUrl(url);
            requestBuilder.addHeader(HttpHeaders.ACCEPT, mediaType.toString());

            final Request request = requestBuilder.build();
            if (LOG.isDebugEnabled()) {
                LOG.debug("API Request: {}", request);
            }

            // Set 200 OK as standard if not defined.
            ensureExpectedResponseCodes();

            try {
                Response response = requestBuilder.execute().get(timeoutValue, timeoutUnit);

                target.touch();
                return handleResponse(request, response);
            } catch (InterruptedException e) {
                target.markFailure();
            } catch (MalformedURLException e) {
                LOG.error("Malformed URL", e);
                throw new RuntimeException("Malformed URL.", e);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof ConnectException) {
                    LOG.warn("Graylog server unavailable. Connection refused.");
                    target.markFailure();
                    throw new Graylog2ServerUnavailableException(e);
                }
                LOG.error("REST call failed", rootCause(e));
                throw new APIException(request, e);
            } catch (IOException e) {
                LOG.error("unhandled IOException", rootCause(e));
                target.markFailure();
                throw e;
            } catch (TimeoutException e) {
                LOG.warn("Timed out requesting {}", request);
                target.markFailure();
            }
            throw new APIException(request, new IllegalStateException("Unhandled error condition in API client"));
        }

        private T handleResponse(Request request, Response response) throws IOException, APIException {
            // TODO: once we switch to jackson we can take the media type into account automatically
            final MediaType responseContentType;
            if (response.getContentType() == null) {
                responseContentType = MediaType.JSON_UTF_8;
            } else {
                responseContentType = MediaType.parse(response.getContentType());
            }

            if (!responseContentType.is(mediaType.withoutParameters())) {
                LOG.error("Accept header was {} but response is {}. Failing.", mediaType, responseContentType);
                throw new APIException(request, response);
            }
            if (responseClass.equals(String.class)) {
                return responseClass.cast(response.getResponseBody("UTF-8"));
            }

            final boolean responseCodeIsExpected = expectedResponseCodes.contains(response.getStatusCode());
            final boolean responseCodeIsSuccessful = response.getStatusCode() >= 200 && response.getStatusCode() < 300;
            if (responseCodeIsExpected || responseCodeIsSuccessful) {
                T result;
                try {
                    if (response.getResponseBody().isEmpty()) {
                        return null;
                    }

                    if (responseContentType.is(MediaType.JSON_UTF_8.withoutParameters())) {
                        result = deserializeJson(response, responseClass);
                    } else {
                        LOG.error("Cannot deserialize content type {} expected {}, failing.", responseContentType, mediaType);
                        throw new APIException(request, response);
                    }

                    if (result == null) {
                        throw new APIException(request, response);
                    }

                    return result;
                } catch (Exception e) {
                    LOG.error("Caught Exception while deserializing JSON request: ", e);
                    LOG.debug("Response from backend was: " + response.getResponseBody("UTF-8"));

                    throw new APIException(request, response, e);
                }
            } else {
                throw new APIException(request, response);
            }
        }

        private void ensureExpectedResponseCodes() {
            if (expectedResponseCodes.isEmpty()) {
                expectedResponseCodes.add(200);
            }
        }

        private void ensureAuthentication() {
            if (!unauthenticated && sessionId == null) {
                final User user = UserService.current();
                if (user != null) {
                    session(user.getSessionId());
                } else {
                    LOG.warn("You did not add unauthenticated() nor session() but also don't have a current user. You probably meant unauthenticated(). This is a bug!", new Throwable());
                }
            }
        }

        private class RequestContext {
            private final Node node;
            private final Request request;
            private final ListenableFuture<Response> listenableFuture;

            public RequestContext(Node node, Request request, ListenableFuture<Response> listenableFuture) {
                this.node = node;
                this.request = request;
                this.listenableFuture = listenableFuture;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                RequestContext that = (RequestContext) o;

                if (!node.equals(that.node)) return false;
                if (!request.equals(that.request)) return false;
                return listenableFuture.equals(that.listenableFuture);

            }

            @Override
            public int hashCode() {
                int result = node.hashCode();
                result = 31 * result + request.hashCode();
                result = 31 * result + listenableFuture.hashCode();
                return result;
            }
        }

        @Override
        public Map<Node, T> executeOnAll() throws APIException {
            HashMap<Node, T> results = Maps.newHashMap();
            if (node == null && nodes == null) {
                nodes = serverNodes.all();
            }

            final Set<RequestContext> requestContexts = Sets.newHashSetWithExpectedSize(nodes.size());
            final Collection<Response> responses = Lists.newArrayList();

            ensureAuthentication();
            for (Node currentNode : nodes) {
                final URL url = prepareUrl(currentNode);
                try {
                    final AsyncHttpClient.BoundRequestBuilder requestBuilder = requestBuilderForUrl(url);
                    requestBuilder.addHeader(HttpHeaders.ACCEPT, mediaType.toString());

                    // we need to build the request in case we have to throw an APIException in handleResponse()
                    final Request request = requestBuilder.build();
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("API Request: {}", request);
                    }
                    final ListenableFuture<Response> future = requestBuilder.execute(new AsyncCompletionHandler<Response>() {
                        @Override
                        public Response onCompleted(Response response) throws Exception {
                            responses.add(response);
                            return response;
                        }
                    });
                    requestContexts.add(new RequestContext(currentNode, request, future));
                } catch (IOException e) {
                    LOG.error("Cannot execute request", e);
                    currentNode.markFailure();
                }
            }

            // Set 200 OK as standard if not defined.
            ensureExpectedResponseCodes();

            for (RequestContext context : requestContexts) {
                final Node node = context.node;
                final ListenableFuture<Response> future = context.listenableFuture;
                try {
                    final Response response = future.get(timeoutValue, timeoutUnit);
                    if (response == null) {
                        LOG.error("Didn't receive response from node {}", node);
                        node.markFailure();
                        continue;
                    }
                    node.touch();
                    final T result = handleResponse(context.request, response);
                    results.put(node, result);
                } catch (InterruptedException e) {
                    LOG.error("API call Interrupted", e);
                    node.markFailure();
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof ConnectException) {
                        LOG.error("{}", e.getCause().getMessage());
                    } else {
                        LOG.error("API call failed to execute.", e);
                    }
                    node.markFailure();
                } catch (IOException e) {
                    LOG.error("API failed due to IO error", e);
                    node.markFailure();
                } catch (TimeoutException e) {
                    LOG.error("API call timed out", e);
                    node.markFailure();
                }
            }

            return results;
        }

        private AsyncHttpClient.BoundRequestBuilder requestBuilderForUrl(URL url) throws JsonProcessingException {
            // *sigh* the generic requestBuilder methods are protected/private making this verbose :(
            final AsyncHttpClient.BoundRequestBuilder requestBuilder;
            final String userInfo = url.getUserInfo();
            // have to hack around here, because the userInfo will unescape the @ in usernames :(
            final HttpUrl httpUrl = HttpUrl.get(url)
                    .newBuilder()
                    .username("")
                    .password("")
                    .build();

            switch (method) {
                case GET:
                    requestBuilder = client.prepareGet(httpUrl.toString());
                    break;
                case POST:
                    requestBuilder = client.preparePost(httpUrl.toString());
                    break;
                case PUT:
                    requestBuilder = client.preparePut(httpUrl.toString());
                    break;
                case DELETE:
                    requestBuilder = client.prepareDelete(httpUrl.toString());
                    break;
                default:
                    throw new IllegalStateException("Illegal method " + method.toString());
            }

            applyBasicAuthentication(requestBuilder, userInfo);
            requestBuilder.setRequestTimeout((int) timeoutUnit.toMillis(timeoutValue));

            if (body != null) {
                if (method != Method.PUT && method != Method.POST) {
                    throw new IllegalArgumentException("Cannot set request body on non-PUT or POST requests.");
                }
                requestBuilder.addHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8");
                requestBuilder.setBodyEncoding("UTF-8");
                requestBuilder.setBody(objectMapper.writeValueAsString(body));
            } else if (method == Method.POST) {
                LOG.warn("POST without body, this doesn't make sense,", new IllegalStateException());
            }
            // TODO: should we always insist on things being wrapped in json?
            if (!responseClass.equals(String.class)) {
                requestBuilder.addHeader("Accept", "application/json");
            }
            requestBuilder.addHeader("Accept-Charset", "utf-8");
            // check for the request-global flag passed from the periodicals.
            // you can override it per request, but that seems unlikely.
            // this is a hack, if you have a better idea without touching dozens of methods, please share :)
            if (extendSession == null) {
                extendSession = Tools.apiRequestShouldExtendSession();
            }
            if (!extendSession) {
                requestBuilder.addHeader("X-Graylog2-No-Session-Extension", "true");
            }
            return requestBuilder;
        }

        // default visibility for tests
        public URL prepareUrl(ClusterEntity target) {
            // if this is null there's not much we can do anyway...
            Preconditions.checkNotNull(pathTemplate, "path() needs to be set to a non-null value.");

            HttpUrl builtUrl;
            try {
                String path;
                if (pathParams.isEmpty()) {
                    path = pathTemplate;
                } else {
                    path = MessageFormat.format(pathTemplate, pathParams.toArray());
                }

                final HttpUrl.Builder builder = HttpUrl.parse(target.getTransportAddress())
                        .newBuilder()
                        .encodedPath(path);

                for (String key : queryParams.keySet()) {
                    for (String value : queryParams.get(key)) {
                        builder.addQueryParameter(key, value);
                    }
                }

                if (unauthenticated && sessionId != null) {
                    LOG.error("Both session() and unauthenticated() are set for this request, this is a bug, using session id.", new Throwable());
                }
                if (sessionId != null) {
                    // pass the current session id via basic auth and special "password"
                    builder.username(sessionId);
                    builder.password("session");
                }

                builtUrl = builder.build();
                return builtUrl.url();
            } catch (RuntimeException e) {
                LOG.error("Could not build target URL", e);
                throw e;
            }
        }

        @Override
        public InputStream executeStreaming() throws APIException, IOException {
            if (radio != null && (node != null || nodes != null)) {
                throw new RuntimeException("You set both and a Node and a Radio as target. This is not possible.");
            }

            final ClusterEntity target;

            if (radio == null) {
                if (node == null) {
                    if (nodes != null) {
                        LOG.error("Multiple nodes are set, but execute() was called. This is most likely a bug and you meant to call executeOnAll()!", new Throwable());
                    }
                    node(serverNodes.any());
                }

                target = node;
            } else {
                target = radio;
            }

            ensureAuthentication();
            final URL url = prepareUrl(target);
            final AsyncHttpClient.BoundRequestBuilder requestBuilder = requestBuilderForUrl(url);
            requestBuilder.addHeader(HttpHeaders.ACCEPT, mediaType.toString());

            final Request request = requestBuilder.build();
            if (LOG.isDebugEnabled()) {
                LOG.debug("API Request: {}", request);
            }

            // Set 200 OK as standard if not defined.
            ensureExpectedResponseCodes();

            try {
                final AsyncByteBufferInputStream stream = new AsyncByteBufferInputStream();
                requestBuilder.execute(new TransferCompletionHandler().addTransferListener(new TransferListener() {
                    @Override
                    public void onRequestHeadersSent(FluentCaseInsensitiveStringsMap headers) {
                    }

                    @Override
                    public void onResponseHeadersReceived(FluentCaseInsensitiveStringsMap headers) {
                        target.touch();
                    }

                    @Override
                    public void onBytesSent(long amount, long current, long total) {
                    }

                    @Override
                    public void onBytesReceived(byte[] bytes) throws IOException {
                        stream.putBuffer(ByteBuffer.wrap(bytes));
                    }

                    @Override
                    public void onRequestResponseCompleted() {
                        stream.setDone(true);
                    }

                    @Override
                    public void onThrowable(Throwable t) {
                        stream.setFailed(t);
                    }
                }));
                return stream;
            } catch (Exception e) {
                LOG.error("unhandled IOException", rootCause(e));
                target.markFailure();
                throw e;
            }
        }
    }
}
