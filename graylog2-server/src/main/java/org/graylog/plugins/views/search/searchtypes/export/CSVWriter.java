package org.graylog.plugins.views.search.searchtypes.export;

import org.graylog.plugins.views.search.searchtypes.export.ExportTabularResultResponse;

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
