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
import org.graylog2.cluster.NodeService;
import org.graylog2.cluster.TestNodeService;
import org.graylog2.plugin.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

class NodeTitleDecoratorTest {

    private FieldDecorator decorator;

    @BeforeEach
    void setUp() {
        final NodeService nodeService = new TestNodeService();
        nodeService.registerServer("5ca1ab1e-0000-4000-a000-000000000000", false, URI.create("http://my-host.example.com"), "my-host.example.com");

        decorator = new NodeTitleDecorator(nodeService);
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

    @Test
    void testUnknownNode() {
        final Object decorated = decorator.decorate(RequestedField.parse(Message.FIELD_GL2_SOURCE_NODE),
                "2e7e1436-9ca4-43e3-b857-c75e61dea424",
                TestSearchUser.builder().build());

        Assertions.assertThat(decorated).isEqualTo("2e7e1436-9ca4-43e3-b857-c75e61dea424");
    }
}
