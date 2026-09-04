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

import au.com.bytecode.opencsv.CSVWriter;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.IOException;
import java.io.StringWriter;
import java.util.stream.Collector;

public class OutputBuilder extends MarkdownBuilder {
    public OutputBuilder() {
        super();
    }

    public static Collector<String[], ?, String> toCsvString(String[] headers) {
        return Collector.of(
                () -> {
                    StringWriter sw = new StringWriter();
                    if (headers != null && headers.length > 0) {
                        try (CSVWriter writer = new CSVWriter(sw)) {
                            writer.writeNext(headers);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return sw;
                },
                (sw, row) -> {
                    try (CSVWriter writer = new CSVWriter(sw)) {
                        writer.writeNext(row);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                (sw1, sw2) ->  {
                    sw1.write(sw2.toString());
                    return sw1;
                },
                StringWriter::toString
        );
    }

    public enum OutputFormat {
        @JsonProperty("list")
        @JsonAlias({"LIST", "List"})
        LIST,
        @JsonProperty("table")
        @JsonAlias({"TABLE", "Table"})
        TABLE,
        @JsonProperty("csv")
        @JsonAlias({"CSV", "Csv"})
        CSV
    }
}
