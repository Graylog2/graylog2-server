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
package org.graylog.events.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class MoreSearchTest {

    // --- boundary conditions ---

    @Test
    void nullInputReturnsEmptyString() {
        assertThat(MoreSearch.luceneEscape(null)).isEqualTo("");
    }

    @Test
    void emptyInputReturnsEmptyString() {
        assertThat(MoreSearch.luceneEscape("")).isEqualTo("");
    }

    @Test
    void plainAlphanumericInputIsUnchanged() {
        assertThat(MoreSearch.luceneEscape("fooBar123")).isEqualTo("fooBar123");
    }

    // --- pre-existing special characters ---

    @ParameterizedTest(name = "char ''{0}'' is escaped to ''{1}''")
    @CsvSource({
            "\\,   \\\\",
            "+,    \\+",
            "-,    \\-",
            "!,    \\!",
            "(,    \\(",
            "),    \\)",
            ":,    \\:",
            "^,    \\^",
            "[,    \\[",
            "],    \\]",
            "{,    \\{",
            "},    \\}",
            "~,    \\~",
            "*,    \\*",
            "?,    \\?",
            "|,    \\|",
            "&,    \\&",
            "/,    \\/",
    })
    void preExistingSpecialCharactersAreEscaped(String input, String expected) {
        assertThat(MoreSearch.luceneEscape(input)).isEqualTo(expected);
    }

    // --- double-quote: was escaped via '\"', now via '"' (same char, same behavior) ---

    @Test
    void doubleQuoteIsEscaped() {
        // '"' and '\"' are identical Java char literals.
        assertThat(MoreSearch.luceneEscape("\"")).isEqualTo("\\\"");
    }

    @Test
    void doubleQuoteInsideStringIsEscaped() {
        assertThat(MoreSearch.luceneEscape("say \"hello\"")).isEqualTo("say\\ \\\"hello\\\"");
    }

    // --- space: newly escaped as of the change ---

    @Test
    void spaceIsEscaped() {
        assertThat(MoreSearch.luceneEscape(" ")).isEqualTo("\\ ");
    }

    @Test
    void spaceInsideStringIsEscaped() {
        assertThat(MoreSearch.luceneEscape("hello world")).isEqualTo("hello\\ world");
    }

    @Test
    void multipleSpacesAreAllEscaped() {
        assertThat(MoreSearch.luceneEscape("a b c")).isEqualTo("a\\ b\\ c");
    }

    // --- combined / real-world inputs ---

    @Test
    void streamIdWithoutSpecialCharsIsUnchanged() {
        // MongoDB ObjectIds are 24 hex chars — no special characters, no spaces.
        assertThat(MoreSearch.luceneEscape("5e8c1d2f3a4b5c6d7e8f9a0b")).isEqualTo("5e8c1d2f3a4b5c6d7e8f9a0b");
    }

    @Test
    void ipAddressWithSlashIsEscaped() {
        // e.g. CIDR used in watchlist
        assertThat(MoreSearch.luceneEscape("10.0.0.0/8")).isEqualTo("10.0.0.0\\/8");
    }

    @Test
    void usernameWithSpaceIsEscaped() {
        // user names can contain spaces; the escaped form is safe inside a quoted phrase query
        assertThat(MoreSearch.luceneEscape("John Doe")).isEqualTo("John\\ Doe");
    }

    @Test
    void tagWithSpaceIsEscaped() {
        // tags can be multi-word; callers wrap the result in quotes, so "multi\ word" still matches
        assertThat(MoreSearch.luceneEscape("multi word tag")).isEqualTo("multi\\ word\\ tag");
    }

    @Test
    void wildcardTermWithSpaceProducesSingleEscapedTerm() {
        // ExclusionQuery uses luceneEscape for wildcard values (not quoted). Before the change, "foo bar*"
        // would have produced "foo bar*" (two Lucene terms). Now it produces "foo\ bar*" (one term).
        assertThat(MoreSearch.luceneEscape("foo bar*")).isEqualTo("foo\\ bar\\*");
    }
}
