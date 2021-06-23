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
package org.graylog2.indexer;

import com.google.common.collect.ImmutableMap;

import static org.graylog2.plugin.Message.FIELD_GL2_MESSAGE_ID;

public class EventsIndexMapping7 extends EventsIndexMapping {
    @Override
    protected ImmutableMap<String, Object> fieldProperties() {
        return map()
                .putAll(super.fieldProperties())
                .put(FIELD_GL2_MESSAGE_ID, map()
                        .put("type", "alias")
                        .put("path", "id")
                        .build())
                .build();
    }

    @Override
    protected String dateFormat() {
        return ConstantsES7.ES_DATE_FORMAT;
    }
}
