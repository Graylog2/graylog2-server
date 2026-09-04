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
        String result = builder.unorderedList(null).toString();
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
        String result = builder.unorderedListKVItem(null).toString();
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
        String result = builder.table(null).toString();
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

    // ===== Escaping Tests =====

    @Test
    void testEscapeMarkdown() {
        assertThat(MarkdownBuilder.escapeMarkdown(null, null)).isEqualTo("");
        assertThat(MarkdownBuilder.escapeMarkdown("", null)).isEqualTo("");
        assertThat(MarkdownBuilder.escapeMarkdown("plain text", null)).isEqualTo("plain text");
    }

    @Test
    void testEscapeMarkdownAlwaysEscaped() {
        // Backslash and backtick are ALWAYS escaped regardless of context
        assertThat(MarkdownBuilder.escapeMarkdown("\\", null)).isEqualTo("\\\\");
        assertThat(MarkdownBuilder.escapeMarkdown("`", null)).isEqualTo("\\`");
        assertThat(MarkdownBuilder.escapeMarkdown("test\\code`here", null)).isEqualTo("test\\\\code\\`here");

        // Test in different contexts - still always escaped
        assertThat(MarkdownBuilder.escapeMarkdown("\\", MarkdownBuilder.MdContext.PARAGRAPH)).isEqualTo("\\\\");
        assertThat(MarkdownBuilder.escapeMarkdown("`", MarkdownBuilder.MdContext.TABLE_CELL)).isEqualTo("\\`");
        assertThat(MarkdownBuilder.escapeMarkdown("\\`", MarkdownBuilder.MdContext.HEADING)).isEqualTo("\\\\\\`");
    }

    @Test
    void testEscapeMarkdownMultipleCharactersTableContext() {
        // In TABLE_CELL context, only pipes are escaped
        String input = "User|Name*Age#Status";
        String expected = "User\\|Name\\*Age#Status";
        assertThat(MarkdownBuilder.escapeMarkdown(input, MarkdownBuilder.MdContext.TABLE_CELL)).isEqualTo(expected);
    }

    @Test
    void testEscapeMarkdownMultipleCharactersParagraphContext() {
        // In PARAGRAPH context, only * and _ between word characters are escaped
        String input = "text*with*underscores_and_asterisks";
        String expected = "text\\*with\\*underscores\\_and\\_asterisks";
        assertThat(MarkdownBuilder.escapeMarkdown(input, MarkdownBuilder.MdContext.PARAGRAPH)).isEqualTo(expected);
    }

    @Test
    void testEscapeMarkdownWithBackslashFirst() {
        // Ensure backslash is always escaped first
        String input = "\\test";
        String expected = "\\\\test";
        assertThat(MarkdownBuilder.escapeMarkdown(input, null)).isEqualTo(expected);

        // In PARAGRAPH context with asterisk
        String input2 = "test\\*with";
        String expected2 = "test\\\\*with";
        assertThat(MarkdownBuilder.escapeMarkdown(input2, MarkdownBuilder.MdContext.PARAGRAPH)).isEqualTo(expected2);
    }

    @Test
    void testBoldWithSpecialCharacters() {
        String result = MarkdownBuilder.bold("text*with*asterisks");
        assertThat(result).isEqualTo("**text\\*with\\*asterisks**");
    }

    @Test
    void testItalicWithSpecialCharacters() {
        String result = MarkdownBuilder.italic("text_with_underscores");
        assertThat(result).isEqualTo("*text\\_with\\_underscores*");
    }

    @Test
    void testCodeWithBackticks() {
        String result = MarkdownBuilder.code("code`with`backticks");
        assertThat(result).isEqualTo("`code\\`with\\`backticks`");
    }

    @Test
    void testLinkWithSpecialCharacters() {
        String result = MarkdownBuilder.link("Link [text]", "https://example.com");
        assertThat(result).contains("\\[text\\]");
    }

    @Test
    void testTableWithPipes() {
        String result = builder.tableRow(List.of("Cell|with|pipes", "Normal")).toString();
        assertThat(result).contains("Cell\\|with\\|pipes");
        assertThat(result).contains("| Cell\\|with\\|pipes | Normal |");
    }

    @Test
    void testTableRowWithSpecialCharacters() {
        String[] row = {"Name*", "Age|25", "Status#Active"};
        String result = builder.tableRow(row).toString();
        // In TABLE_CELL context, pipes and inline emphasis (* _) between word chars are escaped
        assertThat(result).contains("Name*"); // * not between word characters, not escaped
        assertThat(result).contains("Age\\|25");
        assertThat(result).contains("Status#Active");
    }

    @Test
    void testTableRowWithInlineEmphasis() {
        String[] row = {"text*with*asterisks", "word_with_underscores", "normal|pipe"};
        String result = builder.tableRow(row).toString();
        // TABLE_CELL escapes pipes AND inline emphasis between word chars
        assertThat(result).contains("text\\*with\\*asterisks");
        assertThat(result).contains("word\\_with\\_underscores");
        assertThat(result).contains("normal\\|pipe");
    }

    @Test
    void testUnorderedListWithSpecialCharacters() {
        String result = builder.unorderedList(List.of("Item*1", "Item|2", "Item#3")).toString();
        // In HEADING context, special chars only escape at line start, not in middle, except for * and _
        assertThat(result).contains("- Item\\*1");
        assertThat(result).contains("- Item|2");
        assertThat(result).contains("- Item#3");
    }

    @Test
    void testUnorderedListWithLeadingSpecialCharacters() {
        // Test that special characters at the start of items ARE escaped
        String result = builder.unorderedList(List.of("#hashtag", "*asterisk", "-dash")).toString();
        assertThat(result).contains("- \\#hashtag");
        assertThat(result).contains("- \\*asterisk");
        assertThat(result).contains("- \\-dash");
    }

    @Test
    void testUnorderedListKVItemWithSpecialCharacters() {
        String result = builder.unorderedListKVItem("Key*Special", "Value|Pipes").toString();
        // Key uses HEADING context, Value has no context
        assertThat(result).contains("**Key\\\\*Special**");
        assertThat(result).contains("Value|Pipes");
    }

    @Test
    void testHeadingWithSpecialCharacters() {
        // Special characters in middle of heading text are not escaped
        String result = builder.h1("Heading # with hash").toString();
        assertThat(result).isEqualTo("# Heading # with hash\n");
    }

    @Test
    void testHeadingWithLeadingSpecialCharacters() {
        // Special characters at the start ARE escaped in HEADING context
        String result = builder.h1("# Leading hash").toString();
        assertThat(result).isEqualTo("# \\# Leading hash\n");

        builder = new MarkdownBuilder();
        String result2 = builder.h2("> Quote-like").toString();
        assertThat(result2).isEqualTo("## \\> Quote-like\n");
    }

    @Test
    void testParagraphWithSpecialCharacters() {
        // In PARAGRAPH context, * and _ only escape when between word characters
        // Spaces around them mean no escaping occurs
        String result = builder.paragraph("Text with *asterisks* and |pipes|").toString();
        assertThat(result).contains("*asterisks*");
        assertThat(result).contains("|pipes|");
    }

    @Test
    void testParagraphWithEmphasizableCharacters() {
        // When * or _ appear between word characters, they ARE escaped
        String result = builder.paragraph("word*with*asterisks and word_with_underscores").toString();
        assertThat(result).contains("word\\*with\\*asterisks");
        assertThat(result).contains("word\\_with\\_underscores");
    }

    @Test
    void testBlockquoteWithSpecialCharacters() {
        // Blockquote uses HEADING context, * with spaces around not escaped
        String result = builder.blockquote("Quote with *special* chars").toString();
        assertThat(result).contains("*special*");
    }

    @Test
    void testBlockquoteWithLeadingSpecialCharacters() {
        // Test leading special characters ARE escaped
        String result = builder.blockquote("# Looks like heading").toString();
        assertThat(result).contains("> \\# Looks like heading");
    }

    @Test
    void testComplexTableWithUserInput() {
        // Simulate user-controlled content that could break table formatting
        builder.tableHeaders("Name", "Description", "Status");
        builder.tableRow(new String[]{
            "User|Name",
            "Description with * and _",
            "Active|Pending"
        });

        String result = builder.toString();

        // In TABLE_CELL context, pipes are escaped but * and _ with spaces are not
        assertThat(result).contains("User\\|Name");
        assertThat(result).contains("Description with * and _"); // spaces around, not escaped
        assertThat(result).contains("Active\\|Pending");
    }

    @Test
    void testTableWithInlineFormattingCharacters() {
        // Test that inline emphasis between word characters IS escaped in tables
        builder.tableHeaders("Column1", "Column2");
        builder.tableRow(new String[]{"user*name*here", "value_with_underscores"});

        String result = builder.toString();
        assertThat(result).contains("user\\*name\\*here");
        assertThat(result).contains("value\\_with\\_underscores");
    }

    @Test
    void testCodeBlockPreservesLiteralContent() {
        // Code blocks should preserve content literally (except triple backticks)
        String code = "int x = 5 * 3;\nString s = \"pipe|asterisk*underscore_hash#\";\nSystem.out.println(\"Hello\");";
        String result = builder.codeBlock(code, "java").toString();

        // Content should not be escaped - all markdown special chars preserved
        assertThat(result).contains("int x = 5 * 3;");
        assertThat(result).contains("pipe|asterisk*underscore_hash#");
        assertThat(result).contains("System.out.println");

        // Verify these chars are NOT escaped
        assertThat(result).doesNotContain("\\|");
        assertThat(result).doesNotContain("\\*");
        assertThat(result).doesNotContain("\\_");
        assertThat(result).doesNotContain("\\#");
    }

    @Test
    void testOpenCodeBlockPreservesLiteralContent() {
        // Test that content added between openCodeBlock/closeCodeBlock is not escaped
        builder.openCodeBlock("yaml");
        builder.unsafeRaw("key: value|with*special_chars#here");
        builder.unsafeRaw("\nanother: line_with*markdown|syntax");
        builder.closeCodeBlock();

        String result = builder.toString();

        // Content should not be escaped
        assertThat(result).contains("```yaml");
        assertThat(result).contains("key: value|with*special_chars#here");
        assertThat(result).contains("another: line_with*markdown|syntax");

        // Verify no escaping occurred
        assertThat(result).doesNotContain("\\|");
        assertThat(result).doesNotContain("\\*");
        assertThat(result).doesNotContain("\\_");
        assertThat(result).doesNotContain("\\#");
    }

    @Test
    void testCodeBlockDoesNotEscapeButOtherMethodsDo() {
        // Verify that code blocks don't escape, but regular methods do
        builder.openCodeBlock("text");
        builder.unsafeRaw("text*with*asterisks");
        builder.closeCodeBlock();
        builder.paragraph("text*with*asterisks");

        String result = builder.toString();

        // Inside code block: not escaped
        assertThat(result).containsPattern("```text\\s+text\\*with\\*asterisks");

        // In paragraph: escaped
        assertThat(result).contains("text\\*with\\*asterisks");
    }

    @Test
    void testCastMapValuesWithSpecialCharacters() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("key1", "value|with|pipes");
        input.put("key2", "value*with*asterisks");

        Map<String, String> result = MarkdownBuilder.castMapValues(input);

        // castMapValues does NOT escape, it just converts to strings
        assertThat(result.get("key1")).isEqualTo("value|with|pipes");
        assertThat(result.get("key2")).isEqualTo("value*with*asterisks");
    }

    @Test
    void testNoDoubleEscaping() {
        // Verify that already-escaped content gets backslash escaped
        String alreadyEscaped = "already\\|escaped";

        // With no context, only backslash is escaped (pipe is not)
        String result = MarkdownBuilder.escapeMarkdown(alreadyEscaped, null);
        assertThat(result).isEqualTo("already\\\\|escaped");

        // With TABLE_CELL context, both backslash and pipe are escaped
        String result2 = MarkdownBuilder.escapeMarkdown(alreadyEscaped, MarkdownBuilder.MdContext.TABLE_CELL);
        assertThat(result2).isEqualTo("already\\\\\\|escaped");
    }

    @Test
    void testContextSpecificEscaping() {
        // Test that different contexts escape different characters
        String testString = "\\`*_[]()#|";

        // No context: only backslash and backtick
        String noContext = MarkdownBuilder.escapeMarkdown(testString, null);
        assertThat(noContext).isEqualTo("\\\\\\`*_[]()#|");

        // TABLE_CELL: backslash, backtick, pipe (no * or _ since not between word chars here)
        String tableCell = MarkdownBuilder.escapeMarkdown(testString, MarkdownBuilder.MdContext.TABLE_CELL);
        assertThat(tableCell).isEqualTo("\\\\\\`*_[]()#\\|");

        // LINK_TEXT: backslash, backtick, and square brackets
        String linkText = MarkdownBuilder.escapeMarkdown(testString, MarkdownBuilder.MdContext.LINK_TEXT);
        assertThat(linkText).isEqualTo("\\\\\\`*_\\[\\]()#|");

        // LINK_URL: backslash, backtick, and parentheses
        String linkUrl = MarkdownBuilder.escapeMarkdown(testString, MarkdownBuilder.MdContext.LINK_URL);
        assertThat(linkUrl).isEqualTo("\\\\\\`*_[]\\(\\)#|");
    }

    @Test
    void testContextSpecificEscapingWithWordBoundaries() {
        // Test inline emphasis escaping when between word characters
        String testString = "word*test*word word_test_word";

        // PARAGRAPH: escapes * and _ between word chars
        String paragraph = MarkdownBuilder.escapeMarkdown(testString, MarkdownBuilder.MdContext.PARAGRAPH);
        assertThat(paragraph).isEqualTo("word\\*test\\*word word\\_test\\_word");

        // TABLE_CELL: also escapes * and _ between word chars
        String tableCell = MarkdownBuilder.escapeMarkdown(testString, MarkdownBuilder.MdContext.TABLE_CELL);
        assertThat(tableCell).isEqualTo("word\\*test\\*word word\\_test\\_word");

        // LINK_TEXT: does NOT escape * and _ (only brackets)
        String linkText = MarkdownBuilder.escapeMarkdown(testString, MarkdownBuilder.MdContext.LINK_TEXT);
        assertThat(linkText).isEqualTo("word*test*word word_test_word");
    }

    @Test
    void testUnsafeRaw() {
        String result = builder.unsafeRaw("**bold** and *italic* with |pipes|").toString();
        // unsafeRaw should preserve all Markdown syntax literally
        assertThat(result).isEqualTo("**bold** and *italic* with |pipes|\n");
    }
}
