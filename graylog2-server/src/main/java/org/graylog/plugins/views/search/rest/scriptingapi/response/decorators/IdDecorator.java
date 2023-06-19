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

import com.google.common.collect.Sets;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.scriptingapi.request.RequestedField;
import org.graylog2.plugin.Message;

import java.util.Set;

/**
 * This decorator is needed to support field.id format for messages and aggregations. The ID is the actual
 * content of the fields below, but since we default to outputting name, we need some logic that will
 * return the raw IDs if requested.
 */
public class IdDecorator implements FieldDecorator {

    private final Set<String> ID_FIELDS = Sets.newHashSet(
            Message.FIELD_STREAMS,
            Message.FIELD_GL2_SOURCE_NODE,
            Message.FIELD_GL2_SOURCE_INPUT
    );

    @Override
    public boolean accept(RequestedField field) {
        return ID_FIELDS.contains(field.name()) && field.hasDecorator("id");
    }

    @Override
    public Object decorate(RequestedField field, Object value, SearchUser searchUser) {
        // ID is the default value, we don't need to map anything, just forward it directly
        return value;
    }
}
