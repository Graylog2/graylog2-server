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
package org.graylog.mcp.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarkdownBuilderTest {

    private MarkdownBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new MarkdownBuilder();
    }

    @Test
    void testIsEmpty() {
        assertThat(builder.isEmpty()).isTrue();
        builder.paragraph("test");
        assertThat(builder.isEmpty()).isFalse();
    }

    @Test
    void testOrEmpty() {
        assertThat(MarkdownBuilder.orEmpty(null)).isEqualTo("");
        assertThat(MarkdownBuilder.orEmpty("test")).isEqualTo("test");
        assertThat(MarkdownBuilder.orEmpty(123)).isEqualTo("123");
    }

    @Test
    void testBold() {
        assertThat(MarkdownBuilder.bold("test")).isEqualTo("**test**");
        assertThat(MarkdownBuilder.bold(null)).isEqualTo("****");
        assertThat(MarkdownBuilder.bold("")).isEqualTo("****");
    }

    @Test
    void testItalic() {
        assertThat(MarkdownBuilder.italic("test")).isEqualTo("*test*");
        assertThat(MarkdownBuilder.italic(null)).isEqualTo("**");
        assertThat(MarkdownBuilder.italic("")).isEqualTo("**");
    }

    @Test
    void testCode() {
        assertThat(MarkdownBuilder.code("test")).isEqualTo("`test`");
        assertThat(MarkdownBuilder.code(null)).isEqualTo("``");
        assertThat(MarkdownBuilder.code("")).isEqualTo("``");
    }

    @Test
    void testLink() {
        assertThat(MarkdownBuilder.link("GitHub", "https://github.com"))
            .isEqualTo("[GitHub](https://github.com)");
        assertThat(MarkdownBuilder.link(null, "https://github.com"))
            .isEqualTo("[](https://github.com)");
        assertThat(MarkdownBuilder.link("GitHub", null))
            .isEqualTo("[GitHub]()");
    }

    @Test
    void testCastMapValues() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("key1", "value1");
        input.put("key2", 123);
        input.put("key3", "multi\nline");

        Map<String, String> result = MarkdownBuilder.castMapValues(input);

        assertThat(result).hasSize(3);
        assertThat(result.get("key1")).isEqualTo("value1");
        assertThat(result.get("key2")).isEqualTo("123");
        assertThat(result.get("key3")).isEqualTo("multi line");
    }

    @Test
    void testCastMapValuesWithKeys() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("key1", "value1");
        input.put("key2", "value2");
        input.put("key3", "value3");

        Map<String, String> result = MarkdownBuilder.castMapValues(input, List.of("key1", "key3"));

        assertThat(result).hasSize(2);
        assertThat(result).containsKeys("key1", "key3");
        assertThat(result).doesNotContainKey("key2");
    }

    @Test
    void testCastMapValuesWithNull() {
        assertThat(MarkdownBuilder.castMapValues(null)).isNull();
        assertThat(MarkdownBuilder.castMapValues(null, List.of())).isNull();
    }

    @Test
    void testH1() {
        String result = builder.h1("Heading 1").toString();
        assertThat(result).isEqualTo("# Heading 1\n");
    }

    @Test
    void testH2() {
        String result = builder.h2("Heading 2").toString();
        assertThat(result).isEqualTo("## Heading 2\n");
    }

    @Test
    void testH3() {
        String result = builder.h3("Heading 3").toString();
        assertThat(result).isEqualTo("### Heading 3\n");
    }

    @Test
    void testH4() {
        String result = builder.h4("Heading 4").toString();
        assertThat(result).isEqualTo("#### Heading 4\n");
    }

    @Test
    void testH5() {
        String result = builder.h5("Heading 5").toString();
        assertThat(result).isEqualTo("##### Heading 5\n");
    }

    @Test
    void testH6() {
        String result = builder.h6("Heading 6").toString();
        assertThat(result).isEqualTo("###### Heading 6\n");
    }

    @Test
    void testHeadingWithNull() {
        String result = builder.h1(null).toString();
        assertThat(result).isEqualTo("#\n");
    }

    @Test
    void testParagraph() {
        String result = builder.paragraph("This is a paragraph.").toString();
        assertThat(result).isEqualTo("This is a paragraph.\n");
    }

    @Test
    void testParagraphWithNull() {
        String result = builder.paragraph(null).toString();
        assertThat(result).isEqualTo("\n");
    }

    @Test
    void testCodeBlockWithoutLanguage() {
        String result = builder.codeBlock("System.out.println();").toString();
        assertThat(result).isEqualTo("```\nSystem.out.println();\n```\n");
    }

    @Test
    void testCodeBlockWithLanguage() {
        String result = builder.codeBlock("System.out.println();", "java").toString();
        assertThat(result).isEqualTo("```java\nSystem.out.println();\n```\n");
    }

    @Test
    void testCodeBlockWithNull() {
        String result = builder.codeBlock(null).toString();
        assertThat(result).isEqualTo("```\n\n```\n");
    }

    @Test
    void testBlockquote() {
        String result = builder.blockquote("This is a quote.").toString();
        assertThat(result).isEqualTo("> This is a quote.\n");
    }

    @Test
    void testBlockquoteMultiline() {
        String result = builder.blockquote("Line 1\nLine 2\nLine 3").toString();
        assertThat(result).isEqualTo("> Line 1\n> Line 2\n> Line 3\n");
    }

    @Test
    void testBlockquoteWithNull() {
        String result = builder.blockquote(null).toString();
        assertThat(result).isEqualTo("\n");
    }

    @Test
    void testOrderedListWithList() {
        String result = builder.orderedList(List.of("First", "Second", "Third")).toString();
        assertThat(result).isEqualTo("1. First\n2. Second\n3. Third\n");
    }

    @Test
    void testOrderedListWithVarargs() {
        String result = builder.orderedList("First", "Second", "Third").toString();
        assertThat(result).isEqualTo("1. First\n2. Second\n3. Third\n");
    }

    @Test
    void testOrderedListWithNull() {
        String result = builder.orderedList((List<String>) null).toString();
        assertThat(result).isEqualTo("\n");
    }

    @Test
    void testUnorderedList() {
        String result = builder.unorderedList(List.of("Apple", "Banana", "Cherry")).toString();
        assertThat(result).isEqualTo("- Apple\n- Banana\n- Cherry\n");
    }

    @Test
    void testUnorderedListItem() {
        String result = builder.unorderedListItem("Single item").toString();
        assertThat(result).isEqualTo("- Single item\n");
    }

    @Test
    void testUnorderedListWithNull() {
        String result = builder.unorderedList((Iterable<String>) null).toString();
        assertThat(result).isEqualTo("\n");
    }

    @Test
    void testUnorderedListKVItem() {
        String result = builder.unorderedListKVItem("Name", "John Doe").toString();
        assertThat(result).isEqualTo("- **Name**: John Doe\n");
    }

    @Test
    void testUnorderedListKVItemWithArrays() {
        String[] keys = {"Name", "Age", "City"};
        String[] values = {"John", "30", "NYC"};
        String result = builder.unorderedListKVItem(keys, values).toString();
        assertThat(result).contains("Name: John");
        assertThat(result).contains("Age: 30");
        assertThat(result).contains("City: NYC");
    }

    @Test
    void testUnorderedListKVItemWithArraysMismatch() {
        String[] keys = {"Name", "Age"};
        String[] values = {"John"};
        assertThatThrownBy(() -> builder.unorderedListKVItem(keys, values))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Arrays have different sizes");
    }

    @Test
    void testUnorderedListKVItemWithMap() {
        Map<String, String> items = new LinkedHashMap<>();
        items.put("Name", "Jane");
        items.put("Role", "Developer");

        String result = builder.unorderedListKVItem(items).toString();
        assertThat(result).contains("Name: Jane");
        assertThat(result).contains("Role: Developer");
    }

    @Test
    void testUnorderedListKVItemWithNullMap() {
        String result = builder.unorderedListKVItem((Map<String, String>) null).toString();
        assertThat(result).isEqualTo("\n");
    }

    @Test
    void testTableRow() {
        String result = builder.tableRow(List.of("Col1", "Col2", "Col3")).toString();
        assertThat(result).isEqualTo("| Col1 | Col2 | Col3 |\n");
    }

    @Test
    void testTableRowWithArray() {
        String result = builder.tableRow(new String[]{"A", "B", "C"}).toString();
        assertThat(result).isEqualTo("| A | B | C |\n");
    }

    @Test
    void testTableRowWithNull() {
        String result = builder.tableRow((List<String>) null).toString();
        assertThat(result).isEqualTo("\n");
    }

    @Test
    void testTableHeaders() {
        String result = builder.tableHeaders(List.of("Name", "Age", "City")).toString();
        assertThat(result).contains("| Name | Age | City |");
        assertThat(result).contains("| --- | --- | --- |");
    }

    @Test
    void testTableHeadersWithAlignment() {
        String result = builder.tableHeaders(
            List.of("Left", "Center", "Right"),
            MarkdownBuilder.Alignment.LEFT,
            MarkdownBuilder.Alignment.CENTER,
            MarkdownBuilder.Alignment.RIGHT
        ).toString();

        assertThat(result).contains("| Left | Center | Right |");
        assertThat(result).contains("| :--- | :---: | ---: |");
    }

    @Test
    void testTableHeadersWithSingleAlignment() {
        String result = builder.tableHeaders(
            List.of("Col1", "Col2"),
            MarkdownBuilder.Alignment.CENTER
        ).toString();

        assertThat(result).contains("| :---: | :---: |");
    }

    @Test
    void testTableHeadersVarargs() {
        String result = builder.tableHeaders("Header1", "Header2").toString();
        assertThat(result).contains("| Header1 | Header2 |");
    }

    @Test
    void testTable() {
        String[] headers = {"Name", "Age"};
        String[][] rows = {
            {"Alice", "25"},
            {"Bob", "30"}
        };

        String result = builder.table(headers, rows).toString();
        assertThat(result).contains("| Name | Age |");
        assertThat(result).contains("| Alice | 25 |");
        assertThat(result).contains("| Bob | 30 |");
    }

    @Test
    void testTableWithAlignment() {
        String[] headers = {"Name", "Age"};
        String[][] rows = {{"Alice", "25"}};

        String result = builder.table(
            headers,
            rows,
            MarkdownBuilder.Alignment.LEFT,
            MarkdownBuilder.Alignment.RIGHT
        ).toString();

        assertThat(result).contains("| :--- | ---: |");
    }

    @Test
    void testTableWithNullRows() {
        String[] headers = {"Col1", "Col2"};
        String result = builder.table(headers, null).toString();
        assertThat(result).contains("| Col1 | Col2 |");
        assertThat(result).doesNotContain("Alice");
    }

    @Test
    void testTableWithMap() {
        Map<String, String> items = new LinkedHashMap<>();
        items.put("Name", "Charlie");
        items.put("City", "SF");

        String result = builder.table(items).toString();
        assertThat(result).contains("| Name | City |");
        assertThat(result).contains("| Charlie | SF |");
    }

    @Test
    void testTableWithNullMap() {
        String result = builder.table((Map<String, String>) null).toString();
        assertThat(result).isEqualTo("\n");
    }

    @Test
    void testHorizontalRule() {
        String result = builder.horizontalRule().toString();
        assertThat(result).isEqualTo("---\n");
    }

    @Test
    void testLineBreak() {
        String result = builder.lineBreak().toString();
        assertThat(result).isEqualTo("\n");
    }

    @Test
    void testRaw() {
        String result = builder.raw("Custom **markdown** content").toString();
        assertThat(result).isEqualTo("Custom **markdown** content\n");
    }

    @Test
    void testChaining() {
        String result = builder
            .h1("Title")
            .paragraph("Intro paragraph")
            .h2("Section")
            .unorderedList(List.of("Item 1", "Item 2"))
            .toString();

        assertThat(result).contains("# Title");
        assertThat(result).contains("Intro paragraph");
        assertThat(result).contains("## Section");
        assertThat(result).contains("- Item 1");
        assertThat(result).contains("- Item 2");
    }

    @Test
    void testComplexDocument() {
        String result = builder
            .h1("Report")
            .paragraph("This is a test report.")
            .h2("Data")
            .table(new String[]{"Metric", "Value"}, new String[][]{
                {"Users", "100"},
                {"Sessions", "250"}
            })
            .h2("Notes")
            .blockquote("Important note here")
            .horizontalRule()
            .paragraph("End of report")
            .toString();

        assertThat(result).contains("# Report");
        assertThat(result).contains("## Data");
        assertThat(result).contains("| Metric | Value |");
        assertThat(result).contains("| Users | 100 |");
        assertThat(result).contains("> Important note here");
        assertThat(result).contains("---");
        assertThat(result).contains("End of report");
    }

    @Test
    void testToStringTrimming() {
        builder.paragraph("Test");
        String result = builder.toString();

        // toString should trim and add single newline at end
        assertThat(result).startsWith("Test");
        assertThat(result).endsWith("\n");
        assertThat(result).doesNotEndWith("\n\n");
    }

    @Test
    void testAlignmentToString() {
        assertThat(MarkdownBuilder.Alignment.LEFT.toString())
            .isEqualTo(" :--- |");
        assertThat(MarkdownBuilder.Alignment.CENTER.toString())
            .isEqualTo(" :---: |");
        assertThat(MarkdownBuilder.Alignment.RIGHT.toString())
            .isEqualTo(" ---: |");
        assertThat(MarkdownBuilder.Alignment.DEFAULT.toString())
            .isEqualTo(" --- |");
    }
}
