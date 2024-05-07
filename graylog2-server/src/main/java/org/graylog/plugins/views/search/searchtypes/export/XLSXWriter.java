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

import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;

public class XLSXWriter {
    public static void writeXlsx(final ExportTabularResultResponse widgetExportResponse,
                                final OutputStream outputStream) throws IOException {
        try (var wb = new XSSFWorkbook()) {
            createWorksheetFor(wb, "export", widgetExportResponse);
            wb.write(outputStream);
        }
    }

    public static void createWorksheetFor(final XSSFWorkbook wb, final String sheetName, final ExportTabularResultResponse data) {
        final var sheet = wb.createSheet(sheetName);

        // write header
        var row = sheet.createRow(0);
        for(int c = 0; c < data.header().size(); c++) {
            var cell = row.createCell(c);
            cell.setCellValue(data.header().get(c));
        }

        // write data
        for(int r = 0; r < data.dataRows().size(); r++) {
            row = sheet.createRow(r + 1);
            final var rowData = data.dataRows().get(r).row();
            // TODO: change to switch statement with Java 21
            for(int c = 0; c < rowData.size(); c++) {
                final var cell = row.createCell(c);
                final var rawData = rowData.get(c);
                if(rawData instanceof Integer i) {
                    cell.setCellValue(i);
                } else if(rawData instanceof Double d) {
                    cell.setCellValue(d);
                } else if(rawData instanceof Boolean b) {
                    cell.setCellValue(b);
                } if(rawData instanceof Date d) {
                    cell.setCellValue(d);
                } if(rawData instanceof LocalDateTime ldt) {
                    cell.setCellValue(ldt);
                } if(rawData instanceof Calendar cal) {
                    cell.setCellValue(cal);
                } if(rawData instanceof String s) {
                    cell.setCellValue(s);
                } if(rawData instanceof RichTextString rts) {
                    cell.setCellValue(rts);
                } if(rawData instanceof LocalDate ld) {
                    cell.setCellValue(ld);
                } else {
                    cell.setCellValue(rawData.toString());
                }
            }
        }
    }
}
