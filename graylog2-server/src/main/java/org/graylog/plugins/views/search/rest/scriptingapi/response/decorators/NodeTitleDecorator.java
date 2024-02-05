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

import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.scriptingapi.request.RequestedField;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.plugin.Message;

import javax.annotation.Nullable;

import jakarta.inject.Inject;


public class NodeTitleDecorator implements FieldDecorator {


    private final NodeService nodeService;

    @Inject
    public NodeTitleDecorator(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @Override
    public boolean accept(RequestedField field) {
        return Message.FIELD_GL2_SOURCE_NODE.equals(field.name()) && acceptsDecorator(field.decorator());
    }

    private boolean acceptsDecorator(@Nullable String decorator) {
        return decorator == null || decorator.equals("title");
    }

    @Override
    public Object decorate(RequestedField field, Object value, SearchUser searchUser) {
        try {
            final Node node = nodeService.byNodeId(value.toString());
            return node.getTitle();
        } catch (NodeNotFoundException e) {
            return value;
        }
    }
}
