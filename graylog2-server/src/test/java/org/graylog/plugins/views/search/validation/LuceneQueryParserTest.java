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
package org.graylog.plugins.views.search.validation;

import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LuceneQueryParserTest {

    private final LuceneQueryParser parser = new LuceneQueryParser(false);


    @Test
    void testSuperSimpleQuery() throws ParseException {
        final ParsedQuery fields = parser.parse("foo:bar");
        assertThat(fields.allFieldNames()).contains("foo");
    }

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
        assertThat(fields.terms())
                .hasSize(1)
                .extracting(ParsedTerm::keyToken)
                .hasOnlyOneElementSatisfying(term ->
                        assertThat(term).hasValueSatisfying(t -> {
                            assertThat(t.beginLine()).isEqualTo(1);
                            assertThat(t.beginColumn()).isEqualTo(9);
                            assertThat(t.endLine()).isEqualTo(1);
                            assertThat(t.endColumn()).isEqualTo(14);
                        }));
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
        assertThat(query.terms()).extracting(ParsedTerm::value).contains("500", "504");
    }

    @Test
    void testGtQuery() throws ParseException {
        final ParsedQuery query = parser.parse("http_response_code:>400");
        assertThat(query.terms()).extracting(ParsedTerm::value).contains("400");
    }

    @Test
    void testMultilineQuery() throws ParseException {
        final ParsedQuery query = parser.parse("foo:bar AND\nlorem:ipsum");

        assertThat(query.tokens())
                .anySatisfy(token -> {
                    assertThat(token.image()).isEqualTo("foo");
                    assertThat(token.beginLine()).isEqualTo(1);
                    assertThat(token.beginColumn()).isEqualTo(0);
                    assertThat(token.endLine()).isEqualTo(1);
                    assertThat(token.endColumn()).isEqualTo(3);
                })
                .anySatisfy(token -> {
                    assertThat(token.image()).isEqualTo("lorem");
                    assertThat(token.beginLine()).isEqualTo(2);
                    assertThat(token.beginColumn()).isEqualTo(0);
                    assertThat(token.endLine()).isEqualTo(2);
                    assertThat(token.endColumn()).isEqualTo(5);
                })
                .anySatisfy(token -> {
                    assertThat(token.image()).isEqualTo("ipsum");
                    assertThat(token.beginLine()).isEqualTo(2);
                    assertThat(token.beginColumn()).isEqualTo(6);
                    assertThat(token.endLine()).isEqualTo(2);
                    assertThat(token.endColumn()).isEqualTo(11);
                });
    }

    @Test
    void testMultilineComplexQuery() throws ParseException {
        final ParsedQuery query = parser.parse("(\"ssh login\" AND (source:example.org OR source:another.example.org))\n" +
                "OR (\"login\" AND (source:example1.org OR source:another.example2.org))\n" +
                "OR not_existing_field:test");

        assertThat(query.tokens())
                .anySatisfy( token -> {
                    assertThat(token.image()).isEqualTo("not_existing_field");
                    assertThat(token.beginLine()).isEqualTo(3);
                    assertThat(token.beginColumn()).isEqualTo(3);
                    assertThat(token.endLine()).isEqualTo(3);
                    assertThat(token.endColumn()).isEqualTo(21);
                });
    }

    @Test
    void testMatchingPositions() {
        assertThatThrownBy(() -> parser.parse("foo:"))
                .hasMessageContaining("Cannot parse 'foo:': Encountered \"<EOF>\" at line 1, column 3.");
    }

    @Test
    void testEmptyQueryNewlines() {
        assertThatThrownBy(() -> parser.parse("\n\n\n"))
                .hasMessageContaining("Cannot parse '\n\n\n': Encountered \"<EOF>\" at line 3, column -1.");
    }

    @Test
    void testValueTokenSimple() throws ParseException {
        final ParsedQuery query = parser.parse("foo:bar AND lorem:ipsum");
        assertThat(query.terms().size()).isEqualTo(2);

        assertThat(query.terms())
                .hasSize(2)
                .anySatisfy(term -> {
                    assertThat(term.field()).isEqualTo("foo");
                    assertThat(term.keyToken()).map(ImmutableToken::image).hasValue("foo");
                    assertThat(term.valueToken()).map(ImmutableToken::image).hasValue("bar");
                })
                .anySatisfy(term -> {
                    assertThat(term.field()).isEqualTo("lorem");
                    assertThat(term.keyToken()).map(ImmutableToken::image).hasValue("lorem");
                    assertThat(term.valueToken()).map(ImmutableToken::image).hasValue("ipsum");
                });
    }

    @Test
    void testValueTokenAnalyzed() throws ParseException {
        // we are using standard analyzer in the parser, which means that values are processed by the lowercase filter
        // This can lead to mismatches in equals during the value token recognition. This tests ensures that
        // uppercase values are correctly recognized and assigned
        final ParsedQuery query = parser.parse("foo:BAR");

        assertThat(query.terms())
                .extracting(ParsedTerm::valueToken)
                .hasOnlyOneElementSatisfying(token -> assertThat(token).map(ImmutableToken::image).hasValue("BAR"));
    }

    @Test
    void testFuzzyQuery() throws ParseException {
        final ParsedQuery query = parser.parse("fuzzy~");
        assertThat(query.terms())
                .hasOnlyOneElementSatisfying(term -> {
                    assertThat(term.field()).isEqualTo("_default_");
                    assertThat(term.value()).isEqualTo("fuzzy");
                });
    }

    @Test
    void testRepeatedQuery() throws ParseException {
        final ParsedQuery parsedQuery = parser.parse("foo:bar AND foo:bar AND something:else");
        assertThat(parsedQuery.terms().size()).isEqualTo(3);
        assertThat(parsedQuery.terms())
                .filteredOn(parsedTerm -> parsedTerm.field().equals("foo"))
                .extracting(ParsedTerm::keyToken)
                .extracting(Optional::get)
                .hasSize(2)
                .satisfies(keyTokens -> assertThat(keyTokens.get(0)).isNotEqualTo(keyTokens.get(1)));
    }

    @Test
    void testOrQuery() throws ParseException {
        final ParsedQuery query = parser.parse("unknown_field:(x OR y)");
        assertThat(query.terms().size()).isEqualTo(2);
        assertThat(query.terms())
                .extracting(ParsedTerm::field)
                .containsOnly("unknown_field");
    }

    @Test
    void testLeadingWildcardsParsingDependsOnParserSettings() throws ParseException {
        assertThatThrownBy(() -> parser.parse("foo:*bar"))
                .isInstanceOf(ParseException.class);

        assertThatThrownBy(() -> parser.parse("foo:?bar"))
                .isInstanceOf(ParseException.class);

        final LuceneQueryParser leadingWildcardsTolerantParser = new LuceneQueryParser(true);
        assertThat(leadingWildcardsTolerantParser.parse("foo:*bar"))
                .isNotNull();
        assertThat(leadingWildcardsTolerantParser.parse("foo:?bar"))
                .isNotNull();
    }
}
