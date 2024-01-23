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
package org.graylog.plugins.pipelineprocessor.functions.strings;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.inject.TypeLiteral;
import java.util.Collections;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderFunctionGroup;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.bool;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;

public class KeyValue extends AbstractFunction<Map<String, String>> {

    public static final String NAME = "key_value";
    public static final String TAKE_FIRST = "take_first";
    public static final String TAKE_LAST = "take_last";
    public static final String ARRAY = "array";
    private final ParameterDescriptor<String, String> valueParam;
    private final ParameterDescriptor<String, CharMatcher> splitParam;
    private final ParameterDescriptor<String, CharMatcher> valueSplitParam;
    private final ParameterDescriptor<Boolean, Boolean> ignoreEmptyValuesParam;
    private final ParameterDescriptor<Boolean, Boolean> allowDupeKeysParam;
    private final ParameterDescriptor<String, String> duplicateHandlingParam;
    private final ParameterDescriptor<String, CharMatcher> trimCharactersParam;
    private final ParameterDescriptor<String, CharMatcher> trimValueCharactersParam;

    public KeyValue() {
        valueParam = string("value").ruleBuilderVariable().description("The string to extract key/value pairs from").build();
        splitParam = string("delimiters", CharMatcher.class).transform(CharMatcher::anyOf).optional().description("The characters used to separate pairs, defaults to whitespace").build();
        valueSplitParam = string("kv_delimiters", CharMatcher.class).transform(CharMatcher::anyOf).optional().description("The characters used to separate keys from values, defaults to '='").build();

        ignoreEmptyValuesParam = bool("ignore_empty_values").optional().description("Whether to ignore keys with empty values, defaults to true").defaultValue(Optional.of(true)).build();
        allowDupeKeysParam = bool("allow_dup_keys").optional().description("Whether to allow duplicate keys, defaults to true").defaultValue(Optional.of(true)).build();
        duplicateHandlingParam = string("handle_dup_keys").optional().defaultValue(Optional.of(TAKE_FIRST)).description("How to handle duplicate keys: (default) 'take_first': only use first value, 'take_last': only take last value, 'array': gather them as a string list or use a delimiter e.g. ','").build();
        trimCharactersParam = string("trim_key_chars", CharMatcher.class)
                .transform(CharMatcher::anyOf)
                .optional()
                .description("The characters to trim from keys, default is not to trim")
                .build();
        trimValueCharactersParam = string("trim_value_chars", CharMatcher.class)
                .transform(CharMatcher::anyOf)
                .optional()
                .description("The characters to trim from values, default is not to trim")
                .build();
    }

    @Override
    public Map<String, String> evaluate(FunctionArgs args, EvaluationContext context) {
        final String value = valueParam.required(args, context);
        if (Strings.isNullOrEmpty(value)) {
            return Collections.emptyMap();
        }
        final CharMatcher kvPairsMatcher = splitParam.optional(args, context).orElse(CharMatcher.whitespace());
        final CharMatcher kvDelimMatcher = valueSplitParam.optional(args, context).orElse(CharMatcher.anyOf("="));

        Splitter outerSplitter = Splitter.on(DelimiterCharMatcher.withQuoteHandling(kvPairsMatcher))
                .omitEmptyStrings()
                .trimResults();

        final Splitter entrySplitter = Splitter.on(kvDelimMatcher)
                .omitEmptyStrings()
                .limit(2)
                .trimResults();
        return new MapSplitter(outerSplitter,
                entrySplitter,
                ignoreEmptyValuesParam.optional(args, context).orElse(true),
                trimCharactersParam.optional(args, context).orElse(CharMatcher.none()),
                trimValueCharactersParam.optional(args, context).orElse(CharMatcher.none()),
                allowDupeKeysParam.optional(args, context).orElse(true),
                duplicateHandlingParam.optional(args, context).orElse(TAKE_FIRST))
                .split(value);
    }

    @Override
    public FunctionDescriptor<Map<String, String>> descriptor() {
        //noinspection unchecked
        return FunctionDescriptor.<Map<String, String>>builder()
                .name(NAME)
                .returnType((Class<? extends Map<String, String>>) new TypeLiteral<Map<String, String>>() {}.getRawType())
                .params(valueParam,
                        splitParam,
                        valueSplitParam,
                        ignoreEmptyValuesParam,
                        allowDupeKeysParam,
                        duplicateHandlingParam,
                        trimCharactersParam,
                        trimValueCharactersParam
                )
                .description("Extracts key/value pairs from a string")
                .ruleBuilderEnabled()
                .ruleBuilderName("Convert key/value to map")
                .ruleBuilderTitle("Convert key/value string '${value}' to map")
                .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.CONVERSION)
                .build();
    }

    private static class DelimiterCharMatcher extends CharMatcher {
        private final char wrapperChar;

        private boolean inWrapper = false;

        /**
         * An implementation that doesn't split when the given delimiter char matcher appears in double or single quotes.
         *
         * @param charMatcher the char matcher
         * @return a char matcher that can handle double and single quotes
         */
        static CharMatcher withQuoteHandling(CharMatcher charMatcher) {
            return new DelimiterCharMatcher('"')
                    .and(new DelimiterCharMatcher('\''))
                    .and(charMatcher);
        }

        private DelimiterCharMatcher(char wrapperChar) {
            this.wrapperChar = wrapperChar;
        }

        @Override
        public boolean matches(char c) {
            if (wrapperChar == c) {
                inWrapper = !inWrapper;
            }
            return !inWrapper;
        }
    }

    private static class MapSplitter {

        private final Splitter outerSplitter;
        private final Splitter entrySplitter;
        private final boolean ignoreEmptyValues;
        private final CharMatcher keyTrimMatcher;
        private final CharMatcher valueTrimMatcher;
        private final Boolean allowDupeKeys;
        private final String duplicateHandling;

        MapSplitter(Splitter outerSplitter,
                    Splitter entrySplitter,
                    boolean ignoreEmptyValues,
                    CharMatcher keyTrimMatcher,
                    CharMatcher valueTrimMatcher,
                    Boolean allowDupeKeys,
                    String duplicateHandling) {
            this.outerSplitter = outerSplitter;
            this.entrySplitter = entrySplitter;
            this.ignoreEmptyValues = ignoreEmptyValues;
            this.keyTrimMatcher = keyTrimMatcher;
            this.valueTrimMatcher = valueTrimMatcher;
            this.allowDupeKeys = allowDupeKeys;
            this.duplicateHandling = duplicateHandling;
        }

        public Map<String, String> split(CharSequence sequence) {
            final Map<String, String> map = new LinkedHashMap<>();

            for (String entry : outerSplitter.split(sequence)) {
                Iterator<String> entryFields = entrySplitter.split(entry).iterator();
                if (!entryFields.hasNext()) {
                    continue;
                }

                String key = processKey(entryFields.next());
                if (entryFields.hasNext()) {
                    if (map.containsKey(key)) {
                        handleDuplicateKey(map, entryFields, key);
                    } else {
                        handleNewKey(entryFields, map, key);
                    }
                } else if (!ignoreEmptyValues) {
                    throw new IllegalArgumentException("Missing value for key " + key);
                }
            }
            return map;
        }

        private void handleDuplicateKey(Map<String, String> map, Iterator<String> entryFields, String key) {
            if (!allowDupeKeys) {
                throw new IllegalArgumentException("Duplicate key " + key + " is not allowed in key_value function.");
            }
            switch (Strings.nullToEmpty(duplicateHandling).toLowerCase(Locale.ENGLISH)) {
                case TAKE_FIRST:
                    // ignore this value
                    break;
                case TAKE_LAST:
                    // simply reset the entry
                    map.put(key, processValue(entryFields.next()));
                    break;
                case ARRAY:
                    concatArrayValues(map, key, processValue(entryFields.next()));
                    break;
                default:
                    concatDelimiter(map, key, processValue(entryFields.next()));
            }
        }

        private String processKey(String key) {
            return keyTrimMatcher.trimFrom(key);
        }

        private String processValue(String value) {
            return valueTrimMatcher.trimFrom(value);
        }

        private void handleNewKey(Iterator<String> entryFields, Map<String, String> map, String key) {
            String value = processValue(entryFields.next());
            map.put(key, value);
        }

        private void concatArrayValues(Map<String, String> map, String key, String value) {
            String[] array = map.get(key).split(",");
            value = valueTrimMatcher.trimFrom(value);

            StringBuilder result = new StringBuilder();
            result.append("[");
            for (String element : array) {
                result.append("\"").append(element).append("\",");
            }
            result.append("\"").append(value).append("\"]");
            map.put(key, result.toString());
        }

        private void concatDelimiter(Map<String, String> map, String key, String value) {
            String concatenatedValue = map.get(key) + duplicateHandling + value;
            map.put(key, concatenatedValue);
        }
    }
}
