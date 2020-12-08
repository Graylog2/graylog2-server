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
package org.graylog2.lookup.adapters.dsvhttp;

import org.junit.Test;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class DSVParserTest {
    @Test
    public void parseSimpleFile() throws Exception {
        final String input = "# Sample file for testing\n" +
                "foo:23\n" +
                "bar:42\n" +
                "baz:17";
        final DSVParser dsvParser = new DSVParser("#", "\n", ":", "", false, false, 0, Optional.of(1));

        final Map<String, String> result = dsvParser.parse(input);

        assertThat(result)
                .isNotNull()
                .isNotEmpty()
                .hasSize(3)
                .containsExactly(
                        new AbstractMap.SimpleEntry<>("foo", "23"),
                        new AbstractMap.SimpleEntry<>("bar", "42"),
                        new AbstractMap.SimpleEntry<>("baz", "17")
                );
    }

    @Test
    public void parseSimpleFileWithDifferentLineSeparator() throws Exception {
        final String input = "# Sample file for testing;foo:23;bar:42;baz:17";
        final DSVParser dsvParser = new DSVParser("#", ";", ":", "", false, false, 0, Optional.of(1));

        final Map<String, String> result = dsvParser.parse(input);

        assertThat(result)
                .isNotNull()
                .isNotEmpty()
                .hasSize(3)
                .containsExactly(
                        new AbstractMap.SimpleEntry<>("foo", "23"),
                        new AbstractMap.SimpleEntry<>("bar", "42"),
                        new AbstractMap.SimpleEntry<>("baz", "17")
                );
    }

    @Test
    public void parseFileWithSwappedColumns() throws Exception {
        final String input = "# Sample file for testing\n" +
                "foo:23\n" +
                "bar:42\n" +
                "baz:17";
        final DSVParser dsvParser = new DSVParser("#", "\n", ":", "", false, false, 1, Optional.of(0));

        final Map<String, String> result = dsvParser.parse(input);

        assertThat(result)
                .isNotNull()
                .isNotEmpty()
                .hasSize(3)
                .containsExactly(
                        new AbstractMap.SimpleEntry<>("23", "foo"),
                        new AbstractMap.SimpleEntry<>("42", "bar"),
                        new AbstractMap.SimpleEntry<>("17", "baz")
                );
    }

    @Test
    public void parseQuotedStrings() throws Exception {
        final String input = "# Sample file for testing\n" +
                "\"foo\":\"23\"\n" +
                "\"bar\":\"42\"\n" +
                "\"baz\":\"17\"\n" +
                "\"qux\":\"42:23\"\n" +
                "\"qu:ux\":\"42\"";
        final DSVParser dsvParser = new DSVParser("#", "\n", ":", "\"", false, false, 0, Optional.of(1));

        final Map<String, String> result = dsvParser.parse(input);

        assertThat(result)
                .isNotNull()
                .isNotEmpty()
                .hasSize(5)
                .containsExactly(
                        new AbstractMap.SimpleEntry<>("foo", "23"),
                        new AbstractMap.SimpleEntry<>("bar", "42"),
                        new AbstractMap.SimpleEntry<>("baz", "17"),
                        new AbstractMap.SimpleEntry<>("qux", "42:23"),
                        new AbstractMap.SimpleEntry<>("qu:ux", "42")
                );
    }

    @Test
    public void parseQuotedStringsWithSeparatorInKey() throws Exception {
        final String input = "# Sample file for testing\n" +
                "\"foo\":\"23\"\n" +
                "\"bar\":\"42\"\n" +
                "\"baz\":\"17\"\n" +
                "\"qux\":\"42:23\"\n" +
                "\"qu:ux\":\"42\"";
        final DSVParser dsvParser = new DSVParser("#", "\n", ":", "\"", false, false, 0, Optional.of(1));

        final Map<String, String> result = dsvParser.parse(input);

        assertThat(result)
                .isNotNull()
                .isNotEmpty()
                .hasSize(5)
                .containsExactly(
                        new AbstractMap.SimpleEntry<>("foo", "23"),
                        new AbstractMap.SimpleEntry<>("bar", "42"),
                        new AbstractMap.SimpleEntry<>("baz", "17"),
                        new AbstractMap.SimpleEntry<>("qux", "42:23"),
                        new AbstractMap.SimpleEntry<>("qu:ux", "42")
                );
    }

    @Test
    public void parseKeyOnlyFile() throws Exception {
        final String input = "# Sample file for testing\n" +
                "foo\n" +
                "bar\n" +
                "baz";
        final DSVParser dsvParser = new DSVParser("#", "\n", ":", "", true, false, 0, Optional.empty());

        final Map<String, String> result = dsvParser.parse(input);

        assertThat(result)
                .isNotNull()
                .isNotEmpty()
                .hasSize(3)
                .containsExactly(
                        new AbstractMap.SimpleEntry<>("foo", ""),
                        new AbstractMap.SimpleEntry<>("bar", ""),
                        new AbstractMap.SimpleEntry<>("baz", "")
                );
    }

    @Test
    public void parseKeyOnlyFileWithDifferentKeyColumn() throws Exception {
        final String input = "# Sample file for testing\n" +
                "1;foo\n" +
                "2;bar\n" +
                "3;baz";
        final DSVParser dsvParser = new DSVParser("#", "\n", ";", "", true, false, 1, Optional.empty());

        final Map<String, String> result = dsvParser.parse(input);

        assertThat(result)
                .isNotNull()
                .isNotEmpty()
                .hasSize(3)
                .containsExactly(
                        new AbstractMap.SimpleEntry<>("foo", ""),
                        new AbstractMap.SimpleEntry<>("bar", ""),
                        new AbstractMap.SimpleEntry<>("baz", "")
                );
    }

    @Test
    public void parseKeyOnlyFileWithNonexistingKeyColumn() throws Exception {
        final String input = "# Sample file for testing\n" +
                "1;foo\n" +
                "2;bar\n" +
                "3;baz";
        final DSVParser dsvParser = new DSVParser("#", "\n", ";", "", true, false, 2, Optional.empty());

        final Map<String, String> result = dsvParser.parse(input);

        assertThat(result)
                .isNotNull()
                .isEmpty();
    }
}