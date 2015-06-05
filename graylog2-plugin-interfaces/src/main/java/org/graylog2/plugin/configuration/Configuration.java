/**
 * The MIT License
 * Copyright (c) 2012 Graylog, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.plugin.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class Configuration {
    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);
    private static final ObjectMapper objectMapper;
    public static final Configuration EMPTY_CONFIGURATION = new Configuration(null);

    static {
        objectMapper = new ObjectMapper();
        objectMapper
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
    }

    @JsonProperty
    private final Map<String, Object> source;

    @JsonIgnore
    private final Map<String, String> strings;
    @JsonIgnore
    private final Map<String, Integer> ints;
    @JsonIgnore
    private final Map<String, Boolean> bools;
    @JsonIgnore
    private String serializedSource = null;

    @JsonCreator
    public Configuration(@JsonProperty("source") Map<String, Object> m) {
        this.source = m;

        if (m != null && !m.isEmpty()) {
            try {
                this.serializedSource = objectMapper.writeValueAsString(this);
            } catch (JsonProcessingException e) {
                LOG.error("Serializing configuration failed.", e);
            }
        }

        strings = Maps.newHashMap();
        ints = Maps.newHashMap();
        bools = Maps.newHashMap();

        if (m != null) {
            for(Map.Entry<String, Object> e : m.entrySet()) {
                try {
                    if (e.getValue() == null) {
                        LOG.debug("NULL value in configuration key <{}>", e.getKey());
                        continue;
                    }

                    if (e.getValue() instanceof String) {
                        strings.put(e.getKey(), (String) e.getValue());
                    } else if (e.getValue() instanceof Integer) {
                        ints.put(e.getKey(), (Integer) e.getValue());
                    } else if (e.getValue() instanceof Long) {
                        ints.put(e.getKey(), ((Long) e.getValue()).intValue()); // We only support integers but MongoDB likes to return longs.
                    } else if (e.getValue() instanceof Double) {
                        ints.put(e.getKey(), ((Double) e.getValue()).intValue()); // same as for longs lol
                    } else if (e.getValue() instanceof Boolean) {
                        bools.put(e.getKey(), (Boolean) e.getValue());
                    } else {
                        LOG.error("Cannot handle type [{}] of plugin configuration key <{}>.", e.getValue().getClass().getCanonicalName(), e.getKey());
                    }

                } catch(Exception ex) {
                    LOG.warn("Could not read input configuration key <" + e.getKey() + ">. Skipping.", ex);
                }
            }
        }
    }

    public String getString(String key) {
        return strings.get(key);
    }

    public void setString(String key, String value) {
        strings.put(key, value);
    }

    public int getInt(String key) {
        return ints.get(key);
    }

    public boolean getBoolean(String key) {
        if (!bools.containsKey(key)) {
            return false;
        }

        return bools.get(key);
    }
    public boolean booleanIsSet(String key) {
        return bools.containsKey(key);
    }

    public void setBoolean(String key, boolean value) {
        bools.put(key, value);
    }

    public Map<String, Object> getSource() {
        return source;
    }

    public boolean stringIsSet(String key) {
        return strings.get(key) != null && !strings.get(key).isEmpty();
    }

    public boolean intIsSet(String key) {
        return ints.get(key) != null;
    }

    public String serializeToJson() {
        return serializedSource;
    }

    public static Configuration deserializeFromJson(String json) {
        if (Strings.isNullOrEmpty(json)) {
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
