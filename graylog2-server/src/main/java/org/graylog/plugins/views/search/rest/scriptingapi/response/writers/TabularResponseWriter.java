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
package org.graylog.plugins.views.search.rest.scriptingapi.response.writers;

import au.com.bytecode.opencsv.CSVWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vandermeer.asciitable.AsciiTable;
import org.graylog.plugins.views.search.rest.scriptingapi.response.ResponseSchemaEntry;
import org.graylog.plugins.views.search.rest.scriptingapi.response.TabularResponse;
import org.graylog2.rest.MoreMediaTypes;

import javax.inject.Inject;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Provider
@Produces({MediaType.APPLICATION_JSON, MoreMediaTypes.TEXT_CSV, MediaType.TEXT_PLAIN})
public class TabularResponseWriter implements MessageBodyWriter<TabularResponse> {

    private final ObjectMapper objectMapper;

    @Inject
    public TabularResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    @Override
    public boolean isWriteable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return aClass.equals(TabularResponse.class);
    }

    @Override
    public void writeTo(TabularResponse tabularResponse, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> multivaluedMap, OutputStream outputStream) throws IOException, WebApplicationException {
        switch (mediaType.toString()) {
            case MediaType.APPLICATION_JSON -> writeJson(tabularResponse, outputStream);
            case MediaType.TEXT_PLAIN -> writeAsciiTable(tabularResponse, outputStream);
            case MoreMediaTypes.TEXT_CSV -> writeCsv(tabularResponse, outputStream);
            default -> throw new IllegalArgumentException("Media type " + mediaType + " not supported");
        }
    }

    private void writeJson(TabularResponse tabularResponse, OutputStream outputStream) throws IOException {
        objectMapper.writeValue(outputStream, tabularResponse);
    }

    private void writeAsciiTable(TabularResponse response, OutputStream outputStream) {
        try (final PrintStream printStream = new PrintStream(outputStream, true, StandardCharsets.UTF_8)) {
            printStream.print(renderAsciiTable(response));
        }
    }

    private void writeCsv(TabularResponse response, OutputStream outputStream) throws IOException {
        try (final CSVWriter csvWriter = new CSVWriter(new PrintWriter(outputStream, true, StandardCharsets.UTF_8))) {
            // header
            csvWriter.writeNext(response.schema().stream().map(ResponseSchemaEntry::name).toArray(String[]::new));

            // rows
            response.datarows().stream()
                    .map(row -> row.stream().map(String::valueOf).toArray(String[]::new))
                    .forEach(csvWriter::writeNext);
        }
    }

    private static String renderAsciiTable(TabularResponse response) {
        AsciiTable at = new AsciiTable();
        at.getContext().setWidth(response.schema().size() * 25);
        at.addRule();
        at.addRow(response.schema().stream().map(ResponseSchemaEntry::name).collect(Collectors.toList()));
        at.addRule();
        response.datarows().forEach(at::addRow);
        at.addRule();
        return at.render();
    }
}
