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
package org.graylog.storage.elasticsearch7;

import org.graylog.shaded.elasticsearch7.org.apache.http.HttpException;
import org.graylog.shaded.elasticsearch7.org.apache.http.HttpResponse;
import org.graylog.shaded.elasticsearch7.org.apache.http.ProtocolVersion;
import org.graylog.shaded.elasticsearch7.org.apache.http.message.BasicHttpResponse;
import org.graylog.shaded.elasticsearch7.org.apache.http.message.BasicStatusLine;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class ElasticsearchFilterDeprecationWarningsInterceptorTest {
    @Test
    public void testInterceptorNoHeader() throws IOException, HttpException {
        ElasticsearchFilterDeprecationWarningsInterceptor interceptor = new ElasticsearchFilterDeprecationWarningsInterceptor();

        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 0, 0), 0, null));
        interceptor.process(response, null);

        assertThat(response.getAllHeaders())
                .as("Number of Headers should be 0 and the interceptor should not fail in itself.")
                .hasSize(0);
    }

    @Test
    public void testInterceptorSingleHeader() throws IOException, HttpException {
        ElasticsearchFilterDeprecationWarningsInterceptor interceptor = new ElasticsearchFilterDeprecationWarningsInterceptor();

        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 0, 0), 0, null));
        response.addHeader("Test", "This header should not trigger the interceptor.");
        interceptor.process(response, null);

        assertThat(response.getAllHeaders())
                .as("Number of Headers should be unchanged.")
                .hasSize(1);

        assertThat(response.getAllHeaders()[0].getName())
                .as("Remaining Header should be same as the given.")
                .isEqualTo("Test");
    }

    @Test
    public void testInterceptorMultipleHeaderIgnoredWarning() throws IOException, HttpException {
        ElasticsearchFilterDeprecationWarningsInterceptor interceptor = new ElasticsearchFilterDeprecationWarningsInterceptor();

        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 0, 0), 0, null));
        response.addHeader("Test", "This header should not trigger the interceptor.");
        response.addHeader("Warning", "This warning should not trigger the interceptor.");
        interceptor.process(response, null);

        assertThat(response.getAllHeaders())
                .as("Number of Headers should be unchanged.")
                .hasSize(2);
    }

    @Test
    public void testInterceptorMultipleHeaderFilteredWarning() throws IOException, HttpException {
        ElasticsearchFilterDeprecationWarningsInterceptor interceptor = new ElasticsearchFilterDeprecationWarningsInterceptor();

        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 0, 0), 0, null));
        response.addHeader("Test", "This header should not trigger the interceptor.");
        response.addHeader("Warning", "This warning should not trigger the interceptor.");
        response.addHeader("Warning", "This text contains the trigger: setting was deprecated in Elasticsearch - and should be filtered out");

        assertThat(response.getAllHeaders())
                .as("Number of Headers should be 3 before start.")
                .hasSize(3);

        interceptor.process(response, null);

        assertThat(response.getAllHeaders())
                .as("Number of Headers should be 1 less after running the interceptor.")
                .hasSize(2);
    }

    @Test
    public void testInterceptorMultipleHeaderFilteredWarning2() throws IOException, HttpException {
        ElasticsearchFilterDeprecationWarningsInterceptor interceptor = new ElasticsearchFilterDeprecationWarningsInterceptor();

        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 0, 0), 0, null));
        response.addHeader("Test", "This header should not trigger the interceptor.");
        response.addHeader("Warning", "This warning should not trigger the interceptor.");
        response.addHeader("Warning", "This text contains the trigger: but in a future major version, directaccess to system indices and their aliases will not be allowed - and should be filtered out");

        assertThat(response.getAllHeaders())
                .as("Number of Headers should be 3 before start.")
                .hasSize(3);

        interceptor.process(response, null);

        assertThat(response.getAllHeaders())
                .as("Number of Headers should be 1 less after running the interceptor.")
                .hasSize(2);
    }

    @Test
    public void testInterceptorMultipleHeaderFilteredWarningAndMultipleTriggers() throws IOException, HttpException {
        ElasticsearchFilterDeprecationWarningsInterceptor interceptor = new ElasticsearchFilterDeprecationWarningsInterceptor();

        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 0, 0), 0, null));
        response.addHeader("Test", "This header should not trigger the interceptor.");
        response.addHeader("Warning", "This warning should not trigger the interceptor.");
        response.addHeader("Warning", "This text contains the trigger: but in a future major version, directaccess to system indices and their aliases will not be allowed - and should be filtered out");
        response.addHeader("Warning", "This text contains the trigger: setting was deprecated in Elasticsearch - and should be filtered out");

        assertThat(response.getAllHeaders())
                .as("Number of Headers should be 4 before start.")
                .hasSize(4);

        interceptor.process(response, null);

        assertThat(response.getAllHeaders())
                .as("Number of Headers should be 2 less after running the interceptor.")
                .hasSize(2);
    }
}
