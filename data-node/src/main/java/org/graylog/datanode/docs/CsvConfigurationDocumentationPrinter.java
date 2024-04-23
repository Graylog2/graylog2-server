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
package org.graylog.datanode.docs;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.OutputStreamWriter;

public class CsvConfigurationDocumentationPrinter implements ConfigurationDocumentationPrinter {

    private final CSVPrinter csvPRinter;

    public CsvConfigurationDocumentationPrinter(OutputStreamWriter streamWriter) {
        try {
            this.csvPRinter = new CSVPrinter(streamWriter, CSVFormat.EXCEL);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void writeHeader() throws IOException {
        csvPRinter.printRecord("Parameter", "Type", "Required", "Default value", "Description");
    }

    @Override
    public void writeField(ConfigurationField f) throws IOException {
        this.csvPRinter.printRecord(f.configName(), f.type(), f.required(), f.defaultValue(), f.documentation());
    }

    @Override
    public void close() throws IOException {
        csvPRinter.close();
    }

    @Override
    public void flush() throws IOException {
        csvPRinter.flush();
    }
}
