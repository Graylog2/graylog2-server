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

import com.google.common.collect.ImmutableSet;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PointRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LuceneQueryBuilderTest {

    private LuceneQueryBuilder builder;
    private SearchQueryParser parser;

    @BeforeEach
    void setUp() {
        builder = new LuceneQueryBuilder(Map.of(
                "name", SearchQueryField.Type.STRING,
                "age", SearchQueryField.Type.INT,
                "status", SearchQueryField.Type.STRING,
                "role", SearchQueryField.Type.STRING
        ));
        parser = new SearchQueryParser("defaultfield", ImmutableSet.of("name", "age", "status", "role"));
    }

    @Test
    void testNullQueryMap() {
        Query query = builder.toLuceneQuery(null);
        assertThat(query).isInstanceOf(MatchAllDocsQuery.class);
    }

    @Test
    void testEmptyQueryMap() {
        SearchQuery searchQuery = parser.parse("");
        Query query = builder.toLuceneQuery(searchQuery);
        assertThat(query).isInstanceOf(MatchAllDocsQuery.class);
    }

    @Test
    void testSimpleTermQuery() {
        SearchQuery searchQuery = parser.parse("name:john");
        Query query = builder.toLuceneQuery(searchQuery);

        assertThat(query).isInstanceOf(BooleanQuery.class);
        BooleanQuery booleanQuery = (BooleanQuery) query;
        assertThat(booleanQuery.clauses()).hasSize(1);

        BooleanClause clause = booleanQuery.clauses().getFirst();
        assertThat(clause.getOccur()).isEqualTo(BooleanClause.Occur.SHOULD);
        assertThat(clause.getQuery()).isInstanceOf(RegexpQuery.class);

        RegexpQuery regexpQuery = (RegexpQuery) clause.getQuery();
        assertThat(regexpQuery.getRegexp().field()).isEqualTo("name");
        assertThat(regexpQuery.getRegexp().text()).isEqualTo(".*john.*");
    }

    @Test
    void testExactMatchWithEqualsOperator() {
        SearchQuery searchQuery = parser.parse("name:=john");
        Query query = builder.toLuceneQuery(searchQuery);

        assertThat(query).isInstanceOf(BooleanQuery.class);
        BooleanQuery booleanQuery = (BooleanQuery) query;
        assertThat(booleanQuery.clauses()).hasSize(1);

        BooleanClause clause = booleanQuery.clauses().getFirst();
        assertThat(clause.getOccur()).isEqualTo(BooleanClause.Occur.SHOULD);
        assertThat(clause.getQuery()).isInstanceOf(TermQuery.class);

        TermQuery termQuery = (TermQuery) clause.getQuery();
        assertThat(termQuery.getTerm().field()).isEqualTo("name");
        assertThat(termQuery.getTerm().text()).isEqualTo("john");
    }

    @Test
    void testRegexpQuery() {
        SearchQuery searchQuery = parser.parse("name:jo*n");
        Query query = builder.toLuceneQuery(searchQuery);

        assertThat(query).isInstanceOf(BooleanQuery.class);
        BooleanQuery booleanQuery = (BooleanQuery) query;
        assertThat(booleanQuery.clauses()).hasSize(1);

        BooleanClause clause = booleanQuery.clauses().getFirst();
        assertThat(clause.getQuery()).isInstanceOf(RegexpQuery.class);

        RegexpQuery regexpQuery = (RegexpQuery) clause.getQuery();
        assertThat(regexpQuery.getRegexp().field()).isEqualTo("name");
        // When user provides wildcards, just convert them without adding substring matching
        assertThat(regexpQuery.getRegexp().text()).isEqualTo("jo.*n");
    }

    @Test
    void testRegexpQueryWithQuestionMark() {
        SearchQuery searchQuery = parser.parse("name:jo?n");
        Query query = builder.toLuceneQuery(searchQuery);

        assertThat(query).isInstanceOf(BooleanQuery.class);
        BooleanQuery booleanQuery = (BooleanQuery) query;
        BooleanClause clause = booleanQuery.clauses().getFirst();
        assertThat(clause.getQuery()).isInstanceOf(RegexpQuery.class);

        RegexpQuery regexpQuery = (RegexpQuery) clause.getQuery();
        // Wildcards in input are converted: ? -> . (no additional .* wrapping when wildcards present)
        assertThat(regexpQuery.getRegexp().text()).isEqualTo("jo.n");
    }

    @Test
    void testRegexpQueryWithPrefixWildcard() {
        SearchQuery searchQuery = parser.parse("name:*john");
        Query query = builder.toLuceneQuery(searchQuery);

        assertThat(query).isInstanceOf(BooleanQuery.class);
        BooleanQuery booleanQuery = (BooleanQuery) query;
        BooleanClause clause = booleanQuery.clauses().getFirst();
        assertThat(clause.getQuery()).isInstanceOf(RegexpQuery.class);

        RegexpQuery regexpQuery = (RegexpQuery) clause.getQuery();
        // User provided wildcard, so no additional wrapping
        assertThat(regexpQuery.getRegexp().text()).isEqualTo(".*john");
    }

    @Test
    void testRegexpQueryWithSuffixWildcard() {
        SearchQuery searchQuery = parser.parse("name:john*");
        Query query = builder.toLuceneQuery(searchQuery);

        assertThat(query).isInstanceOf(BooleanQuery.class);
        BooleanQuery booleanQuery = (BooleanQuery) query;
        BooleanClause clause = booleanQuery.clauses().getFirst();
        assertThat(clause.getQuery()).isInstanceOf(RegexpQuery.class);

        RegexpQuery regexpQuery = (RegexpQuery) clause.getQuery();
        // User provided wildcard, so no additional wrapping
        assertThat(regexpQuery.getRegexp().text()).isEqualTo("john.*");
    }

    @Test
    void testRegexpQueryWithSubstringMatching() {
        // Default operator is REGEXP, should wrap with .* for substring matching
        SearchQuery searchQuery = parser.parse("name:local");
        Query query = builder.toLuceneQuery(searchQuery);

        assertThat(query).isInstanceOf(BooleanQuery.class);
        BooleanQuery booleanQuery = (BooleanQuery) query;
        BooleanClause clause = booleanQuery.clauses().getFirst();
        assertThat(clause.getQuery()).isInstanceOf(RegexpQuery.class);

        RegexpQuery regexpQuery = (RegexpQuery) clause.getQuery();
        // Should be wrapped with .* for substring matching: "local" matches "localhost"
        assertThat(regexpQuery.getRegexp().text()).isEqualTo(".*local.*");
    }

    @Test
    void testGreaterThanQuery() {
        SearchQuery searchQuery = parser.parse("age:>30");
        Query query = builder.toLuceneQuery(searchQuery);

        assertThat(query).isInstanceOf(BooleanQuery.class);
        BooleanQuery booleanQuery = (BooleanQuery) query;
        BooleanClause clause = booleanQuery.clauses().getFirst();
        assertThat(clause.getQuery()).isInstanceOf(PointRangeQuery.class);

        PointRangeQuery rangeQuery = (PointRangeQuery) clause.getQuery();
        assertThat(rangeQuery.getField()).isEqualTo("age");
        // For age:>30, the range should be [31, MAX_VALUE] (exclusive converted to inclusive by adding 1)
        assertThat(rangeQuery.toString()).contains("age:[31 TO 2147483647]");
    }

    @Test
    void testGreaterThanOrEqualsQuery() {
        SearchQuery searchQuery = parser.parse("age:>=30");
        Query query = builder.toLuceneQuery(searchQuery);

        assertThat(query).isInstanceOf(BooleanQuery.class);
        BooleanQuery booleanQuery = (BooleanQuery) query;
        BooleanClause clause = booleanQuery.clauses().getFirst();
        assertThat(clause.getQuery()).isInstanceOf(PointRangeQuery.class);

        PointRangeQuery rangeQuery = (PointRangeQuery) clause.getQuery();
        assertThat(rangeQuery.getField()).isEqualTo("age");
        // For age:>=30, the range should be [30, MAX_VALUE]
        assertThat(rangeQuery.toString()).contains("age:[30 TO 2147483647]");
    }

    @Test
    void testLessThanQuery() {
        SearchQuery searchQuery = parser.parse("age:<30");
        Query query = builder.toLuceneQuery(searchQuery);

        assertThat(query).isInstanceOf(BooleanQuery.class);
        BooleanQuery booleanQuery = (BooleanQuery) query;
        BooleanClause clause = booleanQuery.clauses().getFirst();
        assertThat(clause.getQuery()).isInstanceOf(PointRangeQuery.class);

        PointRangeQuery rangeQuery = (PointRangeQuery) clause.getQuery();
        assertThat(rangeQuery.getField()).isEqualTo("age");
        // For age:<30, the range should be [MIN_VALUE, 29] (exclusive converted to inclusive by subtracting 1)
        assertThat(rangeQuery.toString()).contains("age:[-2147483648 TO 29]");
    }

    @Test
    void testLessThanOrEqualsQuery() {
        SearchQuery searchQuery = parser.parse("age:<=30");
        Query query = builder.toLuceneQuery(searchQuery);

        assertThat(query).isInstanceOf(BooleanQuery.class);
        BooleanQuery booleanQuery = (BooleanQuery) query;
        BooleanClause clause = booleanQuery.clauses().getFirst();
        assertThat(clause.getQuery()).isInstanceOf(PointRangeQuery.class);

        PointRangeQuery rangeQuery = (PointRangeQuery) clause.getQuery();
        assertThat(rangeQuery.getField()).isEqualTo("age");
        // For age:<=30, the range should be [MIN_VALUE, 30]
        assertThat(rangeQuery.toString()).contains("age:[-2147483648 TO 30]");
    }

    @Test
    void testNegatedQuery() {
        SearchQuery searchQuery = parser.parse("-name:john");
        Query query = builder.toLuceneQuery(searchQuery);

        assertThat(query).isInstanceOf(BooleanQuery.class);
        BooleanQuery booleanQuery = (BooleanQuery) query;
        assertThat(booleanQuery.clauses()).hasSize(1);

        BooleanClause clause = booleanQuery.clauses().get(0);
        assertThat(clause.getOccur()).isEqualTo(BooleanClause.Occur.MUST_NOT);
    }

    @Test
    void testMultipleFields() {
        SearchQuery searchQuery = parser.parse("name:john age:>30");
        Query query = builder.toLuceneQuery(searchQuery);

        assertThat(query).isInstanceOf(BooleanQuery.class);
        BooleanQuery booleanQuery = (BooleanQuery) query;
        assertThat(booleanQuery.clauses()).hasSize(2);

        assertThat(booleanQuery.clauses().get(0).getOccur()).isEqualTo(BooleanClause.Occur.SHOULD);
        assertThat(booleanQuery.clauses().get(1).getOccur()).isEqualTo(BooleanClause.Occur.SHOULD);

        // First clause should be RegexpQuery for name
        assertThat(booleanQuery.clauses().get(0).getQuery()).isInstanceOf(RegexpQuery.class);
        // Second clause should be PointRangeQuery for age (numeric field)
        assertThat(booleanQuery.clauses().get(1).getQuery()).isInstanceOf(PointRangeQuery.class);
    }

    @Test
    void testMultipleValuesForSameField() {
        SearchQuery searchQuery = parser.parse("name:john,jane");
        Query query = builder.toLuceneQuery(searchQuery);

        assertThat(query).isInstanceOf(BooleanQuery.class);
        BooleanQuery booleanQuery = (BooleanQuery) query;
        assertThat(booleanQuery.clauses()).hasSize(2);

        // Default operator is REGEXP, so both should be RegexpQuery with substring matching
        assertThat(booleanQuery.clauses().get(0).getQuery()).isInstanceOf(RegexpQuery.class);
        assertThat(booleanQuery.clauses().get(1).getQuery()).isInstanceOf(RegexpQuery.class);
    }

    @Test
    void testMixedPositiveAndNegativeQueries() {
        SearchQuery searchQuery = parser.parse("name:john -status:inactive");
        Query query = builder.toLuceneQuery(searchQuery);

        assertThat(query).isInstanceOf(BooleanQuery.class);
        BooleanQuery booleanQuery = (BooleanQuery) query;
        assertThat(booleanQuery.clauses()).hasSize(2);

        assertThat(booleanQuery.clauses().get(0).getOccur()).isEqualTo(BooleanClause.Occur.SHOULD);
        assertThat(booleanQuery.clauses().get(1).getOccur()).isEqualTo(BooleanClause.Occur.MUST_NOT);

        // Both should be RegexpQuery (default operator)
        assertThat(booleanQuery.clauses().get(0).getQuery()).isInstanceOf(RegexpQuery.class);
        assertThat(booleanQuery.clauses().get(1).getQuery()).isInstanceOf(RegexpQuery.class);
    }

    @Test
    void testDefaultField() {
        SearchQuery searchQuery = parser.parse("john");
        Query query = builder.toLuceneQuery(searchQuery);

        assertThat(query).isInstanceOf(BooleanQuery.class);
        BooleanQuery booleanQuery = (BooleanQuery) query;
        assertThat(booleanQuery.clauses()).hasSize(1);

        BooleanClause clause = booleanQuery.clauses().get(0);
        // Default operator is REGEXP, so it should use RegexpQuery with substring matching
        assertThat(clause.getQuery()).isInstanceOf(RegexpQuery.class);

        RegexpQuery regexpQuery = (RegexpQuery) clause.getQuery();
        assertThat(regexpQuery.getRegexp().field()).isEqualTo("defaultfield");
        assertThat(regexpQuery.getRegexp().text()).isEqualTo(".*john.*");
    }

    @Test
    void testCaseInsensitiveRegexpQuery() {
        // Verify that uppercase input is lowercased for case-insensitive matching
        SearchQuery searchQuery = parser.parse("name:John");
        Query query = builder.toLuceneQuery(searchQuery);

        assertThat(query).isInstanceOf(BooleanQuery.class);
        BooleanQuery booleanQuery = (BooleanQuery) query;
        assertThat(booleanQuery.clauses()).hasSize(1);

        BooleanClause clause = booleanQuery.clauses().getFirst();
        assertThat(clause.getQuery()).isInstanceOf(RegexpQuery.class);

        RegexpQuery regexpQuery = (RegexpQuery) clause.getQuery();
        assertThat(regexpQuery.getRegexp().field()).isEqualTo("name");
        // Input "John" should be lowercased to "john" for case-insensitive matching
        assertThat(regexpQuery.getRegexp().text()).isEqualTo(".*john.*");
    }

    @Test
    void testCaseInsensitiveExactMatch() {
        // Verify that uppercase input is lowercased even for exact match (EQUALS operator)
        SearchQuery searchQuery = parser.parse("name:=JOHN");
        Query query = builder.toLuceneQuery(searchQuery);

        assertThat(query).isInstanceOf(BooleanQuery.class);
        BooleanQuery booleanQuery = (BooleanQuery) query;
        assertThat(booleanQuery.clauses()).hasSize(1);

        BooleanClause clause = booleanQuery.clauses().getFirst();
        assertThat(clause.getQuery()).isInstanceOf(TermQuery.class);

        TermQuery termQuery = (TermQuery) clause.getQuery();
        assertThat(termQuery.getTerm().field()).isEqualTo("name");
        // Input "JOHN" should be lowercased to "john" for case-insensitive matching
        assertThat(termQuery.getTerm().text()).isEqualTo("john");
    }

    @Test
    void testCaseInsensitiveWithWildcards() {
        // Verify that mixed case with wildcards is lowercased
        SearchQuery searchQuery = parser.parse("name:Jo*N");
        Query query = builder.toLuceneQuery(searchQuery);

        assertThat(query).isInstanceOf(BooleanQuery.class);
        BooleanQuery booleanQuery = (BooleanQuery) query;
        assertThat(booleanQuery.clauses()).hasSize(1);

        BooleanClause clause = booleanQuery.clauses().getFirst();
        assertThat(clause.getQuery()).isInstanceOf(RegexpQuery.class);

        RegexpQuery regexpQuery = (RegexpQuery) clause.getQuery();
        assertThat(regexpQuery.getRegexp().field()).isEqualTo("name");
        // Input "Jo*N" should be lowercased and wildcards converted: "jo.*n"
        assertThat(regexpQuery.getRegexp().text()).isEqualTo("jo.*n");
    }
}
