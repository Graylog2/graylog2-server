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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MarkdownBuilder {
    private final StringBuilder sb;
    private final ObjectMapper mapper;

    public MarkdownBuilder() {
        this.sb = new StringBuilder();
        this.mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new JodaModule());
    }

    public boolean isEmpty() {
        return sb.isEmpty();
    }

    public MarkdownBuilder h1(String text) {
        sb.append("# ").append(text).append("\n\n");
        return this;
    }

    public MarkdownBuilder h2(String text) {
        sb.append("## ").append(text).append("\n\n");
        return this;
    }

    public MarkdownBuilder h3(String text) {
        sb.append("### ").append(text).append("\n\n");
        return this;
    }

    public MarkdownBuilder h4(String text) {
        sb.append("#### ").append(text).append("\n\n");
        return this;
    }

    public MarkdownBuilder h5(String text) {
        sb.append("##### ").append(text).append("\n\n");
        return this;
    }

    public MarkdownBuilder h6(String text) {
        sb.append("###### ").append(text).append("\n\n");
        return this;
    }

    public MarkdownBuilder paragraph(String text) {
        sb.append(text).append("\n\n");
        return this;
    }

    public static String bold(String text) {
        return "**" + text + "**";
    }

    public static String italic(String text) {
        return "*" + text + "*";
    }

    public static String code(String text) {
        return "`" + text + "`";
    }

    public static String link(String text, String url) {
        return "[" + text + "](" + url + ")";
    }

    public MarkdownBuilder codeBlock(String code) {
        sb.append("```\n").append(code).append("\n```\n\n");
        return this;
    }

    public MarkdownBuilder codeBlock(String code, String language) {
        sb.append("```").append(language).append("\n")
                .append(code).append("\n```\n\n");
        return this;
    }

    public MarkdownBuilder unorderedList(String... items) {
        for (String item : items) {
            sb.append("- ").append(item).append("\n");
        }
        sb.append("\n");
        return this;
    }

    public MarkdownBuilder unorderedListItem(String item) {
        sb.append("- ").append(item).append("\n");
        return this;
    }

    public MarkdownBuilder unorderedListKVItem(String[] keys, String[] values) {
        if (keys.length != values.length) {
            throw new IllegalArgumentException("Arrays have different sizes");
        }
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            s.append("  ").append(bold(keys[i])).append(": ").append(values[i]);
            s.append("\n");
        }
        return unorderedListItem(s.toString().trim());
    }

    public MarkdownBuilder unorderedListKVItem(Map<String, String> items) {
        StringBuilder s = new StringBuilder();
        items.forEach((key, value) -> s.append("  ").append(key).append(": ").append(value).append("\n"));
        return unorderedListItem(s.toString().trim());
    }

    public MarkdownBuilder unorderedListKVItemFromSerializable(Object serializableItems, String[] keys) {
        return unorderedListKVItem(castMapValues(
                mapper.convertValue(serializableItems, new TypeReference<>() {}),
                keys == null ? null : Set.of(keys)
        ));
    }

    public static Map<String, String> castMapValues(Map<String, Object> items, Set<String> keys) {
        return items.entrySet().stream()
                .filter(e -> keys == null || keys.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() == null ? "null" : e.getValue().toString().trim().replace("\n", " ")));
    }

    public static Map<String, String> castMapValues(Map<String, Object> items) {
        return castMapValues(items, null);
    }

    public MarkdownBuilder orderedList(String... items) {
        for (int i = 0; i < items.length; i++) {
            sb.append(i + 1).append(". ").append(items[i]).append("\n");
        }
        sb.append("\n");
        return this;
    }

    public MarkdownBuilder blockquote(String text) {
        String[] lines = text.split("\n");
        for (String line : lines) {
            sb.append("> ").append(line).append("\n");
        }
        sb.append("\n");
        return this;
    }

    public enum Alignment {
        LEFT, CENTER, RIGHT
    }

    public MarkdownBuilder tableRow(String[] row) {
        sb.append("| ").append(String.join(" | ", row)).append(" |\n");
        return this;
    }

    public MarkdownBuilder tableRow(Map<String, String> items) {
        sb.append("| ").append(String.join(" | ", items.values())).append(" |\n");
        return this;
    }

    public MarkdownBuilder tableRow(Map<String, String> items, String[] orderedKeys) {
        if  (orderedKeys == null || orderedKeys.length == 0) {
            return tableRow(items);
        }
        List<String> orderedValues = new ArrayList<>();
        for (String key : orderedKeys) {
            if (items.containsKey(key)) {
                orderedValues.add(items.get(key));
            }
        }
        sb.append("| ").append(String.join(" | ", orderedValues)).append(" |\n");
        return this;
    }

    public MarkdownBuilder tableRowFromSerializable(Object serializableItems, String[] keys) {
        return tableRow(castMapValues(
                mapper.convertValue(serializableItems, new TypeReference<>() {}),
                keys == null ? null : Set.of(keys)
        ), keys);
    }

    public MarkdownBuilder table(String[] headers) {
        return table(headers, null);
    }

    public MarkdownBuilder table(String[] headers, String[][] rows) {
        sb.append("| ").append(String.join(" | ", headers)).append(" |\n");
        sb.append("|");
        sb.append(" --- |".repeat(headers.length));
        sb.append("\n");

        if (rows != null && rows.length > 0) {
            for (String[] row : rows) {
                if (row != null && row.length == headers.length) {
                    sb.append("| ").append(String.join(" | ", row)).append(" |\n");
                }
            }
            sb.append("\n");
        }

        return this;
    }

    public MarkdownBuilder table(Map<String, String> items) {
        return table(items.keySet().toArray(String[]::new), new String[][]{items.values().toArray(String[]::new)});
    }

    public MarkdownBuilder table(String[] headers, Alignment[] alignments, String[][] rows) {
        sb.append("| ").append(String.join(" | ", headers)).append(" |\n");
        sb.append("|");
        for (Alignment alignment : alignments) {
            switch (alignment) {
                case LEFT:
                    sb.append(" :--- |");
                    break;
                case CENTER:
                    sb.append(" :---: |");
                    break;
                case RIGHT:
                    sb.append(" ---: |");
                    break;
            }
        }
        sb.append("\n");

        for (String[] row : rows) {
            sb.append("| ").append(String.join(" | ", row)).append(" |\n");
        }
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
