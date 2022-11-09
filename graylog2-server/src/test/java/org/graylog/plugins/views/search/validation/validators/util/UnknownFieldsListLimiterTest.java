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
package org.graylog.plugins.views.search.validation.validators.util;

import org.apache.lucene.queryparser.classic.Token;
import org.graylog.plugins.views.search.validation.ImmutableToken;
import org.graylog.plugins.views.search.validation.ParsedTerm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UnknownFieldsListLimiterTest {

    private UnknownFieldsListLimiter toTest;

    @BeforeEach
    void setUp() {
        toTest = new UnknownFieldsListLimiter();
    }

    @Test
    void doesNothingOnEmptyInput() {
        assertThat(toTest.filterElementsContainingUsefulInformation(Map.of()))
                .isEmpty();
    }

    @Test
    void doesNotLimitOnSingleElement() {
        final Map<String, List<ParsedTerm>> fieldTerms = Map.of("field", List.of(
                ParsedTerm.create("field", "oh!")
        ));
        assertThat(toTest.filterElementsContainingUsefulInformation(fieldTerms))
                .hasSize(1)
                .contains(ParsedTerm.create("field", "oh!"));
    }

    @Test
    void limitsToOneTermOnlyIfMultipleTermsWithoutPosition() {
        final Map<String, List<ParsedTerm>> fieldTerms = Map.of("field", List.of(
                ParsedTerm.create("field", "oh!"),
                ParsedTerm.create("field", "ah!"),
                ParsedTerm.create("field", "eh!")
        ));
        assertThat(toTest.filterElementsContainingUsefulInformation(fieldTerms))
                .hasSize(1)
                .contains(ParsedTerm.create("field", "oh!"));
    }

    @Test
    void limitsToPositionalTermsIfTheyArePresent() {
        final Token token1 = new Token(1, "token1");
        token1.beginLine = 1;
        token1.beginColumn = 1;
        token1.endLine = 1;
        token1.endColumn = 6;
        final ParsedTerm positionalTerm1 = ParsedTerm.builder().field("field").value("nvmd").keyToken(ImmutableToken.create(token1)).build();
        final Token token2 = new Token(1, "token2");
        token1.beginLine = 1;
        token1.beginColumn = 11;
        token1.endLine = 1;
        token1.endColumn = 16;
        final ParsedTerm positionalTerm2 = ParsedTerm.builder().field("field").value("nvmd").keyToken(ImmutableToken.create(token2)).build();

        final Map<String, List<ParsedTerm>> fieldTerms = Map.of(
                "field", List.of(
                        positionalTerm1,
                        ParsedTerm.create("field", "ah!"),
                        positionalTerm2,
                        ParsedTerm.create("field", "eh!")
                )
        );
        assertThat(toTest.filterElementsContainingUsefulInformation(fieldTerms))
                .hasSize(2)
                .contains(positionalTerm1, positionalTerm2);
    }
}
