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
import org.graylog.plugins.views.search.validation.ImmutableToken;
import org.graylog.plugins.views.search.validation.LuceneQueryParser;
import org.graylog.plugins.views.search.validation.ParsedQuery;
import org.graylog.plugins.views.search.validation.ParsedTerm;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void getFieldExistPosition() throws ParseException {
        final ParsedQuery fields = parser.parse("_exists_:lorem");
        assertThat(fields.allFieldNames()).contains("lorem");
        final ParsedTerm term = fields.terms().iterator().next();
        assertThat(term.keyToken()).isPresent();
        final ImmutableToken fieldNameToken = term.keyToken().get();
        assertThat(fieldNameToken.beginLine()).isEqualTo(1);
        assertThat(fieldNameToken.beginColumn()).isEqualTo(9);
        assertThat(fieldNameToken.endLine()).isEqualTo(1);
        assertThat(fieldNameToken.endColumn()).isEqualTo(14);
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
    void testRangeQuery() throws ParseException {
        final ParsedQuery query = parser.parse("http_response_code:[500 TO 504]");
        assertThat(query.terms().get(0).value()).isEqualTo("500");
        assertThat(query.terms().get(1).value()).isEqualTo("504");
    }


    @Test
    void testGtQuery() throws ParseException {
        final ParsedQuery query = parser.parse("http_response_code:>400");
        assertThat(query.terms().get(0).value()).isEqualTo("400");
    }

    @Test
    void testMultilineQuery() throws ParseException {
        final ParsedQuery query = parser.parse("foo:bar AND\nlorem:ipsum");

        {
            final ImmutableToken token = query.tokens().stream().filter(t -> t.image().equals("foo")).findFirst().orElseThrow(() -> new IllegalStateException("Expected token not found"));
            assertThat(token.beginLine()).isEqualTo(1);
            assertThat(token.beginColumn()).isEqualTo(0);
            assertThat(token.endLine()).isEqualTo(1);
            assertThat(token.endColumn()).isEqualTo(3);
        }

        {
            final ImmutableToken token = query.tokens().stream().filter(t -> t.image().equals("lorem")).findFirst().orElseThrow(() -> new IllegalStateException("Expected token not found"));
            assertThat(token.beginLine()).isEqualTo(2);
            assertThat(token.beginColumn()).isEqualTo(0);
            assertThat(token.endLine()).isEqualTo(2);
            assertThat(token.endColumn()).isEqualTo(5);
        }

        {
            final ImmutableToken token = query.tokens().stream().filter(t -> t.image().equals("ipsum")).findFirst().orElseThrow(() -> new IllegalStateException("Expected token not found"));
            assertThat(token.beginLine()).isEqualTo(2);
            assertThat(token.beginColumn()).isEqualTo(6);
            assertThat(token.endLine()).isEqualTo(2);
            assertThat(token.endColumn()).isEqualTo(11);
        }
    }

    @Test
    void testMultilineComplexQuery() throws ParseException {
        final ParsedQuery query = parser.parse("(\"ssh login\" AND (source:example.org OR source:another.example.org))\n" +
                "OR (\"login\" AND (source:example1.org OR source:another.example2.org))\n" +
                "OR not_existing_field:test");
        final ImmutableToken token = query.tokens().stream().filter(t -> t.image().equals("not_existing_field")).findFirst().orElseThrow(() -> new IllegalStateException("Expected token not found"));
        assertThat(token.beginLine()).isEqualTo(3);
        assertThat(token.beginColumn()).isEqualTo(3);
        assertThat(token.endLine()).isEqualTo(3);
        assertThat(token.endColumn()).isEqualTo(21);
    }

    @Test
    void testMatchingPositions() throws ParseException {
        assertThatThrownBy(() -> parser.parse("foo:"))
                .hasMessageContaining("Cannot parse 'foo:': Encountered \"<EOF>\" at line 1, column 4.");
    }


    @Test
    void testEmptyQueryNewlines() {
        assertThatThrownBy(() -> parser.parse("\n\n\n"))
                .hasMessageContaining("Cannot parse '\n\n\n': Encountered \"<EOF>\" at line 4, column 0.");
    }

    @Test
    void testValueTokenSimple() throws ParseException {
        final ParsedQuery query = parser.parse("foo:bar AND lorem:ipsum");
        assertThat(query.terms().size()).isEqualTo(2);
        final ParsedTerm fooTerm = query.terms().stream().filter(t -> t.field().equals("foo")).findFirst().get();
        assertThat(fooTerm.keyToken().get().image()).isEqualTo("foo");
        assertThat(fooTerm.valueToken().get().image()).isEqualTo("bar");
        final ParsedTerm loremTerm = query.terms().stream().filter(t -> t.field().equals("lorem")).findFirst().get();
        assertThat(loremTerm.keyToken().get().image()).isEqualTo("lorem");
        assertThat(loremTerm.valueToken().get().image()).isEqualTo("ipsum");
    }

    @Test
    void testValueTokenAnalyzed() throws ParseException {
        // we are using standard analyzer in the parser, which means that values are processed by the lowercase filter
        // This can lead to mismatches in equals during the value token recognition. This tests ensures that
        // uppercase values are correctly recognized and assigned
        final ParsedQuery query = parser.parse("foo:BAR");
        final ParsedTerm term = query.terms().iterator().next();
        assertThat(term.valueToken().get().image()).isEqualTo("BAR");
    }

    @Test
    void testFuzzyQuery() throws ParseException {
        final ParsedQuery query = parser.parse("fuzzy~");
        final ParsedTerm term = query.terms().iterator().next();
        assertThat(term.field()).isEqualTo("_default_");
        assertThat(term.value()).isEqualTo("fuzzy");
    }
}
