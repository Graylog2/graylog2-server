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
    private static final RemoveNullPredicate<Map.Entry<String, Object>> REMOVE_NULL_PREDICATE = new RemoveNullPredicate<>();

    private JsonUtils() {
    }

    public static JsonNode extractJson(
            String value, ObjectMapper mapper, ExtractFlags extractFlags)
            throws IOException {
        if (isNullOrEmpty(value)) {
            throw new IOException("null result");
        }
        final Map<String, Object> json = mapper.readValue(value, TypeReferences.MAP_STRING_OBJECT);

        ObjectNode resultRoot = mapper.createObjectNode();
        for (Map.Entry<String, Object> mapEntry : json.entrySet()) {
            for (Entry entry : parseValue(mapEntry.getKey(), mapEntry.getValue(), mapper, extractFlags)) {
                resultRoot.put(entry.key(), entry.value().toString());
            }
        }
        return resultRoot;
    }

    private static Collection<Entry> parseValue(
            String key, Object value, ObjectMapper mapper, ExtractFlags extractFlags)
            throws JsonProcessingException {
        if (value instanceof Boolean || value instanceof Number || value instanceof String) {
            return Collections.singleton(Entry.create(key, value));
        } else if (value instanceof Map) {
            return parseObject(key, (Map<String, Object>) value, mapper, extractFlags);
        } else if (value instanceof List) {
            return parseArray(key, (List<Object>) value, mapper, extractFlags);
        } else if (value == null) {
            // Ignore null values, so we don't try to create fields for that in the message.
            return Collections.emptySet();
        } else {
            LOG.debug("Unknown type \"{}\" in key \"{}\"", value.getClass(), key);
            return Collections.emptySet();
        }
    }

    private static Collection<Entry> parseObject(
            String key, Map<String, Object> value, ObjectMapper mapper, ExtractFlags extractFlags)
            throws JsonProcessingException {
        final Map<String, Object> mapWithoutNull = Maps.filterEntries(value, REMOVE_NULL_PREDICATE);
        if (extractFlags.flattenObjects()) {
            final List<Entry> result = new ArrayList<>(mapWithoutNull.size());
            for (Map.Entry<String, Object> entry : mapWithoutNull.entrySet()) {
                result.addAll(parseValue(key + KEY_SEPARATOR + entry.getKey(), entry.getValue(),
                        mapper, extractFlags));
            }
            return result;
        } else {
            final Joiner.MapJoiner joiner = Joiner.on(LIST_SEPARATOR).withKeyValueSeparator(KEY_VALUE_SEPARATOR);
            return Collections.singleton(Entry.create(key, joiner.join(mapWithoutNull)));
        }
    }

    private static Collection<Entry> parseArray(
            String key, List<Object> value, ObjectMapper mapper, ExtractFlags extractFlags)
            throws JsonProcessingException {
        if (extractFlags.deleteArrays()) {
            // ignore all arrays
            return Collections.emptySet();
        } else if (extractFlags.escapeArrays()) {
            // serialize, so it can be re-parsed as valid JSON
            return Collections.singleton(Entry.create(key, mapper.writeValueAsString(value)));
        } else {
            // flatten array using indices for unique keys
            int listSize = value.size();
            final List<Entry> result = new ArrayList<>((listSize));
            int index = 0;
            for (Object obj : value) {
                result.addAll(parseValue(key + KEY_SEPARATOR + index, obj,
                        mapper, extractFlags));
                index++;
            }
            return result;
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

    protected static final class RemoveNullPredicate<T extends Map.Entry> implements Predicate<T> {
        @Override
        public boolean apply(@Nullable Map.Entry input) {
            return input != null && input.getKey() != null && input.getValue() != null;
        }
    }

    @AutoValue
    protected abstract static class ExtractFlags {
        public abstract boolean flattenObjects();
        public abstract boolean escapeArrays();
        public abstract boolean deleteArrays();
        public static Builder builder() {
            return new AutoValue_JsonUtils_ExtractFlags.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder flattenObjects(boolean flattenObjects);
            public abstract Builder escapeArrays(boolean escapeArrays);
            public abstract Builder deleteArrays(boolean deleteArrays);
            public abstract JsonUtils.ExtractFlags build();
        }
    }
}
