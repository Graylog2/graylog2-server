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
package org.graylog2.i18n.feedback;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Translation reports grouped as {@code language → namespace → key → reports}, which is how the locale files are organized.
 * Texts which aren't translated at all are grouped under the namespace {@value #UNTRANSLATED} by their text.
 */
public record TranslationFeedbackExport(@JsonProperty("generated_at") DateTime generatedAt,
                                        @JsonProperty("total") int total,
                                        @JsonProperty("languages") Map<String, Map<String, Map<String, List<Entry>>>> languages) {
    public static final String UNTRANSLATED = "_untranslated";

    public record Entry(@JsonProperty("id") String id,
                        @JsonProperty("status") String status,
                        @JsonProperty("resolution") @Nullable String resolution,
                        @JsonProperty("category") String category,
                        @JsonProperty("text") String text,
                        @JsonProperty("english_text") @Nullable String englishText,
                        @JsonProperty("suggestion") @Nullable String suggestion,
                        @JsonProperty("comment") @Nullable String comment,
                        @JsonProperty("reporter") String reporter,
                        @JsonProperty("path") @Nullable String path,
                        @JsonProperty("dom_path") @Nullable String domPath,
                        @JsonProperty("created_at") DateTime createdAt) {
        static Entry of(TranslationFeedbackDTO feedback) {
            return new Entry(feedback.id(), feedback.status(), feedback.resolution(), feedback.category(), feedback.text(), feedback.englishText(),
                    feedback.suggestion(), feedback.comment(), feedback.reporter(), feedback.path(), feedback.domPath(),
                    feedback.createdAt());
        }
    }

    public static TranslationFeedbackExport of(List<TranslationFeedbackDTO> reports, DateTime generatedAt) {
        final Map<String, Map<String, Map<String, List<Entry>>>> languages = new TreeMap<>();

        reports.forEach(report -> {
            final String namespace = report.isUntranslated() ? UNTRANSLATED : report.namespace();
            final String key = report.isUntranslated() ? report.text() : report.key();

            languages.computeIfAbsent(report.language(), language -> new TreeMap<>())
                    .computeIfAbsent(namespace, ns -> new TreeMap<>())
                    .computeIfAbsent(key, k -> new ArrayList<>())
                    .add(Entry.of(report));
        });

        return new TranslationFeedbackExport(generatedAt, reports.size(), languages);
    }

    private static final List<String> CSV_HEADER = List.of("id", "language", "namespace", "key", "status", "resolution",
            "category", "text", "english_text", "suggestion", "comment", "reporter", "path", "dom_path", "created_at");

    public static String toCsv(List<TranslationFeedbackDTO> reports) {
        final StringBuilder csv = new StringBuilder();
        appendRow(csv, CSV_HEADER.stream());

        reports.forEach(report -> appendRow(csv, Stream.of(report.id(), report.language(), report.namespace(), report.key(),
                report.status(), report.resolution(), report.category(), report.text(), report.englishText(), report.suggestion(), report.comment(),
                report.reporter(), report.path(), report.domPath(),
                report.createdAt() == null ? null : report.createdAt().toString())));

        return csv.toString();
    }

    private static void appendRow(StringBuilder csv, Stream<String> values) {
        csv.append(String.join(",", values.map(TranslationFeedbackExport::escapeCsv).toList())).append("\r\n");
    }

    static String escapeCsv(@Nullable String value) {
        if (value == null) {
            return "";
        }

        // Prevents spreadsheet applications from interpreting texts as formulas.
        final String safe = !value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0 ? "'" + value : value;

        return safe.matches("(?s).*[\",\r\n].*") || !safe.equals(value) || safe.startsWith(" ") || safe.endsWith(" ")
                ? "\"" + safe.replace("\"", "\"\"") + "\""
                : safe;
    }
}
