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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.MediaType;
import org.graylog.plugins.views.search.engine.monitoring.data.histogram.Histogram;
import org.graylog.plugins.views.search.engine.monitoring.data.histogram.MultiValueBin;
import org.graylog.plugins.views.search.engine.monitoring.data.histogram.NamedBinDefinition;
import org.graylog2.rest.MoreMediaTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HistogramResponseWriterTest {

    @Mock
    ObjectMapper objectMapper;

    HistogramResponseWriter toTest;

    @BeforeEach
    void setUp() {
        toTest = new HistogramResponseWriter(objectMapper);
    }

    @Test
    void testJsonWrite() throws IOException {
        Histogram histogram = new Histogram(List.of(), List.of());
        OutputStream outputStream = mock(OutputStream.class);
        toTest.writeTo(histogram, Histogram.class, null, null, MediaType.APPLICATION_JSON_TYPE, null, outputStream);
        verify(objectMapper).writeValue(outputStream, histogram);
    }

    @Test
    void testCsvWrite() throws IOException {
        Histogram histogram = new Histogram(List.of("X", "Y"), List.of(
                new MultiValueBin<>(new NamedBinDefinition("x1"), List.of(13)),
                new MultiValueBin<>(new NamedBinDefinition("x2"), List.of(42))
        ));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        toTest.writeTo(histogram, Histogram.class, null, null, MoreMediaTypes.TEXT_CSV_TYPE, null, outputStream);
        final String csv = outputStream.toString(Charset.defaultCharset());

        assertEquals("""
                X,Y
                x1,13
                x2,42
                """, csv);
    }
}
