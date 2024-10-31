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
package org.graylog.plugins.views.search.searchtypes.export;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.google.common.collect.ImmutableList;
import org.bson.Document;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog.plugins.views.search.util.ListOfStringsComparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public record ExportTabularResultResponse(@JsonProperty List<String> header,
                                          @JsonProperty @JacksonXmlElementWrapper(useWrapping = false) List<DataRow> dataRows) {

    public record DataRow(@JacksonXmlElementWrapper(useWrapping = false) List<Object> row) {

    }

    public static ExportTabularResultResponse fromPivotResult(final PivotResult pivotResult) {

        final Collection<PivotResult.Row> rows = pivotResult.rows();

        final int longestRowKey = rows.stream()
                .mapToInt(row -> row.key().size())
                .max()
                .orElse(0);

        final List<ImmutableList<String>> columns = rows.stream()
                .flatMap(row -> row.values()
                        .stream()
                        .map(PivotResult.Value::key)
                )
                .distinct()
                .sorted(new ListOfStringsComparator())
                .toList();

        final List<String> header = new ArrayList<>(longestRowKey + columns.size());
        header.addAll(Collections.nCopies(longestRowKey, ""));
        columns.forEach(column -> header.add(column.toString()));

        final List<DataRow> dataRows = rows.stream()
                .filter(row -> "leaf".equals(row.source()))
                .map(row -> {
                    final ImmutableList<String> key = row.key();
                    final List<Object> values = columns.stream()
                            .map(metric -> row.values()
                                    .stream()
                                    .filter(value -> value.key().equals(metric))
                                    .filter(value -> value.value() != null)
                                    .findFirst()
                                    .map(value -> value.value())
                                    .orElse(null)
                            ).toList();

                    List<Object> dataRow = new ArrayList<>();
                    dataRow.addAll(key);
                    dataRow.addAll(values);
                    return new DataRow(dataRow);
                })
                .toList();

        return new ExportTabularResultResponse(header, dataRows);
    }

    public static ExportTabularResultResponse fromMessageListResult(final MessageList.Result m) {
        if(m.messages().isEmpty()) {
            return new ExportTabularResultResponse(List.of(), List.of());
        }

        final var first = m.messages().get(0);

        final var header = first.message().keySet().stream().toList();
        final var rows = m.messages()
                            .stream()
                            .map(message ->
                                    new DataRow(header.stream().map(key -> message.message().get(key)).toList())
                            )
                            .toList();
        return new ExportTabularResultResponse(header, rows);
    }

    public static ExportTabularResultResponse fromDocumentList(final List<Document> documents,
                                                               final List<String> exportedFields) {

        return new ExportTabularResultResponse(exportedFields,
                documents.stream()
                        .map(doc -> new DataRow(exportedFields.stream().map(doc::get).toList()))
                        .toList());

    }
}
