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
package org.graylog.plugins.views.search.rest.export.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import org.graylog.plugins.views.search.searchtypes.export.CSVWriter;
import org.graylog.plugins.views.search.searchtypes.export.ExportTabularResultResponse;
import org.graylog.plugins.views.search.searchtypes.export.XLSXWriter;
import org.graylog2.rest.MoreMediaTypes;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
@Produces({MoreMediaTypes.TEXT_CSV,
        MediaType.APPLICATION_JSON,
        MoreMediaTypes.APPLICATION_YAML,
        MediaType.APPLICATION_XML,
        MoreMediaTypes.APPLICATION_XLS})
public class AggregationWidgetExportResponseWriter implements MessageBodyWriter<ExportTabularResultResponse> {

    private final ObjectMapper objectMapper;
    private final YAMLMapper yamlMapper;

    private final XmlMapper xmlMapper;

    @Inject
    public AggregationWidgetExportResponseWriter(final ObjectMapper objectMapper,
                                                 final YAMLMapper yamlMapper,
                                                 final XmlMapper xmlMapper) {
        this.objectMapper = objectMapper;
        this.yamlMapper = yamlMapper;
        this.xmlMapper = xmlMapper;
    }


    @Override
    public boolean isWriteable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return aClass.equals(ExportTabularResultResponse.class);
    }

    @Override
    public void writeTo(ExportTabularResultResponse widgetExportResponse, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> multivaluedMap, OutputStream outputStream) throws IOException, WebApplicationException {
        switch (mediaType.toString()) {
            case MoreMediaTypes.TEXT_CSV -> CSVWriter.writeCsv(widgetExportResponse, outputStream);
            case MediaType.APPLICATION_JSON -> objectMapper.writeValue(outputStream, widgetExportResponse);
            case MoreMediaTypes.APPLICATION_YAML -> yamlMapper.writeValue(outputStream, widgetExportResponse);
            case MediaType.APPLICATION_XML -> xmlMapper.writeValue(outputStream, widgetExportResponse);
            case MoreMediaTypes.APPLICATION_XLS -> XLSXWriter.writeXlsx(widgetExportResponse, outputStream);
            default -> throw new IllegalArgumentException("Media type " + mediaType + " not supported");
        }
    }
}
