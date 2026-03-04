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
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LuceneQueryBuilderTest {

    private LuceneQueryBuilder builder;
    private SearchQueryParser parser;

    @BeforeEach
    void setUp() {
        builder = new LuceneQueryBuilder();
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
        assertThat(clause.getQuery()).isInstanceOf(TermRangeQuery.class);

        TermRangeQuery rangeQuery = (TermRangeQuery) clause.getQuery();
        assertThat(rangeQuery.getField()).isEqualTo("age");
        assertThat(rangeQuery.includesLower()).isFalse();
        assertThat(rangeQuery.includesUpper()).isFalse();
    }

    @Test
    void testGreaterThanOrEqualsQuery() {
        SearchQuery searchQuery = parser.parse("age:>=30");
        Query query = builder.toLuceneQuery(searchQuery);

        assertThat(query).isInstanceOf(BooleanQuery.class);
        BooleanQuery booleanQuery = (BooleanQuery) query;
        BooleanClause clause = booleanQuery.clauses().getFirst();
        assertThat(clause.getQuery()).isInstanceOf(TermRangeQuery.class);

        TermRangeQuery rangeQuery = (TermRangeQuery) clause.getQuery();
        assertThat(rangeQuery.getField()).isEqualTo("age");
        assertThat(rangeQuery.includesLower()).isTrue();
        assertThat(rangeQuery.includesUpper()).isFalse();
    }

    @Test
    void testLessThanQuery() {
        SearchQuery searchQuery = parser.parse("age:<30");
        Query query = builder.toLuceneQuery(searchQuery);

        assertThat(query).isInstanceOf(BooleanQuery.class);
        BooleanQuery booleanQuery = (BooleanQuery) query;
        BooleanClause clause = booleanQuery.clauses().getFirst();
        assertThat(clause.getQuery()).isInstanceOf(TermRangeQuery.class);

        TermRangeQuery rangeQuery = (TermRangeQuery) clause.getQuery();
        assertThat(rangeQuery.getField()).isEqualTo("age");
        assertThat(rangeQuery.includesLower()).isFalse();
        assertThat(rangeQuery.includesUpper()).isFalse();
    }

    @Test
    void testLessThanOrEqualsQuery() {
        SearchQuery searchQuery = parser.parse("age:<=30");
        Query query = builder.toLuceneQuery(searchQuery);

        assertThat(query).isInstanceOf(BooleanQuery.class);
        BooleanQuery booleanQuery = (BooleanQuery) query;
        BooleanClause clause = booleanQuery.clauses().getFirst();
        assertThat(clause.getQuery()).isInstanceOf(TermRangeQuery.class);

        TermRangeQuery rangeQuery = (TermRangeQuery) clause.getQuery();
        assertThat(rangeQuery.getField()).isEqualTo("age");
        assertThat(rangeQuery.includesLower()).isFalse();
        assertThat(rangeQuery.includesUpper()).isTrue();
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
        // Second clause should be TermRangeQuery for age
        assertThat(booleanQuery.clauses().get(1).getQuery()).isInstanceOf(TermRangeQuery.class);
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
}
