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
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryOperator;
import org.graylog2.search.SearchQueryOperators;
import org.graylog2.search.SearchQueryParser;

import java.util.Locale;
import java.util.Map;

/**
 * Builder for converting parsed search queries into Lucene Query objects.
 */
public class LuceneQueryBuilder {

    private final Map<String, SearchQueryField.Type> fieldTypes;

    public LuceneQueryBuilder(Map<String, SearchQueryField.Type> fieldTypes) {
        this.fieldTypes = fieldTypes;
    }

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

        // Get field type, default to STRING if not found
        SearchQueryField.Type fieldType = fieldTypes.getOrDefault(fieldName, SearchQueryField.Type.STRING);

        // Lowercase string values to match StandardAnalyzer's behavior (case-insensitive search)
        // This matches MongoDB's REGEXP operator which uses Pattern.CASE_INSENSITIVE
        String stringValue = value.toString().toLowerCase(Locale.ROOT);

        if (operator == SearchQueryOperators.REGEXP) {
            // REGEXP only works on string fields
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
            // For numeric types, use point queries for exact match
            return buildNumericOrTermQuery(fieldName, stringValue, fieldType);
        } else if (operator == SearchQueryOperators.GREATER) {
            return buildRangeQuery(fieldName, stringValue, null, false, false, fieldType);
        } else if (operator == SearchQueryOperators.GREATER_EQUALS) {
            return buildRangeQuery(fieldName, stringValue, null, true, false, fieldType);
        } else if (operator == SearchQueryOperators.LESS) {
            return buildRangeQuery(fieldName, null, stringValue, false, false, fieldType);
        } else if (operator == SearchQueryOperators.LESS_EQUALS) {
            return buildRangeQuery(fieldName, null, stringValue, false, true, fieldType);
        }

        // Default to term query for unknown operators
        return new TermQuery(new Term(fieldName, stringValue));
    }

    private Query buildNumericOrTermQuery(String fieldName, String stringValue, SearchQueryField.Type fieldType) {
        return switch (fieldType) {
            case INT, BOOLEAN -> IntPoint.newExactQuery(fieldName, Integer.parseInt(stringValue));
            case LONG, DATE -> LongPoint.newExactQuery(fieldName, Long.parseLong(stringValue));
            case DOUBLE -> DoublePoint.newExactQuery(fieldName, Double.parseDouble(stringValue));
            default -> new TermQuery(new Term(fieldName, stringValue));
        };
    }

    private Query buildRangeQuery(String fieldName, String lowerValue, String upperValue,
                                   boolean lowerInclusive, boolean upperInclusive,
                                   SearchQueryField.Type fieldType) {
        return switch (fieldType) {
            case INT, BOOLEAN -> {
                int lower = lowerValue != null ? Integer.parseInt(lowerValue) : Integer.MIN_VALUE;
                int upper = upperValue != null ? Integer.parseInt(upperValue) : Integer.MAX_VALUE;
                // Adjust bounds for exclusive ranges
                if (lowerValue != null && !lowerInclusive) {
                    lower = Math.addExact(lower, 1);
                }
                if (upperValue != null && !upperInclusive) {
                    upper = Math.addExact(upper, -1);
                }
                yield IntPoint.newRangeQuery(fieldName, lower, upper);
            }
            case LONG, DATE -> {
                long lower = lowerValue != null ? Long.parseLong(lowerValue) : Long.MIN_VALUE;
                long upper = upperValue != null ? Long.parseLong(upperValue) : Long.MAX_VALUE;
                // Adjust bounds for exclusive ranges
                if (lowerValue != null && !lowerInclusive) {
                    lower = Math.addExact(lower, 1);
                }
                if (upperValue != null && !upperInclusive) {
                    upper = Math.addExact(upper, -1);
                }
                yield LongPoint.newRangeQuery(fieldName, lower, upper);
            }
            case DOUBLE -> {
                double lower = lowerValue != null ? Double.parseDouble(lowerValue) : Double.NEGATIVE_INFINITY;
                double upper = upperValue != null ? Double.parseDouble(upperValue) : Double.POSITIVE_INFINITY;
                // For doubles, use nextUp/nextDown for exclusive ranges
                if (lowerValue != null && !lowerInclusive) {
                    lower = Math.nextUp(lower);
                }
                if (upperValue != null && !upperInclusive) {
                    upper = Math.nextDown(upper);
                }
                yield DoublePoint.newRangeQuery(fieldName, lower, upper);
            }
            default -> TermRangeQuery.newStringRange(fieldName, lowerValue, upperValue, lowerInclusive, upperInclusive);
        };
    }
}
