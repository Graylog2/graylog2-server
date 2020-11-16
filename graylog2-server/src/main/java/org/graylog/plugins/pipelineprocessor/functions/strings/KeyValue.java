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
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.bool;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;

public class KeyValue extends AbstractFunction<Map<String, String>> {

    public static final String NAME = "key_value";
    private final ParameterDescriptor<String, String> valueParam;
    private final ParameterDescriptor<String, CharMatcher> splitParam;
    private final ParameterDescriptor<String, CharMatcher> valueSplitParam;
    private final ParameterDescriptor<Boolean, Boolean> ignoreEmptyValuesParam;
    private final ParameterDescriptor<Boolean, Boolean> allowDupeKeysParam;
    private final ParameterDescriptor<String, String> duplicateHandlingParam;
    private final ParameterDescriptor<String, CharMatcher> trimCharactersParam;
    private final ParameterDescriptor<String, CharMatcher> trimValueCharactersParam;

    public KeyValue() {
        valueParam = string("value").description("The string to extract key/value pairs from").build();
        splitParam = string("delimiters", CharMatcher.class).transform(CharMatcher::anyOf).optional().description("The characters used to separate pairs, defaults to whitespace").build();
        valueSplitParam = string("kv_delimiters", CharMatcher.class).transform(CharMatcher::anyOf).optional().description("The characters used to separate keys from values, defaults to '='").build();

        ignoreEmptyValuesParam = bool("ignore_empty_values").optional().description("Whether to ignore keys with empty values, defaults to true").build();
        allowDupeKeysParam = bool("allow_dup_keys").optional().description("Whether to allow duplicate keys, defaults to true").build();
        duplicateHandlingParam = string("handle_dup_keys").optional().description("How to handle duplicate keys: 'take_first': only use first value, 'take_last': only take last value, default is to concatenate the values").build();
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
            return null;
        }
        final CharMatcher kvPairsMatcher = splitParam.optional(args, context).orElse(CharMatcher.whitespace());
        final CharMatcher kvDelimMatcher = valueSplitParam.optional(args, context).orElse(CharMatcher.anyOf("="));

        Splitter outerSplitter = Splitter.on(kvPairsMatcher)
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
                               duplicateHandlingParam.optional(args, context).orElse("take_first"))
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
                .build();
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
                boolean concat = false;
                Iterator<String> entryFields = entrySplitter.split(entry).iterator();

                if (!entryFields.hasNext()) {
                    continue;
                }
                String key = entryFields.next();
                key = keyTrimMatcher.trimFrom(key);
                if (map.containsKey(key)) {
                    if (!allowDupeKeys) {
                        throw new IllegalArgumentException("Duplicate key " + key + " is not allowed in key_value function.");
                    }
                    switch (Strings.nullToEmpty(duplicateHandling).toLowerCase(Locale.ENGLISH)) {
                        case "take_first":
                            // ignore this value
                            continue;
                        case "take_last":
                            // simply reset the entry
                            break;
                        default:
                            concat = true;
                    }
                }

                if (entryFields.hasNext()) {
                    String value = entryFields.next();
                    value = valueTrimMatcher.trimFrom(value);
                    // already have a value, concating old+delim+new
                    if (concat) {
                        value = map.get(key) + duplicateHandling + value;
                    }
                    map.put(key, value);
                } else if (!ignoreEmptyValues) {
                    throw new IllegalArgumentException("Missing value for key " + key);
                }

            }
            return Collections.unmodifiableMap(map);
        }
    }
}
