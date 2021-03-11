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

import org.graylog.shaded.elasticsearch7.org.apache.http.Header;
import org.graylog.shaded.elasticsearch7.org.apache.http.HttpException;
import org.graylog.shaded.elasticsearch7.org.apache.http.HttpResponse;
import org.graylog.shaded.elasticsearch7.org.apache.http.HttpResponseInterceptor;
import org.graylog.shaded.elasticsearch7.org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ElasticsearchFilterDeprecationWarningsInterceptor implements HttpResponseInterceptor {
    private String[] messagesToFilter = {
            "setting was deprecated in Elasticsearch",
            "but in a future major version, directaccess to system indices and their aliases will not be allowed",
            "in epoch time formats is deprecated and will not be supported in the next major version of Elasticsearch",
            org.graylog.shaded.elasticsearch7.org.elasticsearch.common.joda.JodaDeprecationPatterns.USE_NEW_FORMAT_SPECIFIERS
    };

    private boolean isDeprecationMessage(final String message) {
        for(String msg: messagesToFilter) {
            if(message.contains(msg)) return true;
        }
        return false;
    }

    @Override
    public void process(HttpResponse response, HttpContext context) throws
            HttpException, IOException {
        List<Header> warnings = Arrays.stream(response.getHeaders("Warning")).filter(header -> !this.isDeprecationMessage(header.getValue())).collect(Collectors.toList());
        response.removeHeaders("Warning");
        warnings.stream().forEach(header -> response.addHeader(header));
    }
}
