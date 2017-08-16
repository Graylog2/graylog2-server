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
        final DSVParser dsvParser = new DSVParser("#", ":", "", false, false, 0, Optional.of(1));

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
        final DSVParser dsvParser = new DSVParser("#", ":", "", false, false, 1, Optional.of(0));

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
                "\"baz\":\"17\"";
        final DSVParser dsvParser = new DSVParser("#", ":", "\"", false, false, 0, Optional.of(1));

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
    public void parseKeyOnlyFile() throws Exception {
        final String input = "# Sample file for testing\n" +
                "foo\n" +
                "bar\n" +
                "baz";
        final DSVParser dsvParser = new DSVParser("#", ":", "", true, false, 0, Optional.empty());

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
        final DSVParser dsvParser = new DSVParser("#", ";", "", true, false, 1, Optional.empty());

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
        final DSVParser dsvParser = new DSVParser("#", ";", "", true, false, 2, Optional.empty());

        final Map<String, String> result = dsvParser.parse(input);

        assertThat(result)
                .isNotNull()
                .isEmpty();
    }
}