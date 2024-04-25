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
package org.graylog.datanode.docs.printers;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.graylog.datanode.docs.ConfigurationEntry;

import java.io.IOException;
import java.io.OutputStreamWriter;

public class CsvDocsPrinter implements DocsPrinter {

    public static final String HEADER_PARAMETER = "Parameter";
    public static final String HEADER_TYPE = "Type";
    public static final String HEADER_REQUIRED = "Required";
    public static final String HEADER_DEFAULT_VALUE = "Default value";
    public static final String HEADER_DESCRIPTION = "Description";
    public static final String[] HEADERS = {HEADER_PARAMETER, HEADER_TYPE, HEADER_REQUIRED, HEADER_DEFAULT_VALUE, HEADER_DESCRIPTION};
    private final CSVPrinter csvPRinter;

    public CsvDocsPrinter(OutputStreamWriter streamWriter) throws IOException {
        this.csvPRinter = new CSVPrinter(streamWriter, CSVFormat.EXCEL);
    }

    @Override
    public void writeHeader() throws IOException {
        csvPRinter.printRecord(HEADERS);
    }

    @Override
    public void writeField(ConfigurationEntry f) throws IOException {
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
