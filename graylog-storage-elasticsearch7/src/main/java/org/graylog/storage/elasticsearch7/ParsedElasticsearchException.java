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
package org.graylog.storage.elasticsearch7;

import com.google.auto.value.AutoValue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AutoValue
abstract class ParsedElasticsearchException {
    private static final Pattern exceptionPattern = Pattern
            .compile("(ElasticsearchException\\[)?Elasticsearch exception \\[type=(?<type>[\\w_]+), (?:reason=(?<reason>.+?)(\\]+;|\\]$))");

    abstract String type();
    abstract String reason();

    static ParsedElasticsearchException create(String type, String reason) {
        return new AutoValue_ParsedElasticsearchException(type, reason);
    }

    static ParsedElasticsearchException from(String s) {
        final Matcher matcher = exceptionPattern.matcher(s);
        if (matcher.find()) {
            final String type = matcher.group("type");
            final String reason = matcher.group("reason");

            return create(type, reason);
        }

        throw new IllegalArgumentException("Unable to parse Elasticsearch exception: " + s);
    }
}
