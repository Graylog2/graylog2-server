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

import org.graylog2.indexer.datanode.ProxyRequestAdapter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ProxyRequestAdapterES7 implements ProxyRequestAdapter {
    private static final String ERROR_MESSAGE = "This functionality is only available when the Data Node is used.";

    @Override
    public ProxyResponse request(ProxyRequest request) throws IOException {
        return new ProxyResponse(400, new ByteArrayInputStream(ERROR_MESSAGE.getBytes(StandardCharsets.UTF_8)));
    }
}
