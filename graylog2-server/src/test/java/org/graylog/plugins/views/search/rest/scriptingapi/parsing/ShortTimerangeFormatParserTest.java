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
package org.graylog.plugins.views.search.rest.scriptingapi.parsing;

import org.graylog2.plugin.indexer.searches.timeranges.KeywordRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShortTimerangeFormatParserTest {

    private ShortTimerangeFormatParser toTest;

    @BeforeEach
    void setUp() {
        toTest = new ShortTimerangeFormatParser();
    }

    @Test
    void returnsEmptyOptionalOnNullInput() {
        assertThat(toTest.parse(null)).isEmpty();
    }

    @Test
    void returnsEmptyOptionalOnBadInput() {
        assertThat(toTest.parse("#$%")).isEmpty();
        assertThat(toTest.parse("13days")).isEmpty();
        assertThat(toTest.parse("42x")).isEmpty();
        assertThat(toTest.parse("-13days")).isEmpty();
        assertThat(toTest.parse("1,5d")).isEmpty();
        assertThat(toTest.parse("d13d")).isEmpty();
        assertThat(toTest.parse("")).isEmpty();
    }

    @Test
    void returnsProperTimeRangeOnGoodInput() {
        assertThat(toTest.parse("12s"))
                .isPresent()
                .contains(KeywordRange.create("last 12 seconds", "UTC"));
        assertThat(toTest.parse("42m"))
                .isPresent()
                .contains(KeywordRange.create("last 42 minutes", "UTC"));
        assertThat(toTest.parse("1h"))
                .isPresent()
                .contains(KeywordRange.create("last 1 hour", "UTC"));
        assertThat(toTest.parse("1d"))
                .isPresent()
                .contains(KeywordRange.create("last 1 day", "UTC"));
        assertThat(toTest.parse("2w"))
                .isPresent()
                .contains(KeywordRange.create("last 2 weeks", "UTC"));
        assertThat(toTest.parse("3M"))
                .isPresent()
                .contains(KeywordRange.create("last 3 months", "UTC"));
        assertThat(toTest.parse("1000y"))
                .isPresent()
                .contains(KeywordRange.create("last 1000 years", "UTC"));
    }
}
