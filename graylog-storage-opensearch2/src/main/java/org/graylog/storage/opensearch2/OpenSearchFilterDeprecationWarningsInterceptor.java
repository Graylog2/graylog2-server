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

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.util.Arrays;

// TODO: check, if these messages still need filtering
public class OpenSearchFilterDeprecationWarningsInterceptor implements HttpResponseInterceptor {
    private final String[] messagesToFilter = {
            "setting was deprecated in OpenSearch",
            "but in a future major version, direct access to system indices and their aliases will not be allowed",
            "but in a future major version, direct access to system indices will be prevented by default",
            "in epoch time formats is deprecated and will not be supported in the next major version of OpenSearch"
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
        final var warnings = Arrays.stream(response.getHeaders("Warning")).filter(header -> !this.isDeprecationMessage(header.getValue())).toList();
        response.removeHeaders("Warning");
        warnings.forEach(response::addHeader);
    }
}
