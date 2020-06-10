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

package org.graylog2.shared.rest.resources;

import com.google.auto.value.AutoValue;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;

import javax.annotation.Nullable;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
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

    protected <RemoteInterfaceType, RemoteCallResponseType> Map<String, Optional<RemoteCallResponseType>> getForAllNodes(Function<RemoteInterfaceType, Call<RemoteCallResponseType>> fn, Function<String, Optional<RemoteInterfaceType>> interfaceProvider) {
        return getForAllNodes(fn, interfaceProvider, Function.identity());
    }

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
        return (nodeId) -> {
            try {
                final Node targetNode = nodeService.byNodeId(nodeId);
                return Optional.of(this.remoteInterfaceProvider.get(targetNode, this.authenticationToken, interfaceClass));
            } catch (NodeNotFoundException e) {
                LOG.warn("Node <" + nodeId + "> not found while trying to call " + interfaceClass.getName() + " on it.");
                return Optional.empty();
            }
        };
    }

    /**
     * Execute the given remote interface function on the primary node.
     * <p>
     * This is used to forward an API request to the primary node. It is useful in situations where an API call can
     * only be executed on the primary node.
     * <p>
     * The returned {@link PrimaryResponse} object is constructed from the remote response's status code and body.
     */
    protected <RemoteInterfaceType, RemoteCallResponseType> PrimaryResponse<RemoteCallResponseType> requestOnPrimary(
            Function<RemoteInterfaceType, Call<RemoteCallResponseType>> remoteInterfaceFunction,
            Function<String, Optional<RemoteInterfaceType>> remoteInterfaceProvider
    ) throws IOException {
        final Node primaryNode = nodeService.allActive().values().stream()
                .filter(Node::isPrimary)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No active primary node found"));

        final RemoteInterfaceType remoteInterfaceType = remoteInterfaceProvider.apply(primaryNode.getNodeId())
                .orElseThrow(() -> new IllegalStateException("Primary node " + primaryNode.getNodeId() + " not found"));

        final Call<RemoteCallResponseType> call = remoteInterfaceFunction.apply(remoteInterfaceType);
        final Response<RemoteCallResponseType> response = call.execute();

        final byte[] errorBody = response.errorBody() == null ? null : response.errorBody().bytes();

        return PrimaryResponse.create(response.isSuccessful(), response.code(), response.body(), errorBody);
    }

    @AutoValue
    protected static abstract class PrimaryResponse<ResponseType> {
        /**
         * Indicates whether the request has been successful or not.
         * @return {@code true} for a successful request, {@code false} otherwise
         */
        public abstract boolean isSuccess();

        /**
         * Returns the HTTP status code of the response.
         *
         * @return HTTP status code
         */
        public abstract int code();

        /**
         * Returns the typed response object if the request was successful. Otherwise it returns an empty {@link Optional}.
         *
         * @return typed response object or empty {@link Optional}
         */
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

        public static <ResponseType> PrimaryResponse<ResponseType> create(boolean isSuccess,
                                                                         int code,
                                                                         @Nullable ResponseType entity,
                                                                         @Nullable byte[] error) {
            return new AutoValue_ProxiedResource_PrimaryResponse<>(isSuccess, code, Optional.ofNullable(entity), Optional.ofNullable(error));
        }
    }
}
