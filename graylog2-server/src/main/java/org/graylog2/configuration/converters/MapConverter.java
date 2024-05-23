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
package org.graylog2.configuration.converters;

import com.github.joschi.jadconfig.Converter;
import com.github.joschi.jadconfig.ParameterException;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.containsWhitespace;

/**
 * A collection of {@link Converter} classes for map config values. (e.g., "setting = a:1,b:2,c:3")
 */
public class MapConverter {
    private static final Splitter ENTRY_SPLITTER = Splitter.on(Pattern.compile("\\s*,\\s*")).trimResults().omitEmptyStrings();
    private static final Splitter VALUE_SPLITTER = Splitter.on(Pattern.compile(":")).trimResults().omitEmptyStrings().limit(2);

    private static <T> Map<String, T> convertFromValue(String value, Function<String, T> valueConverter) {
        try {
            return ENTRY_SPLITTER.splitToStream(value)
                    .map(sequence -> {
                        final var kv = VALUE_SPLITTER.splitToList(sequence);
                        if (kv.size() != 2) {
                            throw new ParameterException("Invalid map entry argument - missing value: " + sequence);
                        }
                        if (containsWhitespace(kv.get(0))) {
                            throw new ParameterException("Invalid map entry argument - key cannot contain spaces: " + sequence);
                        }
                        try {
                            return Maps.immutableEntry(kv.get(0), valueConverter.apply(kv.get(1)));
                        } catch (Exception e) {
                            throw new ParameterException("Invalid map entry value: " + sequence, e);
                        }
                    })
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (v1, v2) -> {throw new IllegalStateException("Duplicate key in value: " + value);},
                            LinkedHashMap::new // We are using a linked hash map to keep the order of the keys
                    ));
        } catch (ParameterException e) {
            throw e;
        } catch (Exception e) {
            throw new ParameterException("Invalid value - " + e.getMessage(), e);
        }
    }

    private static <T> String convertToValue(Map<String, T> value) {
        return value.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining(","));
    }

    /**
     * A {@link Converter} for {@code Map<String, String>} values. (e.g., "test:a,hello:world")
     */
    public static class StringString implements Converter<Map<String, String>> {
        @Override
        public Map<String, String> convertFrom(String value) {
            return convertFromValue(value, Function.identity());
        }

        @Override
        public String convertTo(Map<String, String> value) {
            return convertToValue(value);
        }
    }

    /**
     * A {@link Converter} for {@code Map<String, Integer>} values. (e.g., "test:1,hello:2")
     */
    public static class StringInteger implements Converter<Map<String, Integer>> {
        @Override
        public Map<String, Integer> convertFrom(String value) {
            return convertFromValue(value, Integer::parseInt);
        }

        @Override
        public String convertTo(Map<String, Integer> value) {
            return convertToValue(value);
        }
    }
}
