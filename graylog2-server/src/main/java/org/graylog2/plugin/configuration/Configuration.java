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
package org.graylog2.plugin.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Strings.isNullOrEmpty;

public class Configuration implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
    public static final Configuration EMPTY_CONFIGURATION = new Configuration(null);

    @JsonProperty
    private final Map<String, Object> source;
    @JsonIgnore
    private final Map<String, String> strings = Maps.newHashMap();
    @JsonIgnore
    private final Map<String, Integer> ints = Maps.newHashMap();
    @JsonIgnore
    private final Map<String, Boolean> bools = Maps.newHashMap();
    @JsonIgnore
    private final Map<String, List<String>> lists = Maps.newHashMap();

    @JsonCreator
    public Configuration(@JsonProperty("source") @Nullable Map<String, Object> m) {
        this.source = firstNonNull(m, Collections.<String, Object>emptyMap());

        for (Map.Entry<String, Object> e : this.source.entrySet()) {
            final String key = e.getKey();
            final Object value = e.getValue();

            if (value == null) {
                LOG.debug("NULL value in configuration key <{}>", key);
                continue;
            }

            try {
                if (value instanceof String) {
                    strings.put(key, (String) value);
                } else if (value instanceof Integer) {
                    ints.put(key, (Integer) value);
                } else if (value instanceof Long) {
                    ints.put(key, ((Long) value).intValue()); // We only support integers but MongoDB likes to return longs.
                } else if (value instanceof Double) {
                    ints.put(key, ((Double) value).intValue()); // same as for longs lol
                } else if (value instanceof Boolean) {
                    bools.put(key, (Boolean) value);
                } else if (value instanceof List) {
                    final List<String> list = ((List<?>) value).stream().map(element -> (String) element).collect(Collectors.toList());
                    lists.put(key, list);
                } else {
                    LOG.error("Cannot handle type [{}] of plugin configuration key <{}>.", value.getClass().getCanonicalName(), key);
                }

            } catch (Exception ex) {
                LOG.warn("Could not read input configuration key <" + key + ">. Skipping.", ex);
            }
        }
    }

    @Nullable
    public String getString(String key) {
        return strings.get(key);
    }

    public String getString(String key, String defaultValue) {
        return firstNonNull(strings.get(key), defaultValue);
    }

    public void setString(String key, String value) {
        strings.put(key, value);
    }

    public int getInt(String key) {
        return ints.get(key);
    }

    public int getInt(String key, int defaultValue) {
        return firstNonNull(ints.get(key), defaultValue);
    }

    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return firstNonNull(bools.get(key), defaultValue);
    }

    public boolean booleanIsSet(String key) {
        return bools.containsKey(key);
    }

    public void setBoolean(String key, boolean value) {
        bools.put(key, value);
    }

    public List<String> getList(String key) {
        return lists.get(key);
    }

    public List<String> getList(String key, List<String> defaultValue) {
        return firstNonNull(lists.get(key), defaultValue);
    }

    public boolean listIsSet(String key) {
        return lists.containsKey(key);
    }

    @Nullable
    public Map<String, Object> getSource() {
        return source;
    }

    public boolean stringIsSet(String key) {
        return !isNullOrEmpty(strings.get(key));
    }

    public boolean intIsSet(String key) {
        return ints.containsKey(key);
    }

    @Nullable
    public String serializeToJson() {
        try {
            return source.isEmpty() ? null : objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            LOG.error("Serializing configuration failed.", e);
            return null;
        }
    }

    public static Configuration deserializeFromJson(String json) {
        if (isNullOrEmpty(json)) {
            return EMPTY_CONFIGURATION;
        }

        try {
            return objectMapper.readValue(json, Configuration.class);
        } catch (IOException e) {
            LOG.error("Deserializing configuration failed.", e);
            return EMPTY_CONFIGURATION;
        }
    }
}
