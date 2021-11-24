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
package org.graylog.plugins.views.search.elasticsearch.parser;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.Token;
import org.graylog.plugins.views.search.validation.LuceneQueryParser;
import org.graylog.plugins.views.search.validation.ParsedQuery;
import org.graylog.plugins.views.search.validation.ParsedTerm;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class LuceneQueryParserTest {

    private final LuceneQueryParser parser = new LuceneQueryParser();

    @Test
    void getFieldNamesSimple() throws ParseException {
        final ParsedQuery fields = parser.parse("foo:bar AND lorem:ipsum");
        assertThat(fields.allFieldNames()).contains("foo", "lorem");
    }

    @Test
    void getFieldNamesExist() throws ParseException {
        final ParsedQuery fields = parser.parse("foo:bar AND _exists_:lorem");
        assertThat(fields.allFieldNames()).contains("foo", "lorem");
    }

    @Test
    void getFieldNamesComplex() throws ParseException {
        final ParsedQuery fields = parser.parse("type :( ssh OR login )");
        assertThat(fields.allFieldNames()).contains("type");
    }

    @Test
    void getFieldNamesNot() throws ParseException {
        final ParsedQuery parsedQuery = parser.parse("NOT _exists_ : type");
        assertThat(parsedQuery.allFieldNames()).contains("type");
    }

    @Test
    void unknownTerm() throws ParseException {
        final ParsedQuery query = parser.parse("foo:bar and");
        assertThat(query.allFieldNames()).contains("foo");
        assertThat(query.illegalOperators().stream().map(ParsedTerm::value).collect(Collectors.toSet())).contains("and");

        final ParsedTerm term = query.illegalOperators().iterator().next();
        final Token token = term.tokens().iterator().next();
        assertThat(token).isNotNull();

        assertThat(token.beginColumn).isEqualTo(8);
        assertThat(token.beginLine).isEqualTo(1);
        assertThat(token.endColumn).isEqualTo(11);
        assertThat(token.endLine).isEqualTo(1);
    }

    @Test
    void testIllegalOperators() throws ParseException {
        {
            final ParsedQuery query = parser.parse("foo:bar baz");
            assertThat(query.illegalOperators()).isEmpty();
        }

        {
            final ParsedQuery queryWithAnd = parser.parse("foo:bar and");
            assertThat(queryWithAnd.illegalOperators()).isNotEmpty();
            final ParsedTerm illegalOperator = queryWithAnd.illegalOperators().iterator().next();
            assertThat(illegalOperator.value()).isEqualTo("and");
        }

        {
            final ParsedQuery queryWithOr = parser.parse("foo:bar or");
            assertThat(queryWithOr.illegalOperators()).isNotEmpty();
            final ParsedTerm illegalOperator = queryWithOr.illegalOperators().iterator().next();
            assertThat(illegalOperator.value()).isEqualTo("or");
        }
    }
}
