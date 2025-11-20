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
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.json.stream.JsonParser;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.opensearch.client.json.JsonpDeserializer;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.json.PlainJsonSerializable;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch._types.mapping.Property;

import java.io.StringReader;
import java.util.Map;

/**
 * Utility class that helps use our APIs based on maps with OS3, strongly typed, builder-based APIs.
 * It has its disadvantages, but simplifies significantly mapping between nested maps and complex `org.opensearch.client.opensearch._types.*` classes.
 */
public class OSSerializationUtils {

    private final ObjectMapper objectMapper;
    private final JsonpMapper jsonpMapper;
    private final OfficialOpensearchClient client;

    @Inject
    public OSSerializationUtils(final ObjectMapper objectMapper,
                                final OfficialOpensearchClient client) {
        this.objectMapper = objectMapper;
        this.client = client;
        this.jsonpMapper = new JacksonJsonpMapper(objectMapper);
    }

    public Map<String, Object> toMap(final PlainJsonSerializable openSearchSerializableObject) throws JsonProcessingException {
        return objectMapper.readValue(openSearchSerializableObject.toJsonString(), new TypeReference<>() {});
    }

    public <T> T fromMap(final Map<String, Object> mapRepresentation,
                         final JsonpDeserializer<T> deserializer) throws JsonProcessingException {
        final String json = objectMapper.writeValueAsString(mapRepresentation);
        final JsonpMapper mapper = client.sync()._transport().jsonpMapper();
        final JsonParser parser = mapper.jsonProvider().createParser(new StringReader(json));
        return deserializer.deserialize(parser, mapper);
    }

    /**
     * This is an ugly hack because there is no way to dynamically resolve opensearch property type in os3 client
     *
     */
    public Property propertyOfType(String type) {
        final JsonParser parser = jsonpMapper.jsonProvider().createParser(new StringReader("{\"type\": \"" + type + "\"}"));
        return Property._DESERIALIZER.deserialize(parser, jsonpMapper);
    }
}
