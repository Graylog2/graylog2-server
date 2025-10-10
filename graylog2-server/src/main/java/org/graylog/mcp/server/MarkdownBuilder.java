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
import java.util.stream.Collectors;

public class MarkdownBuilder {
    private final StringBuilder sb;
    private final ObjectMapper mapper;

    public static String orEmpty(Object input) { return input == null ? "" : input.toString(); }

    public static String bold(String text) {
        return "**" + orEmpty(text) + "**";
    }

    public static String italic(String text) {
        return "*" + orEmpty(text) + "*";
    }

    public static String code(String text) {
        return "`" + orEmpty(text) + "`";
    }

    public static String link(String text, String url) {
        return "[" + orEmpty(text) + "](" + orEmpty(url) + ")";
    }

    public static Map<String, String> castMapValues(Map<String, Object> items, List<String> keys) {
        if (items == null) return null;
        try (var stream = (keys == null || keys.isEmpty()) ? items.entrySet().stream() : keys.stream()
                .filter(items::containsKey).map(k -> new AbstractMap.SimpleEntry<>(k, items.get(k)))) {
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

    public MarkdownBuilder() {
        this.sb = new StringBuilder();
        this.mapper = new ObjectMapper().registerModule(new Jdk8Module()).registerModule(new JodaModule());
    }

    public boolean isEmpty() {
        return sb.isEmpty();
    }

    private MarkdownBuilder heading(int level, String text) {
        sb.append("#".repeat(Math.min(6, Math.max(level, 1)))).append(" ").append(orEmpty(text)).append("\n\n");
        return this;
    }

    public MarkdownBuilder h1(String text) { return heading(1, text); }

    public MarkdownBuilder h2(String text) { return heading(2, text); }

    public MarkdownBuilder h3(String text) { return heading(3, text); }

    public MarkdownBuilder h4(String text) { return heading(4, text); }

    public MarkdownBuilder h5(String text) { return heading(5, text); }

    public MarkdownBuilder h6(String text) { return heading(6, text); }

    public MarkdownBuilder paragraph(String text) {
        sb.append(orEmpty(text)).append("\n\n");
        return this;
    }

    public MarkdownBuilder codeBlock(String code) {
        return codeBlock(code, null);
    }

    public MarkdownBuilder codeBlock(String code, String language) {
        sb.append("```").append(orEmpty(language)).append("\n").append(orEmpty(code)).append("\n```\n\n");
        return this;
    }

    public MarkdownBuilder blockquote(String text) {
        if (text == null) return this;
        String[] lines = text.split("\n");
        for (String line : lines) {
            sb.append("> ").append(orEmpty(line)).append("\n");
        }
        sb.append("\n");
        return this;
    }

    public MarkdownBuilder orderedList(List<String> items) {
        if (items == null) return this;
        int i = 0;
        for (String item : items) {
            sb.append(++i).append(". ").append(orEmpty(item)).append("\n");
        }
        sb.append("\n");
        return this;
    }

    public MarkdownBuilder orderedList(String... items) {
        if (items == null) return this;
        return orderedList(Arrays.asList(items));
    }

    public MarkdownBuilder unorderedListItem(String item) {
        sb.append("- ").append(orEmpty(item)).append("\n");
        return this;
    }

    public MarkdownBuilder unorderedList(Iterable<String> items) {
        if (items == null) return this;
        items.forEach(this::unorderedListItem);
        sb.append("\n");
        return this;
    }

    public MarkdownBuilder unorderedListKVItem(String key, String value) {
        sb.append("- ").append(bold(key)).append(": ").append(value).append("\n");
        return this;
    }

    public MarkdownBuilder unorderedListKVItem(String[] keys, String[] values) {
        if (keys == null || values == null) return this;
        if (keys.length != values.length) throw new IllegalArgumentException("Arrays have different sizes");
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            s.append("  ").append(keys[i]).append(": ").append(values[i]);
            s.append("\n");
        }
        return unorderedListItem(s.toString().trim());
    }

    public MarkdownBuilder unorderedListKVItem(Map<String, String> items) {
        if (items == null) return this;
        StringBuilder s = new StringBuilder();
        items.forEach((key, value) -> s.append("  ").append(key).append(": ").append(value).append("\n"));
        return unorderedListItem(s.toString().trim());
    }

    public MarkdownBuilder unorderedListKVItem(Object serializableItems, List<String> keys) {
        return unorderedListKVItem(castMapValues(mapper.convertValue(serializableItems, new TypeReference<>() {}), keys));
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
        if (rowItems == null) return this;
        sb.append("| ").append(String.join(" | ", rowItems)).append(" |\n");
        return this;
    }

    public MarkdownBuilder tableRow(String[] rowItems) {
        if (rowItems == null || rowItems.length == 0) return this;
        return tableRow(Arrays.asList(rowItems));
    }

    public MarkdownBuilder tableRow(Object serializableItems, List<String> keys) {
        if (serializableItems == null) return this;
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
        if (items == null) return this;
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
        sb.append(content);
        return this;
    }

    @Override
    public String toString() {
        return sb.toString().trim() + "\n";
    }
}
