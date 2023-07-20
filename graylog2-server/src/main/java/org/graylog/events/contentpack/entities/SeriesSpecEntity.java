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
package org.graylog.events.contentpack.entities;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;

import java.io.IOException;
import java.util.stream.StreamSupport;

@JsonDeserialize(using = SeriesSpecEntity.Deserializer.class)
public class SeriesSpecEntity {
    public static class Deserializer extends JsonDeserializer<SeriesSpecEntity> {
        private static final String legacyTypeField = "function";

        @Override
        public SeriesSpecEntity deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            final ObjectNode root = p.getCodec().readTree(p);
            final ObjectNode rootCopy = root.deepCopy();
            final Iterable<String> iterable = rootCopy::fieldNames;
            var fields = StreamSupport.stream(iterable.spliterator(), false).toList();

            if (!fields.contains(SeriesSpec.TYPE_FIELD) && fields.contains(legacyTypeField)) {
                var function = rootCopy.get(legacyTypeField);
                rootCopy.set(SeriesSpec.TYPE_FIELD, function);
                rootCopy.remove(legacyTypeField);
                return new SeriesSpecEntity(ctxt.readTreeAsValue(rootCopy, SeriesSpec.class));
            }

            return new SeriesSpecEntity(ctxt.readTreeAsValue(root, SeriesSpec.class));
        }
    }

    private final SeriesSpec seriesSpec;

    private SeriesSpecEntity(SeriesSpec seriesSpec) {
        this.seriesSpec = seriesSpec;
    }

    public static SeriesSpecEntity fromNativeEntity(SeriesSpec seriesSpec) {
        return new SeriesSpecEntity(seriesSpec);
    }

    @JsonValue
    public SeriesSpec toNativeEntity() {
        return this.seriesSpec;
    }
}
