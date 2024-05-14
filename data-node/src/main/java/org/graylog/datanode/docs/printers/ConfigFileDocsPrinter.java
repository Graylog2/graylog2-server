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

import org.apache.commons.lang.WordUtils;
import org.graylog.datanode.docs.ConfigurationEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class ConfigFileDocsPrinter implements DocsPrinter {

    public static final String DATANODE_CONFIG_HEADER = """
            #####################################
            # GRAYLOG DATANODE CONFIGURATION FILE
            #####################################
            #
            # This is the Graylog DataNode configuration file. The file has to use ISO 8859-1/Latin-1 character encoding.
            # Characters that cannot be directly represented in this encoding can be written using Unicode escapes
            # as defined in https://docs.oracle.com/javase/specs/jls/se8/html/jls-3.html#jls-3.3, using the \\u prefix.
            # For example, \\u002c.
            #
            # * Entries are generally expected to be a single line of the form, one of the following:
            #
            # propertyName=propertyValue
            # propertyName:propertyValue
            #
            # * White space that appears between the property name and property value is ignored,
            #   so the following are equivalent:
            #
            # name=Stephen
            # name = Stephen
            #
            # * White space at the beginning of the line is also ignored.
            #
            # * Lines that start with the comment characters ! or # are ignored. Blank lines are also ignored.
            #
            # * The property value is generally terminated by the end of the line. White space following the
            #   property value is not ignored, and is treated as part of the property value.
            #
            # * A property value can span several lines if each line is terminated by a backslash (‘\\’) character.
            #   For example:
            #
            # targetCities=\\
            #         Detroit,\\
            #         Chicago,\\
            #         Los Angeles
            #
            #   This is equivalent to targetCities=Detroit,Chicago,Los Angeles (white space at the beginning of lines is ignored).
            #
            # * The characters newline, carriage return, and tab can be inserted with characters \\n, \\r, and \\t, respectively.
            #
            # * The backslash character must be escaped as a double backslash. For example:
            #
            # path=c:\\\\docs\\\\doc1
            #


            """;

    private static final Logger LOG = LoggerFactory.getLogger(ConfigFileDocsPrinter.class);

    private final OutputStreamWriter writer;

    public ConfigFileDocsPrinter(OutputStreamWriter writer) {
        this.writer = writer;
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }


    public void writeHeader() throws IOException {
        writer.append(DATANODE_CONFIG_HEADER);
    }

    @Override
    public void writeField(ConfigurationEntry field) throws IOException {
        writer.append(toString(field));
    }

    private String toString(ConfigurationEntry field) {
        // enabled with empty value, force to fill in
        final boolean forceFillIn = field.required() && field.defaultValue() == null;


        String template = """
                %s
                %s%s = %s

                """;

        return String.format(Locale.ROOT, template, formatDocumentation(field), forceFillIn ? "" : "#", field.configName(), wrapValue(field.defaultValue()));
    }

    private String wrapValue(Object value) {
        return Optional.ofNullable(value).map(String::valueOf).orElse("");
    }

    private static String formatDocumentation(ConfigurationEntry field) {
        final String[] lines = field.documentation().split("\n");
        return Arrays.stream(lines)
                .map(String::trim)
                .peek(line -> {
                    if (line.length() > 120) {
                        LOG.warn("Documentation line of " + field.configurationBean().getName() + "." + field.fieldName() + " too long, consider splitting into more lines: " + WordUtils.abbreviate(line, 120, 130, "..."));
                    }
                })
                .map(line -> "# " + line)
                .collect(Collectors.joining("\n"));
    }
}
