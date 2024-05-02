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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class CSVWriter {
    public static void writeCsv(final ExportTabularResultResponse widgetExportResponse,
                                final OutputStream outputStream) throws IOException {
        try (final au.com.bytecode.opencsv.CSVWriter csvWriter = new au.com.bytecode.opencsv.CSVWriter(new PrintWriter(outputStream, true, StandardCharsets.UTF_8))) {
            csvWriter.writeNext(widgetExportResponse.header().toArray(new String[0]));
            for (ExportTabularResultResponse.DataRow row : widgetExportResponse.dataRows()) {
                csvWriter.writeNext(row.row().stream().map(obj -> obj == null ? "" : obj.toString()).toList().toArray(new String[0]));
            }
        }
    }
}
