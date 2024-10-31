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
package org.graylog.storage.opensearch2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.shaded.opensearch2.org.apache.http.Header;
import org.graylog.shaded.opensearch2.org.apache.http.HttpEntity;
import org.graylog.shaded.opensearch2.org.apache.http.HttpHost;
import org.graylog.shaded.opensearch2.org.apache.http.ProtocolVersion;
import org.graylog.shaded.opensearch2.org.apache.http.RequestLine;
import org.graylog.shaded.opensearch2.org.apache.http.StatusLine;
import org.graylog.shaded.opensearch2.org.opensearch.OpenSearchStatusException;
import org.graylog.shaded.opensearch2.org.opensearch.client.Response;
import org.graylog.shaded.opensearch2.org.opensearch.client.ResponseException;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestHighLevelClient;
import org.graylog.shaded.opensearch2.org.opensearch.core.rest.RestStatus;
import org.graylog2.indexer.BatchSizeTooLargeException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OpenSearchExceptionTest {
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Response response;

    @Mock
    RestHighLevelClient restHighLevelClient;

    // Verify that an OpenSearchStatusException is translated to a BatchSizeTooLargeException.
    @Test
    public void handle429() throws IOException {
        when(response.getHost()).thenReturn(new HttpHost("myHost"));

        RequestLine requestLine = new RequestLine() {
            @Override
            public String getMethod() {
                return "POST";
            }

            @Override
            public ProtocolVersion getProtocolVersion() {
                return null;
            }

            @Override
            public String getUri() {
                return "http://myTest.com";
            }
        };
        when(response.getRequestLine()).thenReturn(requestLine);

        StatusLine statusLine = new StatusLine() {
            @Override
            public ProtocolVersion getProtocolVersion() {
                return null;
            }

            @Override
            public int getStatusCode() {
                return 429;
            }

            @Override
            public String getReasonPhrase() {
                return null;
            }
        };
        when(response.getStatusLine()).thenReturn(statusLine);

        HttpEntity httpEntity = new HttpEntity() {
            @Override
            public boolean isRepeatable() {
                return false;
            }

            @Override
            public boolean isChunked() {
                return false;
            }

            @Override
            public long getContentLength() {
                return 0;
            }

            @Override
            public Header getContentType() {
                return null;
            }

            @Override
            public Header getContentEncoding() {
                return null;
            }

            @Override
            public InputStream getContent() throws IOException, UnsupportedOperationException {
                return null;
            }

            @Override
            public void writeTo(OutputStream outputStream) throws IOException {

            }

            @Override
            public boolean isStreaming() {
                return false;
            }

            @Override
            public void consumeContent() throws IOException {

            }
        };
        when(response.getEntity()).thenReturn(httpEntity);

        ResponseException responseException = new ResponseException(response);
        RestStatus restStatus = RestStatus.BAD_REQUEST;
        OpenSearchStatusException statusException = new OpenSearchStatusException(
                "status msg", restStatus, responseException);
        final OpenSearchClient openSearchClient = new OpenSearchClient(restHighLevelClient, new ObjectMapper());

        Exception exception = assertThrows(BatchSizeTooLargeException.class, () -> {
            openSearchClient.execute((a, b) -> {throw statusException;});
        });
    }
}
