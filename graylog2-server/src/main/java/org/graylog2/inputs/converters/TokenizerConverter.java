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
package org.graylog2.inputs.converters;

import org.graylog2.plugin.inputs.Converter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Strings.isNullOrEmpty;

public class TokenizerConverter extends Converter {
    // ┻━┻ ︵ ¯\(ツ)/¯ ︵ ┻━┻
    private static final Pattern PATTERN = Pattern.compile("(?:^|\\s)(?:([\\w-]+)\\s?=\\s?((?:\"[^\"]+\")|(?:'[^']+')|(?:[\\S]+)))");

    public TokenizerConverter(Map<String, Object> config) {
        super(Type.TOKENIZER, config);
    }

    @Override
    public Object convert(String value) {
        if (isNullOrEmpty(value)) {
            return value;
        }

        if (value.contains("=")) {
            final Map<String, String> fields = new HashMap<>();

            Matcher m = PATTERN.matcher(value);
            while (m.find()) {
                if (m.groupCount() != 2) {
                    continue;
                }

                fields.put(removeQuotes(m.group(1)), removeQuotes(m.group(2)));
            }

            return fields;
        } else {
            return Collections.emptyMap();
        }
    }

    private String removeQuotes(String s) {
        final boolean doubleQuotes = s.startsWith("\"") && s.endsWith("\"");
        final boolean singleQuotes = s.startsWith("'") && s.endsWith("'");
        if (doubleQuotes || singleQuotes) {
            return s.substring(1, s.length() - 1);
        } else {
            return s;
        }
    }

    @Override
    public boolean buildsMultipleFields() {
        return true;
    }
}
