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

import org.graylog2.indexer.datanode.ProxyRequestAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataNodeProxyAllowlistTest {

    // Mirrors the real "metrics" rule from DataNodeRestApiProxyResource: it's meant to permit read-only
    // access to cluster configuration metrics, nothing else.
    private static final DataNodeProxyAllowlist ALLOWLIST = new DataNodeProxyAllowlist(List.of(
            request -> request.path().startsWith("metrics")
    ));

    private static ProxyRequestAdapter.ProxyRequest requestFor(String path) {
        return new ProxyRequestAdapter.ProxyRequest("GET", path, InputStream.nullInputStream(), "myhost", null);
    }

    @Test
    void permitsExactAllowedPrefix() throws RequestNotAllowedException {
        assertThat(ALLOWLIST.authorize(requestFor("metrics"))).isNotNull();
    }

    @Test
    void rejectsPathOutsideAllowlist() {
        assertThatThrownBy(() -> ALLOWLIST.authorize(requestFor("management/stop")))
                .isInstanceOf(RequestNotAllowedException.class);
    }

    /**
     * Path traversal: "metrics/../management/stop" textually starts with "metrics", but a plain
     * {@code startsWith("metrics")} check on the raw path would incorrectly permit it even though it
     * actually resolves to "management/stop" — a destructive endpoint (it stops the data node, see
     * DatanodeRestApiProxyVoidResponseTest.ManagementClient) that was never meant to be reachable here.
     * Normalizing the path before matching closes this: the rule is checked against "management/stop",
     * not the raw string, so it correctly fails to match and the request is rejected.
     */
    @Test
    void pathTraversalIsNormalizedBeforeMatchingAndRejected() {
        assertThatThrownBy(() -> ALLOWLIST.authorize(requestFor("metrics/../management/stop")))
                .isInstanceOf(RequestNotAllowedException.class);
    }

    @Test
    void normalizedPathIsForwardedWhenAllowed() throws RequestNotAllowedException {
        final var authorized = ALLOWLIST.authorize(requestFor("metrics/./health"));
        assertThat(authorized.path()).isEqualTo("metrics/health");
    }

    @ParameterizedTest
    @ValueSource(strings = {"..", "../metrics", "metrics/../..", "metrics/../../metrics"})
    void rejectsPathsThatTraverseAboveRoot(String path) {
        assertThat(DataNodeProxyAllowlist.normalize(path)).isEmpty();
    }

    @Test
    void normalizeResolvesDotSegmentsWithoutTraversingAboveRoot() {
        assertThat(DataNodeProxyAllowlist.normalize("metrics/../management/stop")).contains("management/stop");
        assertThat(DataNodeProxyAllowlist.normalize("metrics/./health")).contains("metrics/health");
        assertThat(DataNodeProxyAllowlist.normalize("metrics")).contains("metrics");
    }
}
