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
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.json.Json;
import jakarta.json.JsonWriter;
import jakarta.json.stream.JsonParser;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.json.JsonpDeserializer;
import org.opensearch.client.json.PlainJsonSerializable;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.msearch.RequestItem;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
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

    public static Object valueNode(JsonNode jsonNode) {
        if (jsonNode.isInt()) {
            return jsonNode.asInt();
        } else if (jsonNode.isLong()) {
            return jsonNode.asLong();
        } else if (jsonNode.isIntegralNumber()) {
            return jsonNode.asLong();
        } else if (jsonNode.isFloatingPointNumber()) {
            return jsonNode.asDouble();
        } else if (jsonNode.isBoolean()) {
            return jsonNode.asBoolean();
        } else if (jsonNode.isNull()) {
            return null;
        } else {
            return jsonNode.asText();
        }
    }

    public Object toObject(JsonData jsonData) {
        try {
            return valueNode(jsonpMapper.objectMapper().readTree(toJson(jsonData)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> toMap(final JsonData openSearchSerializableObject) {
        String json = toJson(openSearchSerializableObject);
        return toMap(json);
    }

    private String toJson(final JsonData openSearchSerializableObject) {
        try (
                StringWriter writer = new StringWriter();
                JsonWriter jsonWriter = Json.createWriter(writer)
        ) {
            jsonWriter.write(openSearchSerializableObject.toJson());
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException("Error serializing json", e);
        }
    }

    public Map<String, Object> toMap(final PlainJsonSerializable openSearchSerializableObject) {
        return toMap(openSearchSerializableObject.toJsonString());
    }

    public Map<String, Object> toMap(final String json) {
        try {
            return this.jsonpMapper.objectMapper().readValue(json, new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException("Error serializing json", e);
        }
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

    public RequestItem toMsearch(SearchRequest request) {
        return RequestItem.of(req -> req
                .body(mbody -> mbody
                        .aggregations(request.aggregations())
                        .query(request.query())
                        .from(request.from())
                        .minScore(request.minScore())
                        .postFilter(request.postFilter())
                        .searchAfter(request.searchAfter())
                        .size(request.size())
                        .sort(request.sort())
                        .trackScores(request.trackScores())
                        .trackTotalHits(request.trackTotalHits())
                        .suggest(request.suggest())
                        .highlight(request.highlight())
                        .source(request.source())
                        .scriptFields(request.scriptFields())
                        .seqNoPrimaryTerm(request.seqNoPrimaryTerm())
                        .storedFields(request.storedFields())
                        .explain(request.explain())
                        .fields(request.fields())
                        .docvalueFields(request.docvalueFields())
                        .indicesBoost(request.indicesBoost())
                        .collapse(request.collapse())
                        .version(request.version())
                        .timeout(request.timeout())
                        .rescore(request.rescore())
                        .ext(request.ext())
                )
                .header(header -> header
                        .allowNoIndices(request.allowNoIndices())
                        .expandWildcards(request.expandWildcards())
                        .ignoreUnavailable(request.ignoreUnavailable())
                        .index(request.index())
                        .preference(request.preference())
                        .requestCache(request.requestCache())
                        .routing(request.routing())
                        .searchType(request.searchType())
                )
        );
    }
}
