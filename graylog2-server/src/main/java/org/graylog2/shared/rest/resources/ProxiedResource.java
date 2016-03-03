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

import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import retrofit2.Call;
import retrofit2.Response;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class ProxiedResource extends RestResource {
    protected final String authenticationToken;
    protected final NodeService nodeService;

    protected final RemoteInterfaceProvider remoteInterfaceProvider;

    protected ProxiedResource(@Context HttpHeaders httpHeaders, NodeService nodeService, RemoteInterfaceProvider remoteInterfaceProvider) {
        this.nodeService = nodeService;
        this.remoteInterfaceProvider = remoteInterfaceProvider;
        final List<String> authenticationTokens = httpHeaders.getRequestHeader("Authorization");
        if (authenticationTokens != null && authenticationTokens.size() >= 1) {
            this.authenticationToken = authenticationTokens.get(0);
        } else {
            this.authenticationToken = null;
        }
    }

    protected <T, K> Map<String, Optional<K>> getForAllNodes(Function<T, Call<K>> fn, Function<String, Optional<T>> interfaceProvider) {
        return getForAllNodes(fn, interfaceProvider, Function.identity());
    }

    protected <T, K, L> Map<String, Optional<K>> getForAllNodes(Function<T, Call<L>> fn, Function<String, Optional<T>> interfaceProvider, Function<L, K> transformer) {
        return this.nodeService.allActive()
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                try {
                    final Optional<T> remoteInterface = interfaceProvider.apply(entry.getKey());
                    if (!remoteInterface.isPresent()) {
                        return Optional.empty();
                    }
                    final Response<L> response = fn.apply(remoteInterface.get()).execute();
                    if (response.isSuccess()) {
                        return Optional.of(transformer.apply(response.body()));
                    } else {
                        return Optional.empty();
                    }
                } catch (IOException e) {
                    return Optional.empty();
                }
            }));
    }

    protected <T> Function<String, Optional<T>> createRemoteInterfaceProvider(Class<T> interfaceClass) {
        return (nodeId) -> {
            try {
                final Node targetNode = nodeService.byNodeId(nodeId);
                return Optional.of(this.remoteInterfaceProvider.get(targetNode, this.authenticationToken, interfaceClass));
            } catch (NodeNotFoundException e) {
                return Optional.empty();
            }
        };
    }
}
