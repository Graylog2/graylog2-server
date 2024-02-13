/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.shared.rest.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.Stopwatch;
import okhttp3.ResponseBody;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.shared.utilities.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.ConnectionCallback;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public abstract class ProxiedResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(ProxiedResource.class);

    private final String authenticationToken;
    protected final NodeService nodeService;

    protected final RemoteInterfaceProvider remoteInterfaceProvider;
    private final ExecutorService executor;

    @Inject
    @Named("proxied_requests_default_call_timeout")
    private com.github.joschi.jadconfig.util.Duration defaultProxyCallTimeout;

    protected ProxiedResource(@Context HttpHeaders httpHeaders,
                              NodeService nodeService,
                              RemoteInterfaceProvider remoteInterfaceProvider,
                              ExecutorService executorService) {
        this.nodeService = nodeService;
        this.remoteInterfaceProvider = remoteInterfaceProvider;
        this.executor = executorService;
        this.authenticationToken = authenticationToken(httpHeaders);
    }

    protected Duration getDefaultProxyCallTimeout() {
        return Duration.ofMillis(requireNonNull(defaultProxyCallTimeout, "defaultProxyCallTimeout not injected").toMilliseconds());
    }

    protected void processAsync(AsyncResponse asyncResponse, Supplier<Object> responseSupplier) {
        requireNonNull(asyncResponse, "asyncResponse cannot be null");
        requireNonNull(responseSupplier, "responseSupplier cannot be null");

        asyncResponse.register((ConnectionCallback) disconnected -> LOG.debug("Remote client disconnected"));

        LOG.debug("Schedule async request");
        executor.submit(() -> {
            try {
                LOG.debug("Running async request");
                var response = responseSupplier.get();
                LOG.debug("Resuming async response");
                asyncResponse.resume(response);
            } catch (Throwable e) {
                LOG.debug("Async request failed");
                LOG.debug("Resuming async response with an error", e);
                asyncResponse.resume(e);
            }
        });
    }

    public static String authenticationToken(HttpHeaders httpHeaders) {
        final Cookie authenticationCookie = httpHeaders.getCookies().get("authentication");
        if (authenticationCookie != null) {
            final String sessionId = authenticationCookie.getValue();
            final String credentials = sessionId + ":session";
            final String base64Credentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            return "Basic " + base64Credentials;
        }

        final List<String> authorizationHeader = httpHeaders.getRequestHeader("Authorization");
        if (authorizationHeader != null && !authorizationHeader.isEmpty()) {
            return authorizationHeader.get(0);
        }

        return null;
    }

    /**
     * Gets an authentication token to be used in an Authorization header of forwarded requests. It was extracted
     * from the authentication information of the original request.
     *
     * @return An authentication token if the request was authenticated and one could be extracted from the original
     */
    @Nullable
    protected String getAuthenticationToken() {
        return authenticationToken;
    }

    /**
     * Prefer using {@link #requestOnAllNodes(Class, Function)} instead.
     * The new method properly handles the case of `No-Content` response and provides
     * detailed report per each node API call.
     */
    @Deprecated
    protected <RemoteInterfaceType, RemoteCallResponseType> Map<String, Optional<RemoteCallResponseType>> getForAllNodes(
            Function<RemoteInterfaceType, Call<RemoteCallResponseType>> fn,
            Function<String, Optional<RemoteInterfaceType>> interfaceProvider
    ) {
        return getForAllNodes(fn, interfaceProvider, Function.identity(), Duration.ZERO);
    }

    /**
     * Prefer using {@link #requestOnAllNodes(Class, Function, Duration)} instead.
     * The new method properly handles the case of `No-Content` response and provides
     * detailed report per each node API call.
     */
    @Deprecated
    protected <RemoteInterfaceType, RemoteCallResponseType> Map<String, Optional<RemoteCallResponseType>> getForAllNodes(
            Function<RemoteInterfaceType, Call<RemoteCallResponseType>> fn,
            Function<String, Optional<RemoteInterfaceType>> interfaceProvider,
            Duration timeout
    ) {
        return getForAllNodes(fn, interfaceProvider, Function.identity(), timeout);
    }

    /**
     * Prefer using {@link ProxiedResource#requestOnAllNodes(Class, Function, Function)} instead.
     * The new method properly handles the case of `No-Content` response and provides
     * detailed report per each node API call.
     */
    @Deprecated
    protected <RemoteInterfaceType, FinalResponseType, RemoteCallResponseType> Map<String, Optional<FinalResponseType>> getForAllNodes(
            Function<RemoteInterfaceType, Call<RemoteCallResponseType>> fn,
            Function<String, Optional<RemoteInterfaceType>> interfaceProvider,
            Function<RemoteCallResponseType, FinalResponseType> transformer
    ) {
        return getForAllNodes(fn, interfaceProvider, transformer, Duration.ZERO);
    }

    /**
     * Prefer using {@link ProxiedResource#requestOnAllNodes(Class, Function, Function, Duration)} instead.
     * The new method properly handles the case of `No-Content` response and provides
     * detailed report per each node API call.
     */
    @Deprecated
    protected <RemoteInterfaceType, FinalResponseType, RemoteCallResponseType> Map<String, Optional<FinalResponseType>> getForAllNodes(
            Function<RemoteInterfaceType, Call<RemoteCallResponseType>> fn,
            Function<String, Optional<RemoteInterfaceType>> interfaceProvider,
            Function<RemoteCallResponseType, FinalResponseType> transformer,
            Duration timeout) {
        final long callTimeoutMs = Duration.ZERO.equals(timeout) ? getDefaultProxyCallTimeout().toMillis() : timeout.toMillis();

        final Map<String, Future<Optional<FinalResponseType>>> futures = this.nodeService.allActive().keySet().stream()
                .collect(Collectors.toMap(Function.identity(), node -> interfaceProvider.apply(node)
                        .map(r -> executor.submit(() -> {
                            final Call<RemoteCallResponseType> call = fn.apply(r);
                            final Stopwatch sw = Stopwatch.createUnstarted();
                            try {
                                call.timeout().timeout(callTimeoutMs, TimeUnit.MILLISECONDS);
                                sw.start();
                                final Response<RemoteCallResponseType> response = call.execute();
                                if (response.isSuccessful()) {
                                    return Optional.of(transformer.apply(response.body()));
                                } else {
                                    LOG.warn("Unable to call {} on node <{}>, result: {} (duration: {} ms)",
                                            call.request().url(), node, response.message(), sw.stop().elapsed().toMillis());
                                    return Optional.<FinalResponseType>empty();
                                }
                            } catch (IOException e) {
                                final long elapsedMs = sw.stop().elapsed().toMillis();
                                if (LOG.isDebugEnabled()) {
                                    LOG.warn("Unable to call {} on node <{}> (duration: {} ms)", call.request().url(), node, elapsedMs, e);
                                } else {
                                    LOG.warn("Unable to call {} on node <{}>: {} (duration: {} ms)", call.request().url(), node, e.getMessage(), elapsedMs);
                                }
                                return Optional.<FinalResponseType>empty();
                            }
                        }))
                        .orElse(CompletableFuture.completedFuture(Optional.empty()))
                ));

        return futures
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                    try {
                        // Never block without timeout to avoid saturating the thread pool with waiting futures.
                        // We double the timeout that we use for the HTTP call here to ensure that the HTTP
                        // requests times out before we hit the timeout on this Future#get call.
                        return entry.getValue().get(callTimeoutMs * 2, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException | ExecutionException e) {
                        LOG.debug("Couldn't retrieve future", e);
                        return Optional.empty();
                    } catch (TimeoutException e) {
                        LOG.debug("Upstream timeout for node <{}>", entry.getKey());
                        return Optional.empty();
                    }
                }));
    }

    protected <RemoteInterfaceType> Function<String, Optional<RemoteInterfaceType>> createRemoteInterface(Class<RemoteInterfaceType> interfaceClass, @Nullable Duration timeout) {
        return nodeId -> {
            try {
                final Node targetNode = nodeService.byNodeId(nodeId);
                return Optional.of(this.remoteInterfaceProvider.get(targetNode, getAuthenticationToken(), interfaceClass, timeout == null ? getDefaultProxyCallTimeout() : timeout));
            } catch (NodeNotFoundException e) {
                LOG.warn("Node <" + nodeId + "> not found while trying to call " + interfaceClass.getName() + " on it.");
                return Optional.empty();
            }
        };
    }

    protected <RemoteInterfaceType, RemoteCallResponseType> Map<String, CallResult<RemoteCallResponseType>> requestOnAllNodes(
            Class<RemoteInterfaceType> interfaceClass,
            Function<RemoteInterfaceType, Call<RemoteCallResponseType>> fn
    ) {
        return requestOnAllNodes(interfaceClass, fn, Function.identity(), null);
    }

    protected <RemoteInterfaceType, RemoteCallResponseType> Map<String, CallResult<RemoteCallResponseType>> requestOnAllNodes(
            Class<RemoteInterfaceType> interfaceClass,
            Function<RemoteInterfaceType, Call<RemoteCallResponseType>> fn,
            Duration timeout
    ) {
        return requestOnAllNodes(interfaceClass, fn, Function.identity(), timeout);
    }

    protected <RemoteInterfaceType, RemoteCallResponseType, FinalResponseType> Map<String, CallResult<FinalResponseType>> requestOnAllNodes(
            Class<RemoteInterfaceType> interfaceClass,
            Function<RemoteInterfaceType, Call<RemoteCallResponseType>> remoteInterfaceCallProvider,
            Function<RemoteCallResponseType, FinalResponseType> responseTransformer
    ) {
        return requestOnAllNodes(interfaceClass, remoteInterfaceCallProvider, responseTransformer, null);
    }

    /**
     * This method concurrently performs an API call on all active nodes.
     *
     * @param interfaceClass              The class of the Retrotfit interface for this call
     * @param remoteInterfaceCallProvider provides an invocation of a Retrofit method for the intended API call.
     * @param responseTransformer         applies transformations to HTTP response body
     * @param <RemoteInterfaceType>       Type of the Retrofit HTTP client
     * @param <RemoteCallResponseType>    Type of the API call response body
     * @param <FinalResponseType>         Type after applying the transformations
     * @return Detailed report on call results per each active node.
     */
    protected <RemoteInterfaceType, RemoteCallResponseType, FinalResponseType> Map<String, CallResult<FinalResponseType>> requestOnAllNodes(
            Class<RemoteInterfaceType> interfaceClass,
            Function<RemoteInterfaceType, Call<RemoteCallResponseType>> remoteInterfaceCallProvider,
            Function<RemoteCallResponseType, FinalResponseType> responseTransformer,
            @Nullable Duration timeout
    ) {
        final long callTimeoutMs = (timeout == null) ? getDefaultProxyCallTimeout().toMillis() : timeout.toMillis();

        final Map<String, Future<CallResult<FinalResponseType>>> futures = this.nodeService.allActive().keySet().stream()
                .collect(Collectors.toMap(Function.identity(), nodeId -> executor.submit(() -> {
                            final Stopwatch sw = Stopwatch.createStarted();
                            try {
                                return CallResult.success(doNodeApiCall(nodeId, interfaceClass, remoteInterfaceCallProvider, responseTransformer, timeout));
                            } catch (Exception e) {
                                final long elapsedMs = sw.stop().elapsed().toMillis();
                                if (LOG.isDebugEnabled()) {
                                    LOG.warn("Failed to call API on node <{}>, cause: {} (duration: {} ms)", nodeId, e.getMessage(), elapsedMs, e);
                                } else {
                                    LOG.warn("Failed to call API on node <{}>, cause: {} (duration: {} ms)", nodeId, e.getMessage(), elapsedMs);
                                }
                                return CallResult.error(e.getMessage());
                            }
                        })
                ));

        return futures
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                    try {
                        // Never block without timeout to avoid saturating the thread pool with waiting futures.
                        // We double the timeout that we use for the HTTP call here to ensure that the HTTP
                        // requests times out before we hit the timeout on this Future#get call.
                        return entry.getValue().get(callTimeoutMs * 2, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException | ExecutionException e) {
                        LOG.debug("Couldn't retrieve future", e);
                        throw new RuntimeException(e);
                    } catch (TimeoutException e) {
                        LOG.debug("Upstream timeout for node <{}>", entry.getKey());
                        return CallResult.upstreamTimeout(entry.getKey());
                    }
                }));
    }

    /**
     * Execute the given remote interface function on the leader node.
     * <p>
     * This is used to forward an API request to the leader node. It is useful in situations where an API call can only
     * be executed on the leader node.
     * <p>
     * The returned {@link NodeResponse} object is constructed from the remote response's status code and body.
     */
    protected <RemoteInterfaceType, RemoteCallResponseType> NodeResponse<RemoteCallResponseType> requestOnLeader(
            Function<RemoteInterfaceType, Call<RemoteCallResponseType>> remoteInterfaceFunction,
            Class<RemoteInterfaceType> interfaceClass, Duration timeout
    ) throws IOException {
        final Node leaderNode = nodeService.allActive().values().stream()
                .filter(Node::isLeader)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No active leader node found"));

        return doNodeApiCall(leaderNode.getNodeId(), interfaceClass, remoteInterfaceFunction, Function.identity(), timeout);
    }

    /**
     * Execute the given remote interface function on the leader node.
     * <p>
     * This is used to forward an API request to the leader node. It is useful in situations where an API call can only
     * be executed on the leader node.
     * <p>
     * The returned {@link NodeResponse} object is constructed from the remote response's status code and body.
     */
    protected <RemoteInterfaceType, RemoteCallResponseType> NodeResponse<RemoteCallResponseType> requestOnLeader(
            Function<RemoteInterfaceType, Call<RemoteCallResponseType>> remoteInterfaceFunction,
            Class<RemoteInterfaceType> interfaceClass
    ) throws IOException {
        return requestOnLeader(remoteInterfaceFunction, interfaceClass, getDefaultProxyCallTimeout());
    }

    protected <RemoteInterfaceType, RemoteCallResponseType, FinalResponseType> NodeResponse<FinalResponseType> doNodeApiCall(
            String nodeId,
            Class<RemoteInterfaceType> interfaceClass,
            Function<RemoteInterfaceType, Call<RemoteCallResponseType>> remoteInterfaceFunction,
            Function<RemoteCallResponseType, FinalResponseType> transformer,
            @Nullable Duration timeout
    ) throws IOException {
        final Function<String, Optional<RemoteInterfaceType>> remoteInterface = createRemoteInterface(interfaceClass, timeout);
        final RemoteInterfaceType remoteInterfaceType = remoteInterface.apply(nodeId)
                .orElseThrow(() -> new IllegalStateException("Node " + nodeId + " not found"));
        final Call<RemoteCallResponseType> call = remoteInterfaceFunction.apply(remoteInterfaceType);

        final long callTimeoutMs = (timeout == null) ? getDefaultProxyCallTimeout().toMillis() : timeout.toMillis();
        call.timeout().timeout(callTimeoutMs, TimeUnit.MILLISECONDS);

        final Response<RemoteCallResponseType> response = call.execute();

        try (final ResponseBody errorBody = response.errorBody()) {
            return NodeResponse.create(
                    response.isSuccessful(),
                    response.code(),
                    transformer.apply(response.body()),
                    errorBody == null ? null : errorBody.bytes()
            );
        }
    }

    /**
     * Helper function to remove the {@link CallResult} wrapper
     *
     * @param input responses that are wrapped with a {@link CallResult}
     * @return the response in the legacy format of {@code Map<String, Optional<T>>}
     */
    protected <T> Map<String, Optional<T>> stripCallResult(Map<String, CallResult<T>> input) {
        return input.entrySet().stream()
                .filter(e -> e.getValue().response() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().response().entity()));
    }

    /**
     * This wrapper is intended to provide additional server error information
     * if something went wrong beyond the actual API HTTP call.
     */
    @AutoValue
    public static abstract class CallResult<ResponseType> {

        @JsonProperty("call_executed")
        public abstract boolean isCallExecuted();

        @JsonProperty("server_error_message")
        @Nullable
        public abstract String serverErrorMessage();

        @JsonProperty("response")
        @Nullable
        public abstract NodeResponse<ResponseType> response();

        public static <ResponseType> CallResult<ResponseType> success(@Nonnull NodeResponse<ResponseType> response) {
            return new AutoValue_ProxiedResource_CallResult<>(true, null, response);
        }

        public static <ResponseType> CallResult<ResponseType> error(@Nonnull String serverErrorMessage) {
            return new AutoValue_ProxiedResource_CallResult<>(false, serverErrorMessage, null);
        }

        public static <ResponseType> CallResult<ResponseType> upstreamTimeout(@Nonnull String nodeId) {
            final String msg = StringUtils.f("upstream timeout (node=%s)", nodeId);
            return new AutoValue_ProxiedResource_CallResult<>(true, msg, null);
        }
    }


    @AutoValue
    public static abstract class NodeResponse<ResponseType> {
        /**
         * Indicates whether the request has been successful or not.
         *
         * @return {@code true} for a successful request, {@code false} otherwise
         */
        @JsonProperty("success")
        public abstract boolean isSuccess();

        /**
         * Returns the HTTP status code of the response.
         *
         * @return HTTP status code
         */
        @JsonProperty("code")
        public abstract int code();

        /**
         * Returns the typed response object if the request was successful. Otherwise it returns an empty {@link Optional}.
         *
         * @return typed response object or empty {@link Optional}
         */
        @JsonProperty("entity")
        public abstract Optional<ResponseType> entity();

        /**
         * Returns the error response if the request wasn't successful. Otherwise it returns an empty {@link Optional}.
         *
         * @return error response or empty {@link Optional}
         */
        public abstract Optional<byte[]> error();

        /**
         * Convenience method that returns either the body of a successful request or if that one is {@code null},
         * it returns the error body.
         * <p>
         * Use {@link #entity()} the get the typed response object. (only available if {@link #isSuccess()} is {@code true})
         *
         * @return either the {@link #entity()} or the {@link #error()}
         */
        public Object body() {
            return entity().isPresent() ? entity().get() : error().orElse(null);
        }

        @JsonProperty("error_text")
        @Nullable
        public String errorText() {
            return error()
                    .map(bytes -> new String(bytes, Charset.defaultCharset()))
                    .orElse(null);
        }

        public static <ResponseType> NodeResponse<ResponseType> create(boolean isSuccess,
                                                                       int code,
                                                                       @Nullable ResponseType entity,
                                                                       @Nullable byte[] error) {
            return new AutoValue_ProxiedResource_NodeResponse<>(isSuccess, code, Optional.ofNullable(entity), Optional.ofNullable(error));
        }
    }

    /**
     * @deprecated Use {@link NodeResponse} instead.
     */
    @Deprecated
    @AutoValue
    public static abstract class MasterResponse<ResponseType> {

        public abstract boolean isSuccess();

        public abstract int code();

        public abstract Optional<ResponseType> entity();

        public abstract Optional<byte[]> error();

        public Object body() {
            return entity().isPresent() ? entity().get() : error().orElse(null);
        }

        public static <ResponseType> MasterResponse<ResponseType> create(NodeResponse<ResponseType> nodeResponse) {
            return new AutoValue_ProxiedResource_MasterResponse<>(
                    nodeResponse.isSuccess(), nodeResponse.code(), nodeResponse.entity(), nodeResponse.error());
        }
    }
}
