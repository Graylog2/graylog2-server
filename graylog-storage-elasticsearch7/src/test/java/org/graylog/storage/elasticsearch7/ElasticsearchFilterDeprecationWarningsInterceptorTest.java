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
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class ElasticsearchFilterDeprecationWarningsInterceptorTest {
    @Test
    public void testInterceptorNoHeader() throws IOException, HttpException {
        ElasticsearchFilterDeprecationWarningsInterceptor interceptor = new ElasticsearchFilterDeprecationWarningsInterceptor();

        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 0, 0), 0, null));
        interceptor.process(response, null);

        Assert.assertEquals( "Number of Headers should be 0 and the interceptor should not fail in itself.", 0, response.getAllHeaders().length);
    }

    @Test
    public void testInterceptorSingleHeader() throws IOException, HttpException {
        ElasticsearchFilterDeprecationWarningsInterceptor interceptor = new ElasticsearchFilterDeprecationWarningsInterceptor();

        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 0, 0), 0, null));
        response.addHeader("Test", "This header should not trigger the interceptor.");
        interceptor.process(response, null);

        Assert.assertEquals( "Number of Headers should be unchanged.", 1, response.getAllHeaders().length);
        Assert.assertEquals( "Remaining Header should be same as the given.", "Test", response.getAllHeaders()[0].getName());
    }

    @Test
    public void testInterceptorMultipleHeaderIgnoredWarning() throws IOException, HttpException {
        ElasticsearchFilterDeprecationWarningsInterceptor interceptor = new ElasticsearchFilterDeprecationWarningsInterceptor();

        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 0, 0), 0, null));
        response.addHeader("Test", "This header should not trigger the interceptor.");
        response.addHeader("Warning", "This warning should not trigger the interceptor.");
        interceptor.process(response, null);

        Assert.assertEquals( "Number of Headers should be unchanged.", 2, response.getAllHeaders().length);
    }

    @Test
    public void testInterceptorMultipleHeaderFilteredWarning() throws IOException, HttpException {
        ElasticsearchFilterDeprecationWarningsInterceptor interceptor = new ElasticsearchFilterDeprecationWarningsInterceptor();

        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 0, 0), 0, null));
        response.addHeader("Test", "This header should not trigger the interceptor.");
        response.addHeader("Warning", "This warning should not trigger the interceptor.");
        response.addHeader("Warning", "This text contains the trigger: setting was deprecated in Elasticsearch - and should be filtered out");

        Assert.assertEquals( "Number of Headers should be 3 before start.", 3, response.getAllHeaders().length);

        interceptor.process(response, null);

        Assert.assertEquals( "Number of Headers should be 1 less after running the interceptor.", 2, response.getAllHeaders().length);
    }
}
