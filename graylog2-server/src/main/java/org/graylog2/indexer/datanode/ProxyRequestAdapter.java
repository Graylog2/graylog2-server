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
package org.graylog2.indexer.datanode;

import jakarta.ws.rs.core.MultivaluedMap;

import java.io.IOException;
import java.io.InputStream;

public interface ProxyRequestAdapter {
    record ProxyRequest(String method, String path, InputStream body, String hostname,
                        MultivaluedMap<String, String> queryParameters) {}

    record ProxyResponse(int status, InputStream response, String contentType) {}

    ProxyResponse request(ProxyRequest request) throws IOException;
}
