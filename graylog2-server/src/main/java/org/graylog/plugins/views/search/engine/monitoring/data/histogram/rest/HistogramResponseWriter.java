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
package org.graylog.plugins.views.search.engine.monitoring.data.histogram.rest;

import au.com.bytecode.opencsv.CSVWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import org.graylog.plugins.views.search.engine.monitoring.data.histogram.Histogram;
import org.graylog2.rest.MoreMediaTypes;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Provider
@Produces({MediaType.APPLICATION_JSON, MoreMediaTypes.TEXT_CSV})
public class HistogramResponseWriter implements MessageBodyWriter<Histogram> {

    private final ObjectMapper objectMapper;

    @Inject
    public HistogramResponseWriter(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isWriteable(final Class<?> aClass,
                               final Type type,
                               final Annotation[] annotations,
                               final MediaType mediaType) {
        return aClass.equals(Histogram.class);
    }

    @Override
    public void writeTo(final Histogram histogram,
                        final Class<?> aClass,
                        final Type type,
                        final Annotation[] annotations,
                        final MediaType mediaType,
                        final MultivaluedMap<String, Object> multivaluedMap,
                        OutputStream outputStream) throws IOException, WebApplicationException {
        switch (mediaType.toString()) {
            case MediaType.APPLICATION_JSON -> writeJson(histogram, outputStream);
            case MoreMediaTypes.TEXT_CSV -> writeCsv(histogram, outputStream);
            default -> throw new IllegalArgumentException("Media type " + mediaType + " not supported");
        }
    }

    private void writeJson(final Histogram histogram, OutputStream outputStream) throws IOException {
        objectMapper.writeValue(outputStream, histogram);
    }

    private void writeCsv(final Histogram histogram, OutputStream outputStream) throws IOException {
        try (final CSVWriter csvWriter = new CSVWriter(new PrintWriter(outputStream, true, StandardCharsets.UTF_8),
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.NO_QUOTE_CHARACTER)) {

            //schema
            final List<String> schema = histogram.schema();
            csvWriter.writeNext(schema.toArray(new String[0]));

            // rows
            histogram.bins().stream()
                    .map(bin -> bin.toDataLine().toArray(new String[0]))
                    .forEach(csvWriter::writeNext);
        }
    }
}
