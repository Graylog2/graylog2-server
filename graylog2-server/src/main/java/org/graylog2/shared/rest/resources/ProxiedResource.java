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
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class ProxiedResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(ProxiedResource.class);

    protected final String authenticationToken;
    protected final NodeService nodeService;

    protected final RemoteInterfaceProvider remoteInterfaceProvider;
    private final ExecutorService executor;

    protected ProxiedResource(@Context HttpHeaders httpHeaders,
                              NodeService nodeService,
                              RemoteInterfaceProvider remoteInterfaceProvider,
                              ExecutorService executorService) {
        this.nodeService = nodeService;
        this.remoteInterfaceProvider = remoteInterfaceProvider;
        this.executor = executorService;
        final List<String> authenticationTokens = httpHeaders.getRequestHeader("Authorization");
        if (authenticationTokens != null && authenticationTokens.size() >= 1) {
            this.authenticationToken = authenticationTokens.get(0);
        } else {
            this.authenticationToken = null;
        }
    }

    /**
     * Prefer using {@link ProxiedResource#requestOnAllNodes(Function, Function)} instead.
     * The new method properly handles the case of `No-Content` response and provides
     * detailed report per each node API call.
     */
    @Deprecated
    protected <RemoteInterfaceType, RemoteCallResponseType> Map<String, Optional<RemoteCallResponseType>> getForAllNodes(Function<RemoteInterfaceType, Call<RemoteCallResponseType>> fn, Function<String, Optional<RemoteInterfaceType>> interfaceProvider) {
        return getForAllNodes(fn, interfaceProvider, Function.identity());
    }

    /**
     * Prefer using {@link ProxiedResource#requestOnAllNodes(Function, Function, Function)} instead.
     * The new method properly handles the case of `No-Content` response and provides
     * detailed report per each node API call.
     */
    @Deprecated
    protected <RemoteInterfaceType, FinalResponseType, RemoteCallResponseType> Map<String, Optional<FinalResponseType>> getForAllNodes(Function<RemoteInterfaceType, Call<RemoteCallResponseType>> fn, Function<String, Optional<RemoteInterfaceType>> interfaceProvider, Function<RemoteCallResponseType, FinalResponseType> transformer) {
        final Map<String, Future<Optional<FinalResponseType>>> futures = this.nodeService.allActive().keySet().stream()
                .collect(Collectors.toMap(Function.identity(), node -> interfaceProvider.apply(node)
                        .map(r -> executor.submit(() -> {
                            final Call<RemoteCallResponseType> call = fn.apply(r);
                            try {
                                final Response<RemoteCallResponseType> response = call.execute();
                                if (response.isSuccessful()) {
                                    return Optional.of(transformer.apply(response.body()));
                                } else {
                                    LOG.warn("Unable to call {} on node <{}>, result: {}", call.request().url(), node, response.message());
                                    return Optional.<FinalResponseType>empty();
                                }
                            } catch (IOException e) {
                                if (LOG.isDebugEnabled()) {
                                    LOG.warn("Unable to call {} on node <{}>", call.request().url(), node, e);
                                } else {
                                    LOG.warn("Unable to call {} on node <{}>: {}", call.request().url(), node, e.getMessage());
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
                        return entry.getValue().get();
                    } catch (InterruptedException | ExecutionException e) {
                        LOG.debug("Couldn't retrieve future", e);
                        return Optional.empty();
                    }
                }));
    }

    protected <RemoteInterfaceType> Function<String, Optional<RemoteInterfaceType>> createRemoteInterfaceProvider(Class<RemoteInterfaceType> interfaceClass) {
        return nodeId -> {
            try {
                final Node targetNode = nodeService.byNodeId(nodeId);
                return Optional.of(this.remoteInterfaceProvider.get(targetNode, this.authenticationToken, interfaceClass));
            } catch (NodeNotFoundException e) {
                LOG.warn("Node <" + nodeId + "> not found while trying to call " + interfaceClass.getName() + " on it.");
                return Optional.empty();
            }
        };
    }

    protected <RemoteInterfaceType, RemoteCallResponseType> Map<String, CallResult<RemoteCallResponseType>> requestOnAllNodes(
            Function<String, Optional<RemoteInterfaceType>> interfaceProvider,
            Function<RemoteInterfaceType, Call<RemoteCallResponseType>> fn
    ) {
        return requestOnAllNodes(interfaceProvider, fn, Function.identity());
    }

    /**
     * This method concurrently performs an API call on all active nodes.
     *
     * @param remoteInterfaceProvider provides an instance of Retrofit HTTP client for the target API
     * @param remoteInterfaceCallProvider provides an invocation of a Retrofit method for the intended API call.
     * @param responseTransformer applies transformations to HTTP response body
     *
     * @param <RemoteInterfaceType> Type of the Retrofit HTTP client
     * @param <RemoteCallResponseType> Type of the API call response body
     * @param <FinalResponseType> Type after applying the transformations
     *
     * @return Detailed report on call results per each active node.
     */
    protected <RemoteInterfaceType, RemoteCallResponseType, FinalResponseType> Map<String, CallResult<FinalResponseType>> requestOnAllNodes(
            Function<String, Optional<RemoteInterfaceType>> remoteInterfaceProvider,
            Function<RemoteInterfaceType, Call<RemoteCallResponseType>> remoteInterfaceCallProvider,
            Function<RemoteCallResponseType, FinalResponseType> responseTransformer
    ) {

        final Map<String, Future<CallResult<FinalResponseType>>> futures = this.nodeService.allActive().keySet().stream()
                .collect(Collectors.toMap(Function.identity(), nodeId -> executor.submit(() -> {
                            try {
                                return CallResult.success(doNodeApiCall(nodeId, remoteInterfaceProvider, remoteInterfaceCallProvider, responseTransformer));
                            } catch (Exception e) {
                                if (LOG.isDebugEnabled()) {
                                    LOG.warn("Failed to call API on node {}, cause: {}", nodeId, e.getMessage(), e);
                                } else {
                                    LOG.warn("Failed to call API on node {}, cause: {}", nodeId, e.getMessage());
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
                        return entry.getValue().get();
                    } catch (InterruptedException | ExecutionException e) {
                        LOG.debug("Couldn't retrieve future", e);
                        throw new RuntimeException(e);
                    }
                }));
    }

    /**
     * Execute the given remote interface function on the master node.
     * <p>
     * This is used to forward an API request to the master node. It is useful in situations where an API call can
     * only be executed on the master node.
     * <p>
     * The returned {@link MasterResponse} object is constructed from the remote response's status code and body.
     */
    protected <RemoteInterfaceType, RemoteCallResponseType> MasterResponse<RemoteCallResponseType> requestOnMaster(
            Function<RemoteInterfaceType, Call<RemoteCallResponseType>> remoteInterfaceFunction,
            Function<String, Optional<RemoteInterfaceType>> remoteInterfaceProvider
    ) throws IOException {
        final Node masterNode = nodeService.allActive().values().stream()
                .filter(Node::isMaster)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No active master node found"));

        return doNodeApiCall(masterNode.getNodeId(), remoteInterfaceProvider, remoteInterfaceFunction, Function.identity());
    }

    private  <RemoteInterfaceType, RemoteCallResponseType, FinalResponseType> MasterResponse<FinalResponseType> doNodeApiCall(
            String nodeId,
            Function<String, Optional<RemoteInterfaceType>> remoteInterfaceProvider,
            Function<RemoteInterfaceType, Call<RemoteCallResponseType>> remoteInterfaceFunction,
            Function<RemoteCallResponseType, FinalResponseType> transformer
    ) throws IOException {
        final RemoteInterfaceType remoteInterfaceType = remoteInterfaceProvider.apply(nodeId)
                .orElseThrow(() -> new IllegalStateException("Node " + nodeId + " not found"));

        final Call<RemoteCallResponseType> call = remoteInterfaceFunction.apply(remoteInterfaceType);
        final Response<RemoteCallResponseType> response = call.execute();

        final byte[] errorBody = response.errorBody() == null ? null : response.errorBody().bytes();

        return MasterResponse.create(response.isSuccessful(), response.code(), transformer.apply(response.body()), errorBody);
    }

    /**
     * This wrapper is intended to provide additional server error information
     * if something went wrong beyond the actual API HTTP call.
     */
    @AutoValue
    public static abstract class CallResult<ResponseType> {

        @JsonProperty("success")
        public abstract boolean isSuccess();

        @JsonProperty("server_error_message")
        @Nullable
        public abstract String serverErrorMessage();

        @JsonProperty("response")
        @Nullable
        public abstract MasterResponse<ResponseType> response();

        public static <ResponseType> CallResult<ResponseType> success(@Nonnull MasterResponse<ResponseType> response) {
            return new AutoValue_ProxiedResource_CallResult<>(true, null, response);
        }

        public static <ResponseType> CallResult<ResponseType> error(@Nonnull String serverErrorMessage) {
            return new AutoValue_ProxiedResource_CallResult<>(false, serverErrorMessage, null);
        }
    }

    /**
     * The name of the class is preserved for the sake of backward compatibility
     * with existing plugins.
     */
    @AutoValue
    public static abstract class MasterResponse<ResponseType> {
        /**
         * Indicates whether the request has been successful or not.
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

        public static <ResponseType> MasterResponse<ResponseType> create(boolean isSuccess,
                                                                         int code,
                                                                         @Nullable ResponseType entity,
                                                                         @Nullable byte[] error) {
            return new AutoValue_ProxiedResource_MasterResponse<>(isSuccess, code, Optional.ofNullable(entity), Optional.ofNullable(error));
        }
    }
}
