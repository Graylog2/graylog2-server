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
package org.graylog2.utilities.lucene;

import com.google.common.collect.Multimap;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryOperator;
import org.graylog2.search.SearchQueryOperators;
import org.graylog2.search.SearchQueryParser;

import java.util.Locale;
import java.util.Map;

/**
 * Builder for converting parsed search queries into Lucene Query objects.
 */
public class LuceneQueryBuilder {

    /**
     * Converts a multimap of field values into a Lucene Query.
     *
     * @param query the parsed query from SearchQueryParser
     * @return a Lucene Query representing the search conditions
     */
    public Query toLuceneQuery(SearchQuery query) {

        if (query == null || query.getQueryMap() == null || query.getQueryMap().isEmpty()) {
            return new MatchAllDocsQuery();
        }
        final Multimap<String, SearchQueryParser.FieldValue> queryMap = query.getQueryMap();

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        boolean hasAnyClause = false;

        for (Map.Entry<String, SearchQueryParser.FieldValue> entry : queryMap.entries()) {
            String fieldName = entry.getKey();
            SearchQueryParser.FieldValue fieldValue = entry.getValue();
            Object value = fieldValue.getValue();
            SearchQueryOperator operator = fieldValue.getOperator();
            boolean negate = fieldValue.isNegate();

            Query fieldQuery = buildFieldQuery(fieldName, value, operator);
            if (fieldQuery != null) {
                BooleanClause.Occur occur = negate ? BooleanClause.Occur.MUST_NOT : BooleanClause.Occur.SHOULD;
                builder.add(fieldQuery, occur);
                hasAnyClause = true;
            }
        }

        return hasAnyClause ? builder.build() : new MatchAllDocsQuery();
    }

    /**
     * Builds a Lucene Query for a specific field and value with the given operator.
     * <p>
     * String values are lowercased to provide case-insensitive matching, consistent with
     * StandardAnalyzer's behavior during indexing and MongoDB's REGEXP operator behavior.
     *
     * @param fieldName the field name to query
     * @param value the value to search for
     * @param operator the search operator (REGEXP, EQUALS, GREATER, etc.)
     * @return a Lucene Query for the field condition, or null if value is null
     */
    private Query buildFieldQuery(String fieldName, Object value, SearchQueryOperator operator) {
        if (value == null) {
            return null;
        }

        // Lowercase string values to match StandardAnalyzer's behavior (case-insensitive search)
        // This matches MongoDB's REGEXP operator which uses Pattern.CASE_INSENSITIVE
        String stringValue = value.toString().toLowerCase(Locale.ROOT);

        if (operator == SearchQueryOperators.REGEXP) {
            // Check if user provided any explicit wildcards
            boolean hasWildcards = stringValue.contains("*") || stringValue.contains("?");

            // Convert wildcards to regex pattern
            String pattern = stringValue.replace("*", ".*").replace("?", ".");

            // Add .* for substring matching only if user didn't provide any wildcards
            // If they use wildcards, assume they know what pattern they want
            if (!hasWildcards) {
                pattern = ".*" + pattern + ".*";
            }

            return new RegexpQuery(new Term(fieldName, pattern));
        } else if (operator == SearchQueryOperators.EQUALS) {
            return new TermQuery(new Term(fieldName, stringValue));
        } else if (operator == SearchQueryOperators.GREATER) {
            return TermRangeQuery.newStringRange(fieldName, stringValue, null, false, false);
        } else if (operator == SearchQueryOperators.GREATER_EQUALS) {
            return TermRangeQuery.newStringRange(fieldName, stringValue, null, true, false);
        } else if (operator == SearchQueryOperators.LESS) {
            return TermRangeQuery.newStringRange(fieldName, null, stringValue, false, false);
        } else if (operator == SearchQueryOperators.LESS_EQUALS) {
            return TermRangeQuery.newStringRange(fieldName, null, stringValue, false, true);
        }

        // Default to term query for unknown operators
        return new TermQuery(new Term(fieldName, stringValue));
    }
}
