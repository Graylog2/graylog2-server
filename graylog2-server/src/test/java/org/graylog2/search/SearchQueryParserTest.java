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
package org.graylog2.search;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;
import org.mongojack.DBQuery;
import org.mongojack.internal.query.CollectionQueryCondition;
import org.mongojack.internal.query.CompoundQueryCondition;
import org.mongojack.internal.query.QueryCondition;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.search.SearchQueryParser.DEFAULT_OPERATOR;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SearchQueryParserTest {

    @Test
    void explicitAllowedField() {
        SearchQueryParser parser = new SearchQueryParser("defaultfield", ImmutableSet.of("name", "id"));
        final SearchQuery query = parser.parse("name:foo");
        final Multimap<String, SearchQueryParser.FieldValue> queryMap = query.getQueryMap();

        assertThat(queryMap.size()).isEqualTo(1);
        assertThat(queryMap.get("name")).containsOnly(new SearchQueryParser.FieldValue("foo", false));
        assertThat(query.hasDisallowedKeys()).isFalse();
        assertThat(query.getDisallowedKeys()).isEmpty();

        final DBQuery.Query dbQuery = query.toDBQuery();
        final Collection<String> fieldNamesUsed = extractFieldNames(dbQuery.conditions());
        assertThat(fieldNamesUsed).containsExactly("name");
    }

    @Test
    void decodeQuery() throws UnsupportedEncodingException {
        SearchQueryParser parser = new SearchQueryParser("defaultfield", ImmutableSet.of("name", "id"));
        final String urlEncodedQuery = URLEncoder.encode("name:foo", StandardCharsets.UTF_8.name());
        final SearchQuery query = parser.parse(urlEncodedQuery);
        final Multimap<String, SearchQueryParser.FieldValue> queryMap = query.getQueryMap();

        assertThat(queryMap.size()).isEqualTo(1);
        assertThat(queryMap.get("name")).containsOnly(new SearchQueryParser.FieldValue("foo", false));
        assertThat(query.hasDisallowedKeys()).isFalse();
        assertThat(query.getDisallowedKeys()).isEmpty();

        final DBQuery.Query dbQuery = query.toDBQuery();
        final Collection<String> fieldNamesUsed = extractFieldNames(dbQuery.conditions());
        assertThat(fieldNamesUsed).containsExactly("name");
    }

    @Test
    void nullQuery() {
        SearchQueryParser parser = new SearchQueryParser("defaultfield", ImmutableSet.of("name", "id"));
        final SearchQuery query = parser.parse(null);

        assertThat(query.getQueryString()).isNullOrEmpty();

        final Multimap<String, SearchQueryParser.FieldValue> queryMap = query.getQueryMap();

        assertThat(queryMap.size()).isEqualTo(0);
    }

    @Test
    void disallowedField() {
        SearchQueryParser parser = new SearchQueryParser("defaultfield", ImmutableSet.of("name", "id"));
        final SearchQuery query = parser.parse("notallowed:foo");
        final Multimap<String, SearchQueryParser.FieldValue> queryMap = query.getQueryMap();

        assertThat(queryMap.size()).isEqualTo(1);
        assertThat(queryMap.get("defaultfield")).containsOnly(new SearchQueryParser.FieldValue("foo", false));
        assertThat(query.hasDisallowedKeys()).isTrue();
        assertThat(query.getDisallowedKeys()).containsExactly("notallowed");
        final DBQuery.Query dbQuery = query.toDBQuery();
        final Collection<String> fieldNames = extractFieldNames(dbQuery.conditions());
        assertThat(fieldNames).containsExactly("defaultfield");
    }

    @Test
    void mappedFields() {
        SearchQueryParser parser = new SearchQueryParser("defaultfield",
                ImmutableMap.of(
                        "name", SearchQueryField.create("index_name"),
                        "id", SearchQueryField.create("real_id"))
        );

        final SearchQuery query = parser.parse("name:foo id:1234");
        final Multimap<String, SearchQueryParser.FieldValue> queryMap = query.getQueryMap();

        assertThat(queryMap.size()).isEqualTo(2);
        assertThat(queryMap.keySet()).containsOnly("index_name", "real_id");
        assertThat(queryMap.get("index_name")).containsOnly(new SearchQueryParser.FieldValue("foo", false));
        assertThat(queryMap.get("real_id")).containsOnly(new SearchQueryParser.FieldValue("1234", false));
        assertThat(query.hasDisallowedKeys()).isFalse();

        final DBQuery.Query dbQuery = query.toDBQuery();
        final Collection<String> fieldNames = extractFieldNames(dbQuery.conditions());
        assertThat(fieldNames).containsOnly("index_name", "real_id");
    }

    private Collection<String> extractFieldNames(Set<Map.Entry<String, QueryCondition>> conditions) {
        final ImmutableSet.Builder<String> names = ImmutableSet.builder();

        // recurse into the tree, conveniently there's no visitor we can use, so it's manual
        conditions.forEach(entry -> {
            final String op = entry.getKey();
            if (!op.startsWith("$")) {
                names.add(op);
            }
            final QueryCondition queryCondition = entry.getValue();
            if (queryCondition instanceof CollectionQueryCondition) {
                names.addAll(
                        extractFieldNames(((CollectionQueryCondition) queryCondition).getValues().stream()
                                .map(qc -> Maps.immutableEntry("$dummy", qc))
                                .collect(Collectors.toSet()))
                );
            } else if (queryCondition instanceof CompoundQueryCondition) {
                names.addAll(
                        extractFieldNames(((CompoundQueryCondition) queryCondition).getQuery().conditions())
                );
            }
        });

        return names.build();
    }

    @Test
    void extractOperator() {
        final SearchQueryParser parser = new SearchQueryParser("defaultfield",
                ImmutableMap.of(
                        "id", SearchQueryField.create("real_id"),
                        "date", SearchQueryField.create("created_at", SearchQueryField.Type.DATE),
                        "int", SearchQueryField.create("int", SearchQueryField.Type.INT))
        );

        final SearchQueryOperator defaultOp = SearchQueryOperators.REGEXP;

        checkQuery(parser, "", SearchQueryField.Type.STRING, "", defaultOp);
        checkQuery(parser, "h", SearchQueryField.Type.STRING, "h", defaultOp);
        checkQuery(parser, "he", SearchQueryField.Type.STRING, "he", defaultOp);
        checkQuery(parser, "hel", SearchQueryField.Type.STRING, "hel", defaultOp);
        checkQuery(parser, "hello", SearchQueryField.Type.STRING, "hello", defaultOp);
        checkQuery(parser, "=~ hello", SearchQueryField.Type.STRING, "hello", SearchQueryOperators.REGEXP);
        checkQuery(parser, "=  hello", SearchQueryField.Type.STRING, "hello", SearchQueryOperators.EQUALS);

        checkQuery(parser, ">=2017-03-23", SearchQueryField.Type.DATE, "2017-03-23", SearchQueryOperators.GREATER_EQUALS);
        checkQuery(parser, ">= 2017-03-23", SearchQueryField.Type.DATE, "2017-03-23", SearchQueryOperators.GREATER_EQUALS);
        checkQuery(parser, "<=2017-03-23", SearchQueryField.Type.DATE, "2017-03-23", SearchQueryOperators.LESS_EQUALS);
        checkQuery(parser, "<=  2017-03-23", SearchQueryField.Type.DATE, "2017-03-23", SearchQueryOperators.LESS_EQUALS);
        checkQuery(parser, ">2017-03-23", SearchQueryField.Type.DATE, "2017-03-23", SearchQueryOperators.GREATER);
        checkQuery(parser, ">    2017-03-23", SearchQueryField.Type.DATE, "2017-03-23", SearchQueryOperators.GREATER);
        checkQuery(parser, "<2017-03-23", SearchQueryField.Type.DATE, "2017-03-23", SearchQueryOperators.LESS);
        checkQuery(parser, "< 2017-03-23", SearchQueryField.Type.DATE, "2017-03-23", SearchQueryOperators.LESS);
        checkQuery(parser, "2017-03-23", SearchQueryField.Type.DATE, "2017-03-23", SearchQueryOperators.EQUALS);

        checkQuery(parser, ">=1", SearchQueryField.Type.INT, "1", SearchQueryOperators.GREATER_EQUALS);
        checkQuery(parser, "<=1", SearchQueryField.Type.INT, "1", SearchQueryOperators.LESS_EQUALS);
        checkQuery(parser, ">1", SearchQueryField.Type.INT, "1", SearchQueryOperators.GREATER);
        checkQuery(parser, "<1", SearchQueryField.Type.INT, "1", SearchQueryOperators.LESS);
        checkQuery(parser, "=1", SearchQueryField.Type.INT, "1", SearchQueryOperators.EQUALS);
        checkQuery(parser, "1", SearchQueryField.Type.INT, "1", SearchQueryOperators.EQUALS);

        checkQuery(parser, ">=1", SearchQueryField.Type.LONG, "1", SearchQueryOperators.GREATER_EQUALS);
        checkQuery(parser, "<=1", SearchQueryField.Type.LONG, "1", SearchQueryOperators.LESS_EQUALS);
        checkQuery(parser, ">1", SearchQueryField.Type.LONG, "1", SearchQueryOperators.GREATER);
        checkQuery(parser, "<1", SearchQueryField.Type.LONG, "1", SearchQueryOperators.LESS);
        checkQuery(parser, "=1", SearchQueryField.Type.LONG, "1", SearchQueryOperators.EQUALS);
        checkQuery(parser, "1", SearchQueryField.Type.LONG, "1", SearchQueryOperators.EQUALS);
    }

    private void checkQuery(SearchQueryParser parser, String query, SearchQueryField.Type type, String expectedQuery, SearchQueryOperator expectedOp) {
        final SearchQueryOperator defaultOperator = type == SearchQueryField.Type.STRING ? SearchQueryParser.DEFAULT_STRING_OPERATOR : DEFAULT_OPERATOR;
        final Pair<String, SearchQueryOperator> pair = parser.extractOperator(query, defaultOperator);
        assertThat(pair.getLeft()).isEqualTo(expectedQuery);
        assertThat(pair.getRight()).isEqualTo(expectedOp);
    }

    @Test
    void querySplitterMatcher() {
        final SearchQueryParser parser = new SearchQueryParser("defaultfield",
                ImmutableMap.of(
                        "id", SearchQueryField.create("real_id"),
                        "date", SearchQueryField.create("created_at", SearchQueryField.Type.DATE))
        );

        final String queryString = "from:\"2017-10-02 12:07:01.345\" hello:world foo:=~\"bar baz\"";
        final Matcher matcher = parser.querySplitterMatcher(queryString);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group()).isEqualTo("from:\"2017-10-02 12:07:01.345\"");
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group()).isEqualTo("hello:world");
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group()).isEqualTo("foo:=~\"bar baz\"");
        assertThat(matcher.find()).isFalse();
    }

    @Test
    void createFieldValue() {
        final ImmutableMap<String, SearchQueryField> fields = ImmutableMap.of(
                "id", SearchQueryField.create("real_id"),
                "date", SearchQueryField.create("created_at", SearchQueryField.Type.DATE));
        final SearchQueryParser parser = new SearchQueryParser("defaultfield", fields);

        final SearchQueryParser.FieldValue v1 = parser.createFieldValue(fields.get("id").getFieldType(), "abc", false);
        assertThat(v1.getOperator()).isEqualTo(SearchQueryParser.DEFAULT_STRING_OPERATOR);
        assertThat(v1.getValue()).isEqualTo("abc");
        assertThat(v1.isNegate()).isFalse();

        final SearchQueryParser.FieldValue v2 = parser.createFieldValue(fields.get("id").getFieldType(), "=abc", true);
        assertThat(v2.getOperator()).isEqualTo(SearchQueryOperators.EQUALS);
        assertThat(v2.getValue()).isEqualTo("abc");
        assertThat(v2.isNegate()).isTrue();

        final SearchQueryParser.FieldValue v3 = parser.createFieldValue(fields.get("date").getFieldType(), ">=2017-03-01", false);
        assertThat(v3.getOperator()).isEqualTo(SearchQueryOperators.GREATER_EQUALS);
        assertThat(v3.getValue()).isEqualTo(new DateTime(2017, 3, 1, 0, 0, DateTimeZone.UTC));
        assertThat(v3.isNegate()).isFalse();

        final SearchQueryParser.FieldValue v4 = parser.createFieldValue(fields.get("date").getFieldType(), ">=2017-03-01 12:12:12", false);
        assertThat(v4.getOperator()).isEqualTo(SearchQueryOperators.GREATER_EQUALS);
        assertThat(v4.getValue()).isEqualTo(new DateTime(2017, 3, 1, 12, 12, 12, DateTimeZone.UTC));
        assertThat(v4.isNegate()).isFalse();

        final SearchQueryParser.FieldValue v5 = parser.createFieldValue(fields.get("date").getFieldType(), "\">=2017-03-01 12:12:12\"", false);
        assertThat(v5.getOperator()).isEqualTo(SearchQueryOperators.GREATER_EQUALS);
        assertThat(v5.getValue()).isEqualTo(new DateTime(2017, 3, 1, 12, 12, 12, DateTimeZone.UTC));
        assertThat(v5.isNegate()).isFalse();
    }

    @Test
    void fieldPrefixIsAddedToAllFieldsIfSpecified() {
        final SearchQueryParser parser = new SearchQueryParser("name", ImmutableSet.of("name", "breed"), "pets.");

        final SearchQuery searchQuery = parser.parse("Bobby breed:terrier");
        final Multimap<String, SearchQueryParser.FieldValue> queryMap = searchQuery.getQueryMap();

        assertThat(queryMap.keySet().size()).isEqualTo(2);
        assertThat(queryMap.keySet()).containsOnly("pets.name", "pets.breed");
        assertThat(queryMap.get("pets.name")).containsOnly(new SearchQueryParser.FieldValue("Bobby", false));
        assertThat(queryMap.get("pets.breed")).containsOnly(new SearchQueryParser.FieldValue("terrier", false));
        assertThat(searchQuery.hasDisallowedKeys()).isFalse();
    }

    @Test
    void nullFieldPrefixDoesNotChangeDefaultBehavior() {
        final SearchQueryParser parser = new SearchQueryParser("name", ImmutableSet.of("name", "breed"), null);

        final SearchQuery searchQuery = parser.parse("Bobby breed:terrier");
        final Multimap<String, SearchQueryParser.FieldValue> queryMap = searchQuery.getQueryMap();

        assertThat(queryMap.keySet().size()).isEqualTo(2);
        assertThat(queryMap.keySet()).containsOnly("name", "breed");
        assertThat(queryMap.get("name")).containsOnly(new SearchQueryParser.FieldValue("Bobby", false));
        assertThat(queryMap.get("breed")).containsOnly(new SearchQueryParser.FieldValue("terrier", false));
        assertThat(searchQuery.hasDisallowedKeys()).isFalse();
    }

    @Test
    void emptyFieldPrefixDoesNotChangeDefaultBehavior() {
        final SearchQueryParser parser = new SearchQueryParser("name", ImmutableSet.of("name", "breed"), "");

        final SearchQuery searchQuery = parser.parse("Bobby breed:terrier");
        final Multimap<String, SearchQueryParser.FieldValue> queryMap = searchQuery.getQueryMap();

        assertThat(queryMap.keySet().size()).isEqualTo(2);
        assertThat(queryMap.keySet()).containsOnly("name", "breed");
        assertThat(queryMap.get("name")).containsOnly(new SearchQueryParser.FieldValue("Bobby", false));
        assertThat(queryMap.get("breed")).containsOnly(new SearchQueryParser.FieldValue("terrier", false));
        assertThat(searchQuery.hasDisallowedKeys()).isFalse();
    }

    @Test
    void booleanValuesSupported() {
        final SearchQueryParser parser = new SearchQueryParser("name",
                Map.of(
                        "name", SearchQueryField.create("title", SearchQueryField.Type.STRING),
                        "gone", SearchQueryField.create("disabled", SearchQueryField.Type.BOOLEAN)
                )
        );

        final SearchQuery searchQuery = parser.parse("gone:true");

        assertEquals("gone:true", searchQuery.getQueryString());
        final Multimap<String, SearchQueryParser.FieldValue> queryMap = searchQuery.getQueryMap();

        assertThat(queryMap.keySet().size()).isEqualTo(1);
        assertThat(queryMap.keySet()).containsOnly("disabled"); //properly renamed
        assertThat(queryMap.get("disabled"))
                .containsOnly(new SearchQueryParser.FieldValue(
                        true, //boolean true instead of "true" string!
                        SearchQueryOperators.EQUALS, //equals instead of reqexp!
                        false)
                );

    }

    @Test
    void objectIdValuesSupported() {
        final SearchQueryParser parser = new SearchQueryParser("name",
                Map.of(
                        "name", SearchQueryField.create("title", SearchQueryField.Type.STRING),
                        "id", SearchQueryField.create("_id", SearchQueryField.Type.OBJECT_ID)
                )
        );

        final SearchQuery searchQuery = parser.parse("id:5f4dfb9c69be46153b9a9a7b");

        assertEquals("id:5f4dfb9c69be46153b9a9a7b", searchQuery.getQueryString());
        final Multimap<String, SearchQueryParser.FieldValue> queryMap = searchQuery.getQueryMap();

        assertThat(queryMap.keySet().size()).isEqualTo(1);
        assertThat(queryMap.keySet()).containsOnly("_id"); //properly renamed
        assertThat(queryMap.get("_id"))
                .containsOnly(new SearchQueryParser.FieldValue(
                        new ObjectId("5f4dfb9c69be46153b9a9a7b"), //ObjectID instead of String
                        SearchQueryOperators.EQUALS, //equals instead of reqexp!
                        false)
                );

    }
}
