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
package org.graylog2.telemetry.suppliers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TypeFormatter {
    private static final List<String> PACKAGE_PREFIXES = List.of(
            "org.graylog2.outputs",
            "org.graylog2.inputs",
            "org.graylog.plugins",
            "org.graylog.aws.inputs",
            "org.graylog.enterprise.integrations"
    );

    /**
     * Matches a lowercase letter or digit followed by an uppercase letter.
     * Example: "BeatsInput" -> "beats_input"
     */
    private static final Pattern LOWER_OR_DIGIT_BEFORE_UPPER = Pattern.compile("([a-z\\d])([A-Z])");

    /**
     * Matches multiple uppercase letters followed by an uppercase letter and a lowercase letter.
     * Example: "GELFHttpInput" -> "gelf_http_input"
     */
    private static final Pattern ACRONYM_BEFORE_LETTER = Pattern.compile("([A-Z]+)([A-Z][a-z])");

    /**
     * Matches a letter followed by a digit.
     * Example: "Beats2" -> "beats_2"
     */
    private static final Pattern LETTER_BEFORE_DIGIT = Pattern.compile("([A-Za-z])(\\d)");

    private static final String REPLACEMENT = "$1_$2";

    private TypeFormatter() {
        //Nothing to see here, please move on!
    }

    /**
     * Formats fully qualified class names from known packages into snake_case.
     * Examples:
     * - org.graylog.plugins.beats.Beats2Input -> beats_2_input
     * - org.graylog2.inputs.gelf.http.GELFHttpInput -> gelf_http_input
     * - org.graylog2.inputs.beats.kafka.BeatsKafkaInput -> beats_kafka_input
     */
    public static String format(String type) {
        for (String prefix : PACKAGE_PREFIXES) {
            if (type.startsWith(prefix + ".")) {
                String result = type.substring(type.lastIndexOf('.') + 1);
                result = LOWER_OR_DIGIT_BEFORE_UPPER.matcher(result).replaceAll(REPLACEMENT);
                result = ACRONYM_BEFORE_LETTER.matcher(result).replaceAll(REPLACEMENT);
                result = LETTER_BEFORE_DIGIT.matcher(result).replaceAll(REPLACEMENT);
                return result.toLowerCase(Locale.ENGLISH);
            }
        }
        return type;
    }

    /**
     * Formats all keys in the given {@code metrics}-map.
     * Internally, {@link #format(String)} is being called for each entry.
     * In case a key already exists after formatting, the associated value will be summed.
     *
     * @param metrics A {@code Map<String, Long>} where the keys will be formatted using {@link #format(String)}.
     * @return A new map containing formatted keys and possibly summed values.
     */
    public static Map<String, Long> formatAll(Map<String, Long> metrics) {
        return metrics.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        e -> TypeFormatter.format(e.getKey()),
                        Map.Entry::getValue,
                        Long::sum,
                        LinkedHashMap::new
                ));
    }

}
