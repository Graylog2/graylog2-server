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

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryStringParser {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$([a-zA-Z_]\\w*)\\$");

    public QueryMetadata parse(String queryString) {
        if (Strings.isNullOrEmpty(queryString)) {
            return QueryMetadata.empty();
        }
        final Matcher matcher = PLACEHOLDER_PATTERN.matcher(queryString);

        Set<String> paramNames = Sets.newHashSet();
        Set<QueryParam> params = Sets.newHashSet();

        while (matcher.find()) {
            final String name = matcher.group(1);
            if (!paramNames.contains(name)) { // skip already processed params
                paramNames.add(name);
                final List<SubstringMultilinePosition> paramPositions = SubstringMultilinePosition.compute(queryString, "$" + name + "$");
                params.add(QueryParam.create(name, paramPositions));
            }
        }

        return QueryMetadata.builder()
                .usedParameters(ImmutableSet.copyOf(params))
                .build();
    }
}
