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
package org.graylog.plugins.views.search.export;

import javax.ws.rs.ext.MessageBodyWriter;
import java.lang.reflect.Type;

public abstract class SimpleMessageChunkWriter implements MessageBodyWriter<SimpleMessageChunk> {
    protected boolean typesMatch(Class<?> type, Type genericType) {
        return SimpleMessageChunk.class.equals(type) || isAutoValueType(type, genericType);
    }

    private boolean isAutoValueType(Class<?> type, Type genericType) {
        return AutoValue_SimpleMessageChunk.class.equals(type) && SimpleMessageChunk.class.equals(genericType);
    }
}
