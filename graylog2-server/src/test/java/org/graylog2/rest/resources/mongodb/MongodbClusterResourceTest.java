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
package org.graylog2.rest.resources.mongodb;

import jakarta.ws.rs.core.Response;
import org.bson.Document;
import org.graylog2.cluster.nodes.mongodb.MongodbClusterCommand;
import org.graylog2.cluster.nodes.mongodb.MongodbNodesProvider;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MongodbClusterResourceTest {

    @Test
    void profilingStatus_reportsSlowMs_whenAllNodesAgree() {
        final MongodbClusterResource resource = resourceWithNodeResults(
                new Document("was", 1).append("slowms", 100),
                new Document("was", 1).append("slowms", 100)
        );

        final Response response = resource.profilingStatus();

        assertThat(entity(response)).containsEntry("slowMs", 100);
    }

    @Test
    void profilingStatus_omitsSlowMs_whenNodesDisagree() {
        // slowms is per-node mongod state, not replicated cluster config -- nodes can genuinely disagree (e.g. a
        // manually tuned mongod.conf on one member). Reporting an arbitrary single node's value would misrepresent
        // the others, so the field is omitted entirely rather than guessing.
        final MongodbClusterResource resource = resourceWithNodeResults(
                new Document("was", 1).append("slowms", 100),
                new Document("was", 1).append("slowms", 250)
        );

        final Response response = resource.profilingStatus();

        assertThat(entity(response)).doesNotContainKey("slowMs");
    }

    @Test
    void profilingStatus_omitsSlowMs_whenNoNodeReportsIt() {
        final MongodbClusterResource resource = resourceWithNodeResults(new Document("was", 0));

        final Response response = resource.profilingStatus();

        assertThat(entity(response)).doesNotContainKey("slowMs");
    }

    private static MongodbClusterResource resourceWithNodeResults(Document... nodeDocuments) {
        final Map<String, Document> resultsByHost = new LinkedHashMap<>();
        for (int i = 0; i < nodeDocuments.length; i++) {
            resultsByHost.put("host-" + i, nodeDocuments[i]);
        }

        final MongodbClusterCommand clusterCommand = mock(MongodbClusterCommand.class);
        when(clusterCommand.runOnEachNode(any(Document.class))).thenReturn(resultsByHost);

        return new MongodbClusterResource(mock(MongodbNodesProvider.class), clusterCommand);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> entity(Response response) {
        return (Map<String, Object>) response.getEntity();
    }
}
