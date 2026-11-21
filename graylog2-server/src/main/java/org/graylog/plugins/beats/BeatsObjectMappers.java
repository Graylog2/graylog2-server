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
package org.graylog.plugins.beats;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;

final class BeatsObjectMappers {
    private BeatsObjectMappers() {
    }

    /**
     * Returns a copy of the given mapper without Jackson's limit on the length of JSON strings (20 million characters
     * by default). The size of a Beats event is already limited by {@link BeatsFrameDecoder}, and its frames are
     * acknowledged before the codec parses them. A lower limit in the codec would drop events the sender considers
     * delivered.
     */
    static ObjectMapper withoutStringLengthLimit(ObjectMapper objectMapper) {
        final JsonFactory jsonFactory = objectMapper.getFactory().copy();
        jsonFactory.setStreamReadConstraints(jsonFactory.streamReadConstraints().rebuild()
                .maxStringLength(Integer.MAX_VALUE)
                .build());
        return objectMapper.copyWith(jsonFactory);
    }
}
