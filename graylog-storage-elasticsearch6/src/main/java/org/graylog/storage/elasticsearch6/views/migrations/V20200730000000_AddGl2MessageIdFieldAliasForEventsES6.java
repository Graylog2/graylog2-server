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
package org.graylog.storage.elasticsearch6.views.migrations;

import org.graylog.plugins.views.migrations.V20200730000000_AddGl2MessageIdFieldAliasForEvents;

import java.util.Set;

public class V20200730000000_AddGl2MessageIdFieldAliasForEventsES6 implements V20200730000000_AddGl2MessageIdFieldAliasForEvents.ElasticsearchAdapter {
    @Override
    public void addGl2MessageIdFieldAlias(Set<String> indexPrefixes) {
        throw new IllegalStateException("Field aliases are not supported for all minor versions of ES6. This should never be called.");
    }
}
