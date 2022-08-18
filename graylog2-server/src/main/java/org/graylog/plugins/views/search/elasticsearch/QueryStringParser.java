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
package org.graylog.plugins.views.search.elasticsearch;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.graylog.plugins.views.search.QueryMetadata;
import org.graylog.plugins.views.search.validation.SubstringMultilinePosition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class QueryStringParser {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$([a-zA-Z_]\\w*)\\$");

    public QueryMetadata parse(String queryString) {
        if (Strings.isNullOrEmpty(queryString)) {
            return QueryMetadata.empty();
        }

        Map<String, List<SubstringMultilinePosition>> positions = new LinkedHashMap<>();

        final String[] lines = queryString.split("\n");

        for (int line = 0; line < lines.length; line++) {
            final String currentLine = lines[line];
            final Matcher matcher = PLACEHOLDER_PATTERN.matcher(currentLine);
            while (matcher.find()) {
                final String name = matcher.group(1);
                if (!positions.containsKey(name)) {
                    positions.put(name, new ArrayList<>());
                }
                positions.get(name).add(SubstringMultilinePosition.create(line + 1, matcher.start(), matcher.end()));
            }
        }
        final ImmutableSet<QueryParam> params = positions.entrySet().stream()
                .map(entry -> QueryParam.create(entry.getKey(), entry.getValue()))
                .collect(ImmutableSet.toImmutableSet());

        return QueryMetadata.builder()
                .usedParameters(params)
                .build();
    }
}
