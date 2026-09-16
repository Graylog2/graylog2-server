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
package org.graylog2.rest.resources.datanodes;

import org.apache.commons.io.FilenameUtils;
import org.graylog2.indexer.datanode.ProxyRequestAdapter;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Shared allowlist evaluation for the data node proxy resources. A request is allowed if, after its path has
 * been normalized to a canonical form (resolving "." and ".." segments), at least one of the configured rules
 * matches it. Paths that attempt to traverse above their own root are rejected outright.
 */
public class DataNodeProxyAllowlist {
    private final List<Predicate<ProxyRequestAdapter.ProxyRequest>> rules;

    public DataNodeProxyAllowlist(List<Predicate<ProxyRequestAdapter.ProxyRequest>> rules) {
        this.rules = rules;
    }

    /**
     * An allowlist that permits any path, for use when the allowlist feature is disabled. Requests still go
     * through {@link #authorize(ProxyRequestAdapter.ProxyRequest)}, so a path can never traverse above its own
     * root even with the allowlist "off" — only the rule matching itself is bypassed.
     */
    public static DataNodeProxyAllowlist allowAll() {
        return new DataNodeProxyAllowlist(List.of(request -> true));
    }

    /**
     * Normalizes the request's path and checks the result against the allowlist rules. Rule matching and any
     * downstream forwarding must both use the returned, normalized request rather than the original one, so
     * that a path segment like "_cluster/../secrets" can't pass the rules on its raw, pre-normalization text
     * while actually resolving to a different, disallowed endpoint.
     *
     * @return the request with its path replaced by the normalized form
     * @throws RequestNotAllowedException if the path traverses above its root, or no rule matches it
     */
    public ProxyRequestAdapter.ProxyRequest authorize(ProxyRequestAdapter.ProxyRequest request) throws RequestNotAllowedException {
        final String normalizedPath = normalize(request.path())
                .orElseThrow(() -> new RequestNotAllowedException(f("Path traverses above its root: %s", request.path())));
        final ProxyRequestAdapter.ProxyRequest normalizedRequest = withPath(request, normalizedPath);
        if (!isAllowed(normalizedRequest)) {
            throw new RequestNotAllowedException(f("Path is not allowlisted: %s", normalizedPath));
        }
        return normalizedRequest;
    }

    private boolean isAllowed(ProxyRequestAdapter.ProxyRequest request) {
        return rules.stream().anyMatch(rule -> rule.test(request));
    }

    private static ProxyRequestAdapter.ProxyRequest withPath(ProxyRequestAdapter.ProxyRequest request, String path) {
        return new ProxyRequestAdapter.ProxyRequest(request.method(), path, request.body(), request.hostname(),
                request.queryParameters(), request.subject());
    }

    /**
     * Resolves "." and ".." segments in the given path. {@code unixSeparator} is forced to {@code true} so
     * behavior doesn't depend on the platform the server runs on.
     *
     * @return the canonical path, or empty if it attempts to traverse above its own root
     *         (e.g. "_cluster/../../_nodes").
     */
    static Optional<String> normalize(String path) {
        return Optional.ofNullable(FilenameUtils.normalize(path, true));
    }
}
