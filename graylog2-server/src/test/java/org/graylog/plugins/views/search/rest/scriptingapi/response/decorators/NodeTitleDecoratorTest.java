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
package org.graylog.plugins.views.search.rest.scriptingapi.response.decorators;

import org.assertj.core.api.Assertions;
import org.graylog.plugins.views.search.rest.TestSearchUser;
import org.graylog.plugins.views.search.rest.scriptingapi.request.RequestedField;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.plugin.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NodeTitleDecoratorTest {

    private FieldDecorator decorator;

    @BeforeEach
    void setUp() throws NodeNotFoundException {
        decorator = new NodeTitleDecorator(mockNodeService("5ca1ab1e-0000-4000-a000-000000000000", "5ca1ab1e", "my-host.example.com"));
    }

    private NodeService mockNodeService(String nodeId, String shortId, String hostname) throws NodeNotFoundException {
        final Node node = Mockito.mock(Node.class);
        Mockito.when(node.getShortNodeId()).thenReturn(shortId);
        Mockito.when(node.getHostname()).thenReturn(hostname);
        final NodeService service = Mockito.mock(NodeService.class);
        Mockito.when(service.byNodeId(nodeId)).thenReturn(node);
        return service;
    }

    @Test
    void accept() {
        Assertions.assertThat(decorator.accept(RequestedField.parse(Message.FIELD_GL2_SOURCE_NODE))).isTrue();
        Assertions.assertThat(decorator.accept(RequestedField.parse(Message.FIELD_STREAMS))).isFalse();
    }

    @Test
    void decorate() {
        final Object decorated = decorator.decorate(RequestedField.parse(Message.FIELD_GL2_SOURCE_NODE),
                "5ca1ab1e-0000-4000-a000-000000000000",
                TestSearchUser.builder().build());

        Assertions.assertThat(decorated).isEqualTo("5ca1ab1e / my-host.example.com");
    }
}
