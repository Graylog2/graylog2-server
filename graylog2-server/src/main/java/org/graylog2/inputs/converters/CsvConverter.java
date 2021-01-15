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
package org.graylog2.inputs.converters;

import au.com.bytecode.opencsv.CSVParser;
import com.google.common.collect.Maps;
import org.graylog2.ConfigurationException;
import org.graylog2.plugin.inputs.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import static com.google.common.base.MoreObjects.firstNonNull;

public class CsvConverter extends Converter {
    private static final Logger LOG = LoggerFactory.getLogger(CsvConverter.class);

    private final String[] fieldNames;

    private final char separator;
    private final char quoteChar;
    private final char escapeChar;
    private final boolean strictQuotes;
    private final boolean trimLeadingWhiteSpace;

    public CsvConverter(Map<String, Object> config) throws ConfigurationException {
        super(Type.CSV, config);
        try {
            String columnHeader = (String) config.get("column_header");
            if (columnHeader == null || columnHeader.isEmpty()) {
                throw new ConfigurationException("Missing column headers.");
            }
            separator = firstCharOrDefault(config.get("separator"), CSVParser.DEFAULT_SEPARATOR);
            quoteChar = firstCharOrDefault(config.get("quote_char"), CSVParser.DEFAULT_QUOTE_CHARACTER);
            escapeChar = firstCharOrDefault(config.get("escape_char"), CSVParser.DEFAULT_ESCAPE_CHARACTER);
            strictQuotes = firstNonNull((Boolean) config.get("strict_quotes"), false);
            trimLeadingWhiteSpace = firstNonNull((Boolean) config.get("trim_leading_whitespace"), true);

            final CSVParser parser = getCsvParser();
            fieldNames = parser.parseLine(columnHeader);
            if (fieldNames.length == 0) {
                throw new ConfigurationException("No field names found.");
            }
        } catch (Exception e) {
            throw new ConfigurationException("Invalid configuration for CsvConverter");
        }
    }

    @Override
    public Object convert(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        final CSVParser parser = getCsvParser();
        final Map<String, String> fields = Maps.newHashMap();
        try {
            final String[] strings = parser.parseLine(value);
            if (strings.length != fieldNames.length) {
                LOG.error("Different number of columns in CSV data ({}) and configured field names ({}). Discarding input.",
                          strings.length, fieldNames.length);
                return null;
            }
            for (int i = 0; i < strings.length; i++) {
                fields.put(fieldNames[i], strings[i]);
            }
        } catch (IOException e) {
            LOG.error("Invalid CSV input, discarding input", e);
            return null;
        }
        return fields;
    }

    private char firstCharOrDefault(Object configValue, char defaultValue) {
        if (configValue != null) {
            final String s = String.valueOf(configValue);
            if (s.length() > 0) {
                return s.charAt(0);
            }
        }
        return defaultValue;
    }

    private CSVParser getCsvParser() {
        // unfortunately CSVParser has state, so we have to re-create it every time :(
        return new CSVParser(separator,
                             quoteChar,
                             escapeChar,
                             strictQuotes,
                             trimLeadingWhiteSpace);
    }

    @Override
    public boolean buildsMultipleFields() {
        return true;
    }
}
