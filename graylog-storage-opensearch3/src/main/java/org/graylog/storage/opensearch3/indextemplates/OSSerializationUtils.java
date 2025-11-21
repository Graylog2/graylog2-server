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
package org.graylog.storage.opensearch3.indextemplates;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.json.stream.JsonParser;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.json.JsonpDeserializer;
import org.opensearch.client.json.PlainJsonSerializable;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;

import java.io.StringReader;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class that helps use our APIs based on maps with OS3, strongly typed, builder-based APIs.
 * It has its disadvantages, but simplifies significantly mapping between nested maps and complex `org.opensearch.client.opensearch._types.*` classes.
 */
@Singleton
public class OSSerializationUtils {

    private final JacksonJsonpMapper jsonpMapper;

    @Inject
    public OSSerializationUtils() {
        this.jsonpMapper = new JacksonJsonpMapper();
    }

    public Map<String, Object> toMap(final PlainJsonSerializable openSearchSerializableObject) throws JsonProcessingException {
        return this.jsonpMapper.objectMapper().readValue(openSearchSerializableObject.toJsonString(), new TypeReference<>() {});
    }

    public Map<String, JsonData> toJsonDataMap(final Map<String, Object> map) {
        return map.entrySet()
                .stream()
                .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> JsonData.of(entry.getValue())
                        )
                );
    }

    public <T> T fromMap(final Map<String, Object> mapRepresentation,
                         final JsonpDeserializer<T> deserializer) throws JsonProcessingException {
        final String json = this.jsonpMapper.objectMapper().writeValueAsString(mapRepresentation);
        return fromJson(json, deserializer);
    }

    public <T> T fromJson(final String json, final JsonpDeserializer<T> deserializer) {
        final JsonParser parser = jsonpMapper.jsonProvider().createParser(new StringReader(json));
        return deserializer.deserialize(parser, jsonpMapper);
    }
}
