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
package org.graylog.storage.opensearch3;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.opensearch.client.opensearch.generic.Request;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IndexManagementAdapterOSTest {
    private final OfficialOpensearchClient client = mock(OfficialOpensearchClient.class);
    private final IndexManagementAdapterOS adapter = new IndexManagementAdapterOS(client);

    @Test
    void sendsMethodEndpointParametersAndBodyThroughGraylogsClient() {
        when(client.performRequest(any(Request.class), any())).thenReturn(JsonNodeFactory.instance.objectNode().put("ok", true));

        assertThat(adapter.request("POST", "/_cluster/allocation/explain", Map.of("pretty", "false"),
                "{\"index\":\"graylog_20\",\"shard\":0,\"primary\":true}", "Couldn't explain").path("ok").asBoolean()).isTrue();

        final ArgumentCaptor<Request> request = ArgumentCaptor.forClass(Request.class);
        verify(client).performRequest(request.capture(), eq("Couldn't explain"));
        assertThat(request.getValue().getMethod()).isEqualTo("POST");
        assertThat(request.getValue().getEndpoint()).isEqualTo("/_cluster/allocation/explain");
        assertThat(request.getValue().getParameters()).isEqualTo(Map.of("pretty", "false"));
        assertThat(request.getValue().getBody()).isPresent();
    }

    @Test
    void sendsNoBodyWhenThereIsNone() {
        adapter.request("GET", "/_cat/indices", Map.of(), null, "Couldn't list indices");

        final ArgumentCaptor<Request> request = ArgumentCaptor.forClass(Request.class);
        verify(client).performRequest(request.capture(), eq("Couldn't list indices"));
        assertThat(request.getValue().getBody()).isEmpty();
    }
}
