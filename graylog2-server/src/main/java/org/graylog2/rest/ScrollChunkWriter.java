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
package org.graylog2.rest;

import au.com.bytecode.opencsv.CSVWriter;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.results.ScrollResult;
import org.graylog2.plugin.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Provider
@Produces(MoreMediaTypes.TEXT_CSV)
public class ScrollChunkWriter implements MessageBodyWriter<ScrollResult.ScrollChunk> {
    private static final Logger LOG = LoggerFactory.getLogger(ScrollChunkWriter.class);

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return ScrollResult.ScrollChunk.class.isAssignableFrom(type) && MoreMediaTypes.TEXT_CSV_TYPE.isCompatible(mediaType);

    }

    @Override
    public long getSize(ScrollResult.ScrollChunk scrollChunk,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(ScrollResult.ScrollChunk scrollChunk,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("[{}] Writing chunk {}", Thread.currentThread().getId(), scrollChunk.getChunkNumber());
        }

        final List<String> fields = scrollChunk.getFields();
        final int numberOfFields = fields.size();

        try (CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(entityStream, StandardCharsets.UTF_8))) {
            if (scrollChunk.isFirstChunk()) {
                // write field headers only on first chunk
                csvWriter.writeNext(fields.toArray(new String[numberOfFields]));
            }
            // write result set in same order as the header row
            final String[] fieldValues = new String[numberOfFields];
            for (ResultMessage resultMessage : scrollChunk.getMessages()) {
                final Message message = resultMessage.getMessage();

                // first collect all values from the current message
                int idx = 0;
                for (String fieldName : fields) {
                    final Object val = message.getField(fieldName);
                    if (val == null) {
                        fieldValues[idx] = null;
                    } else {
                        String stringVal = val.toString();
                        // TODO: Maybe use StringEscapeUtils.escapeCsv(String) instead?
                        fieldValues[idx] = escapeNewlines(stringVal);
                    }
                    idx++;
                }

                // write the complete line, some fields might not be present in the message, so there might be null values
                csvWriter.writeNext(fieldValues);
            }
            if (csvWriter.checkError()) {
                LOG.error("Encountered unspecified error when writing message result as CSV, result is likely malformed.");
            }
        }
    }

    private String escapeNewlines(String s) {
        final StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            switch (c) {
                case '\n':
                    sb.append("\\\\n");
                    break;
                case '\r':
                    sb.append("\\\\r");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }
}
