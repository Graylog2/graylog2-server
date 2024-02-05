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
package org.graylog.storage.opensearch2.testing;

import org.graylog.testing.completebackend.SearchServerBuilder;
import org.graylog.testing.completebackend.SearchServerInterfaceProvider;
import org.graylog2.storage.SearchVersion;

import static org.graylog2.storage.SearchVersion.Distribution.DATANODE;

public class DatanodeInstanceProvider implements SearchServerInterfaceProvider {
    @Override
    public SearchServerBuilder getBuilderFor(final SearchVersion version) {
        if(version.satisfies(DATANODE, "^5.1.0")) {
            return new DatanodeInstanceBuilder(version);
        }
        return null;
    }
}
