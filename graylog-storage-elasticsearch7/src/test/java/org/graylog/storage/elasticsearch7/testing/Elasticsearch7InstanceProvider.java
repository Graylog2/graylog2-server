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
package org.graylog.storage.elasticsearch7.testing;

import org.graylog.testing.completebackend.SearchServerBuilder;
import org.graylog.testing.completebackend.SearchServerInterfaceProvider;
import org.graylog2.storage.SearchVersion;

import static org.graylog2.storage.SearchVersion.Distribution.ELASTICSEARCH;

public class Elasticsearch7InstanceProvider implements SearchServerInterfaceProvider {
    @Override
    public SearchServerBuilder getBuilderFor(final SearchVersion version) {
        if(version.satisfies(ELASTICSEARCH, "^7.0.0")) {
            return new Elasticsearch7InstanceBuilder(version);
        }
        return null;
    }
}
