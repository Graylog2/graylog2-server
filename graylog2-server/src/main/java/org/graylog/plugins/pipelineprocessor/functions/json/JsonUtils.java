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
package org.graylog.plugins.pipelineprocessor.functions.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.jackson.TypeReferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

public class JsonUtils {
    private static final Logger LOG = LoggerFactory.getLogger(JsonUtils.class);
    private static final String KEY_SEPARATOR = "_";
    private static final String KEY_VALUE_SEPARATOR = ":";
    private static final String LIST_SEPARATOR = ",";
    private static final RemoveNullPredicate REMOVE_NULL_PREDICATE = new RemoveNullPredicate();

    public static JsonNode extractJson(
            String value, ObjectMapper mapper, boolean flattenObjects, boolean escapeArrays, boolean deleteArrays)
            throws IOException {
        if (isNullOrEmpty(value)) {
            throw new IOException("null result");
        }
        final Map<String, Object> json = mapper.readValue(value, TypeReferences.MAP_STRING_OBJECT);

        ObjectNode resultRoot = mapper.createObjectNode();
        for (Map.Entry<String, Object> mapEntry : json.entrySet()) {
            for (Entry entry : parseValue(mapEntry.getKey(), mapEntry.getValue(), mapper, flattenObjects, escapeArrays, deleteArrays)) {
                resultRoot.put(entry.key(), entry.value().toString());
            }
        }
        return resultRoot;
    }

//    public static String extractJson(
//            String value, ObjectMapper mapper, boolean flattenObjects, boolean escapeArrays, boolean deleteArrays)
//            throws IOException {
//        if (isNullOrEmpty(value)) {
//            throw new IOException("null result");
//        }
//        final Map<String, Object> json = mapper.readValue(value, TypeReferences.MAP_STRING_OBJECT);
//
//        final Map<String, Object> results = new TreeMap<>();
//        for (Map.Entry<String, Object> mapEntry : json.entrySet()) {
//            for (Entry entry : parseValue(mapEntry.getKey(), mapEntry.getValue(), mapper, flattenObjects, escapeArrays, deleteArrays)) {
//                results.put(entry.key(), entry.value());
//            }
//        }
//        final Joiner.MapJoiner joiner = Joiner.on(LIST_SEPARATOR).withKeyValueSeparator(KEY_VALUE_SEPARATOR);
//        return "{" + joiner.join(results) + "}";
//    }

    private static Collection<Entry> parseValue(
            String key, Object value, ObjectMapper mapper, boolean flattenObjects, boolean escapeArrays, boolean deleteArrays)
            throws JsonProcessingException {
        if (value instanceof Boolean || value instanceof Number || value instanceof String) {
            return Collections.singleton(Entry.create(key, value));
        } else if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) value;
            final Map<String, Object> withoutNull = Maps.filterEntries(map, REMOVE_NULL_PREDICATE);
            if (flattenObjects) {
                final List<Entry> result = new ArrayList<>(map.size());
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    result.addAll(parseValue(key + KEY_SEPARATOR + entry.getKey(), entry.getValue(),
                            mapper, flattenObjects, escapeArrays, deleteArrays));
                }
                return result;
            } else {
                final Joiner.MapJoiner joiner = Joiner.on(LIST_SEPARATOR).withKeyValueSeparator(KEY_VALUE_SEPARATOR);
                return Collections.singleton(Entry.create(key, joiner.join(withoutNull)));
            }
        } else if (value instanceof List) {
            if (deleteArrays) {
                // ignore all arrays
                return Collections.emptySet();
            } else if (escapeArrays) {
                // serialize, so it can be re-parsed as valid JSON
                return Collections.singleton(Entry.create(key, mapper.writeValueAsString(value)));
            } else {
                // flatten array using indices for unique keys
                int listSize = ((List<Object>) value).size();
                final List<Entry> result = new ArrayList<>((listSize));
                int index = 0;
                for (Object obj : (List<Object>) value) {
                    result.addAll(parseValue(key + KEY_SEPARATOR + index, obj,
                            mapper, flattenObjects, escapeArrays, deleteArrays));
                    index++;
                }
                return result;
            }
        } else if (value == null) {
            // Ignore null values, so we don't try to create fields for that in the message.
            return Collections.emptySet();
        } else {
            LOG.debug("Unknown type \"{}\" in key \"{}\"", value.getClass(), key);
            return Collections.emptySet();
        }
    }

    public static JsonNode deleteBelow(JsonNode root, long maxDepth) {
        if (maxDepth > 0) {
            deleteBelow(root, maxDepth, 1);
        }
        return root;
    }

    private static void deleteBelow(JsonNode root, long maxDepth, long depth) {
        final Iterator<JsonNode> elements = root.elements();
        while (elements.hasNext()) {
            final JsonNode node = elements.next();
            if (node.isContainerNode()) {
                if (depth >= maxDepth) {
                    elements.remove();
                } else {
                    deleteBelow(node, maxDepth, depth + 1);
                }
            }
        }
    }

    @AutoValue
    @WithBeanGetter
    protected abstract static class Entry {
        public abstract String key();

        @Nullable
        public abstract Object value();

        public static Entry create(String key, @Nullable Object value) {
            return new AutoValue_JsonUtils_Entry(key, value);
        }
    }

    protected final static class RemoveNullPredicate implements Predicate<Map.Entry> {
        @Override
        public boolean apply(@Nullable Map.Entry input) {
            return input != null && input.getKey() != null && input.getValue() != null;
        }
    }
}
