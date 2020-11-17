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
package org.graylog.plugins.views.search.errors;

import org.graylog.plugins.views.search.Query;
import org.graylog2.indexer.ElasticsearchException;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchTypeErrorParser {
    public static SearchTypeError parse(Query query, String searchTypeId, ElasticsearchException ex) {
        final Integer resultWindowLimit = parseResultLimit(ex);

        if (resultWindowLimit != null)
            return new ResultWindowLimitError(query, searchTypeId, resultWindowLimit, ex);

        return new SearchTypeError(query, searchTypeId, ex);
    }

    private static Integer parseResultLimit(Throwable throwable) {
        return parseResultLimit(throwable.getMessage());
    }

    private static Integer parseResultLimit(String description) {
        if (description.toLowerCase(Locale.US).contains("result window is too large")) {
            final Matcher matcher = Pattern.compile("[0-9]+").matcher(description);
            if (matcher.find())
                return Integer.parseInt(matcher.group(0));
        }
        return null;
    }

}
