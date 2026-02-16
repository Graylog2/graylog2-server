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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MarkdownBuilder {
    private final StringBuilder sb;
    private final ObjectMapper mapper;
    private boolean isCodeBlockOpen;

    // Pre-compiled regex patterns for performance
    private static final Pattern HEADING_SPECIAL_CHARS = Pattern.compile("(?m)^(\\s*)([#>\\-+*])");
    private static final Pattern HEADING_ORDERED_LIST = Pattern.compile("(?m)^(\\s*)(\\d+)\\.");
    private static final Pattern INLINE_EMPHASIS = Pattern.compile("(?U)(?<=\\w)[*_](?=\\w)");

    public MarkdownBuilder() {
        this.sb = new StringBuilder();
        this.mapper = new ObjectMapper().registerModule(new Jdk8Module()).registerModule(new JodaModule());
        this.isCodeBlockOpen = false;
    }

    public boolean isEmpty() {
        return sb.isEmpty();
    }

    public static String orEmpty(Object input) {
        return input == null ? "" : input.toString();
    }

    public enum MdContext {
        PARAGRAPH,            // normal text
        HEADING,              // text used as a heading after '#'
        TABLE_CELL,           // inside GFM table cell
        LINK_TEXT,            // inside [text]
        LINK_URL              // inside (url)
    }

    public static String escapeMarkdown(String text) {
        return escapeMarkdown(text, null);
    }

    /**
     * Escapes Markdown special characters to prevent formatting issues.
     * This prevents user input from breaking Markdown structure (tables, code blocks, etc.).
     * <p>
     * Note: Does not escape periods (.) as they're rarely problematic in Markdown.
     * <p>
     * <b>Usage guidance:</b><br>
     * Markdown escaping should be applied <em>only when necessary</em>.
     * <ul>
     *   <li>
     *     <b>Always escape</b>:
     *     <ul>
     *       <li>Backslash (<code>\\</code>): must always be escaped first.</li>
     *       <li>Backtick (<code>`</code>): breaks code formatting if unescaped.</li>
     *     </ul>
     *   </li>
     *   <li>
     *     <b>Otherwise, escape only for:</b>
     *     <ul>
     *       <li>
     *         <b>Headings / list items / blockquotes:</b>
     *         Escape leading <code>#</code>, <code>-</code>, <code>+</code>, <code>*</code>, <code>&gt;</code>,
     *         or digit + period (<code>1.</code>) if they appear at the start of a line.
     *       </li>
     *       <li>
     *         <b>Inline text:</b>
     *         Escape <code>*</code> and <code>_</code> only when they are adjacent to word characters
     *         (i.e., could form bold or italic syntax).
     *       </li>
     *       <li>
     *         <b>Links / images:</b>
     *         Escape <code>[</code> and <code>]</code> in link text or image alt text, and
     *         <code>(</code> and <code>)</code> in URLs.
     *       </li>
     *       <li>
     *         <b>Tables:</b>
     *         Escape the pipe character (<code>|</code>) inside table cells
     *         (required in GitHub-Flavored Markdown tables).
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *     <b>Prefer wrapping in code blocks</b>:
     *     When rendering structured or user-generated data (e.g., key-value pairs, YAML, JSON),
     *     wrap the entire section in a fenced {@link #codeBlock(String)} (<code>```</code>).
     *   </li>
     * </ul>
     *
     * <b>Warning:</b> Do not call this method multiple times on the same string,
     * as it will escape already-escaped characters. If you need to add pre-escaped
     * content, use {@link #unsafeRaw(String)} instead.
     *
     * @param text The text to escape (should not be pre-escaped)
     * @param context The markdown context
     * @return The escaped text, or an empty string if input is null or empty.
     */
    public static String escapeMarkdown(String text, MdContext context) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        // Always escape backslash and backstick
        String s = text
                .replace("\\", "\\\\")  // Backslash
                .replace("`", "\\`");   // Backtick (breaks code)

        switch (context) {
            case HEADING:
                // Escape headings (#), blockquotes (>), or unordered list items (- + *)
                // as well as ordered list items (1., 2., …)
                // when these appear at the start of a line
                s = HEADING_SPECIAL_CHARS.matcher(s).replaceAll("$1\\\\$2");
                s = HEADING_ORDERED_LIST.matcher(s).replaceAll("$1$2\\\\.");
                // Fall through to PARAGRAPH to also escape inline emphasis
            case PARAGRAPH:
                // Escape * and _ only when they can start/end emphasis, i.e., when they are between word characters.
                return INLINE_EMPHASIS.matcher(s).replaceAll("\\\\$0");
            case TABLE_CELL:
                // Escape pipes when working with tables
                s = s.replace("|", "\\|");
                // Also escape inline emphasis like PARAGRAPH does
                return INLINE_EMPHASIS.matcher(s).replaceAll("\\\\$0");
            case LINK_TEXT:
                // Escape square brackets only when working with links
                return s.replace("[", "\\[").replace("]", "\\]");
            case LINK_URL:
                // Escape parentheses for link urls
                return s.replace("(", "\\(").replace(")", "\\)");
            case null, default:
                return s;
        }
    }

    /**
     * Instance method that escapes markdown based on current builder state.
     * If a code block is open, no escaping is performed.
     */
    private String escapeMarkdownInstance(String text, MdContext context) {
        if (isCodeBlockOpen) {
            return orEmpty(text);
        }
        return escapeMarkdown(text, context);
    }

    public static String bold(String text) {
        return "**" + escapeMarkdown(orEmpty(text), MdContext.PARAGRAPH) + "**";
    }

    public static String italic(String text) {
        return "*" + escapeMarkdown(orEmpty(text), MdContext.PARAGRAPH) + "*";
    }

    public static String code(String text) {
        return "`" + escapeMarkdown(text) + "`";
    }

    public static String link(String text, String url) {
        // Escape link text but not URL (URLs should remain as-is)
        return "[" + escapeMarkdown(orEmpty(text), MdContext.LINK_TEXT) + "](" + orEmpty(url) + ")";
    }

    public static Map<String, String> castMapValues(Map<String, Object> items, List<String> keys) {
        if (items == null) {
            return null;
        }
        try (var stream = (keys == null || keys.isEmpty()) ? items.entrySet().stream() : keys.stream()
//                .filter(items::containsKey)
                .map(k -> new AbstractMap.SimpleEntry<>(k, items.getOrDefault(k, null)))) {
            return stream.collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> orEmpty(e.getValue()).trim().replace("\n", " "),
                    (a, b) -> b,
                    LinkedHashMap::new
            ));
        }
    }

    public static Map<String, String> castMapValues(Map<String, Object> items) {
        return castMapValues(items, null);
    }

    private MarkdownBuilder heading(int level, String text) {
        sb.append("#".repeat(Math.min(6, Math.max(level, 1))))
                .append(" ")
                .append(escapeMarkdownInstance(orEmpty(text), MdContext.HEADING))
                .append("\n\n");
        return this;
    }

    public MarkdownBuilder h1(String text) {
        return heading(1, text);
    }

    public MarkdownBuilder h2(String text) {
        return heading(2, text);
    }

    public MarkdownBuilder h3(String text) {
        return heading(3, text);
    }

    public MarkdownBuilder h4(String text) {
        return heading(4, text);
    }

    public MarkdownBuilder h5(String text) {
        return heading(5, text);
    }

    public MarkdownBuilder h6(String text) {
        return heading(6, text);
    }

    public MarkdownBuilder paragraph(String text) {
        sb.append(escapeMarkdownInstance(orEmpty(text), MdContext.PARAGRAPH)).append("\n\n");
        return this;
    }

    public MarkdownBuilder codeBlock(String code) {
        return codeBlock(code, null);
    }

    public MarkdownBuilder codeBlock(String code, String language) {
        // Code blocks should preserve literal content
        // Note: triple backticks (```) in code content will break the block, but this is a markdown limitation
        openCodeBlock(language);
        sb.append(orEmpty(code));
        return closeCodeBlock();
    }

    public MarkdownBuilder openCodeBlock() {
        return openCodeBlock(null);
    }

    public MarkdownBuilder openCodeBlock(String language) {
        sb.append("```").append(orEmpty(language)).append("\n");
        isCodeBlockOpen = true;
        return this;
    }

    public MarkdownBuilder closeCodeBlock() {
        sb.append("\n```\n\n");
        isCodeBlockOpen = false;
        return this;
    }

    public MarkdownBuilder blockquote(String text) {
        if (text == null) {
            return this;
        }
        String[] lines = text.split("\n");
        for (String line : lines) {
            sb.append("> ").append(escapeMarkdownInstance(orEmpty(line), MdContext.HEADING)).append("\n");
        }
        sb.append("\n");
        return this;
    }

    public MarkdownBuilder orderedList(List<String> items) {
        if (items == null) {
            return this;
        }
        int i = 0;
        for (String item : items) {
            sb.append(++i).append(". ").append(escapeMarkdownInstance(orEmpty(item), MdContext.HEADING)).append("\n");
        }
        sb.append("\n");
        return this;
    }

    public MarkdownBuilder orderedList(String... items) {
        if (items == null) {
            return this;
        }
        return orderedList(Arrays.asList(items));
    }

    public MarkdownBuilder unorderedListItem(String item) {
        sb.append("- ").append(escapeMarkdownInstance(orEmpty(item), MdContext.HEADING)).append("\n");
        return this;
    }

    public MarkdownBuilder unorderedList(Iterable<String> items) {
        if (items == null) {
            return this;
        }
        items.forEach(this::unorderedListItem);
        sb.append("\n");
        return this;
    }

    public MarkdownBuilder unorderedListKVItem(String key, String value) {
        sb.append("- ")
                .append(bold(escapeMarkdownInstance(key, MdContext.HEADING)))
                .append(": ").append(escapeMarkdownInstance(value, null)).append("\n");
        return this;
    }

    public MarkdownBuilder unorderedListKVItem(String[] keys, String[] values) {
        if (keys == null || values == null) {
            return this;
        }
        if (keys.length != values.length) {
            throw new IllegalArgumentException("Arrays have different sizes");
        }
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            s.append("  ").append(keys[i]).append(": ").append(values[i]);
            s.append("\n");
        }
        return unorderedListItem(s.toString().trim());
    }

    public MarkdownBuilder unorderedListKVItem(Map<String, String> items) {
        if (items == null) {
            return this;
        }
        StringBuilder s = new StringBuilder();
        items.forEach((key, value) -> s.append("  ").append(key).append(": ").append(value).append("\n"));
        return unorderedListItem(s.toString().trim());
    }

    public MarkdownBuilder unorderedListKVItem(Object serializableItems, List<String> keys) {
        return unorderedListKVItem(
                castMapValues(mapper.convertValue(serializableItems, new TypeReference<>() {}), keys));
    }

    public enum Alignment {
        LEFT(" :--- |"),
        CENTER(" :---: |"),
        RIGHT(" ---: |"),
        DEFAULT(" --- |");

        private final String markdown;

        Alignment(String markdown) {
            this.markdown = markdown;
        }

        @Override
        public String toString() {
            return markdown;
        }
    }

    public MarkdownBuilder tableRow(Iterable<String> rowItems) {
        if (rowItems == null) {
            return this;
        }
        // Escape each cell to prevent pipes from breaking table structure
        List<String> escapedItems = new java.util.ArrayList<>();
        for (String item : rowItems) {
            escapedItems.add(escapeMarkdownInstance(item, MdContext.TABLE_CELL));
        }
        sb.append("| ").append(String.join(" | ", escapedItems)).append(" |\n");
        return this;
    }

    public MarkdownBuilder tableRow(String[] rowItems) {
        if (rowItems == null || rowItems.length == 0) {
            return this;
        }
        return tableRow(Arrays.asList(rowItems));
    }

    public MarkdownBuilder tableRow(Object serializableItems, List<String> keys) {
        if (serializableItems == null) {
            return this;
        }
        return tableRow(castMapValues(mapper.convertValue(serializableItems, new TypeReference<>() {}), keys).values());
    }

    public MarkdownBuilder tableRow(Object serializableItems, String[] keys) {
        return tableRow(serializableItems, keys == null || keys.length == 0 ? null : Arrays.asList(keys));
    }

    public MarkdownBuilder tableHeaders(List<String> headerItems, Alignment... alignments) {
        tableRow(headerItems);
        if (alignments != null && alignments.length > 0) {
            sb.append("|");
            if (alignments.length == 1) {
                sb.append(alignments[0].toString().repeat(headerItems.size()));
            } else {
                for (Alignment alignment : alignments) {
                    sb.append(alignment.toString());
                }
            }
            sb.append("\n");
        }
        return this;
    }

    public MarkdownBuilder tableHeaders(List<String> headerItems) {
        return tableHeaders(headerItems, Alignment.DEFAULT);
    }

    public MarkdownBuilder tableHeaders(String... headerItems) {
        return tableHeaders(Arrays.asList(headerItems), Alignment.DEFAULT);
    }

    public MarkdownBuilder table(String[] headers, String[][] rows, Alignment... alignments) {
        tableHeaders(Arrays.asList(headers), alignments);
        if (rows != null && rows.length > 0) {
            for (String[] row : rows) {
                tableRow(row);
            }
            sb.append("\n");
        }
        return this;
    }

    public MarkdownBuilder table(String[] headers, String[][] rows) {
        return table(headers, rows, Alignment.DEFAULT);
    }

    public MarkdownBuilder table(Map<String, String> items) {
        if (items == null) {
            return this;
        }
        List<String> keys = List.copyOf(items.keySet());
        tableHeaders(keys, Alignment.DEFAULT);
        tableRow(keys.stream().map(items::get).toList());
        sb.append("\n");
        return this;
    }

    public MarkdownBuilder horizontalRule() {
        sb.append("---\n\n");
        return this;
    }

    public MarkdownBuilder lineBreak() {
        sb.append("  \n");
        return this;
    }

    public MarkdownBuilder raw(String content) {
        sb.append(escapeMarkdownInstance(content, MdContext.PARAGRAPH));
        return this;
    }

    /**
     * Appends raw content without any markdown escaping.
     * Use this method when you want to insert literal Markdown syntax or
     * when you have already escaped the content yourself.
     * <p>
     * <b>Warning:</b> Using this method with untrusted user input may allow
     * markdown injection and break document structure.
     *
     * @param content The raw content to append (no escaping applied)
     * @return This MarkdownBuilder instance for method chaining
     */
    public MarkdownBuilder unsafeRaw(String content) {
        sb.append(orEmpty(content));
        return this;
    }

    @Override
    public String toString() {
        if (this.isCodeBlockOpen) {
            closeCodeBlock();
        }
        return sb.toString().trim() + "\n";
    }
}
