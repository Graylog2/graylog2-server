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

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class TypeFormatter {
    private static final List<String> PACKAGE_PREFIXES = List.of(
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

    /**
     * Formats fully qualified class names from known packages into snake_case.
     * Examples:
     *  - org.graylog.plugins.beats.Beats2Input -> beats_2_input
     *  - org.graylog2.inputs.gelf.http.GELFHttpInput -> gelf_http_input
     *  - org.graylog2.inputs.beats.kafka.BeatsKafkaInput -> beats_kafka_input
     */
    public static String format(String type) {
        for (String prefix : PACKAGE_PREFIXES) {
            if (type.startsWith(prefix + ".")) {
                String result = type.substring(type.lastIndexOf('.') + 1);
                result = LOWER_OR_DIGIT_BEFORE_UPPER.matcher(result).replaceAll("$1_$2");
                result = ACRONYM_BEFORE_LETTER.matcher(result).replaceAll("$1_$2");
                result = LETTER_BEFORE_DIGIT.matcher(result).replaceAll("$1_$2");
                return result.toLowerCase(Locale.ENGLISH);
            }
        }
        return type;
    }
}
