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
package org.graylog.testing.storage;

import com.github.zafarkhaja.semver.Version;
import org.graylog2.storage.SearchVersion;

public class SearchServer {
    private static final String DEFAULT_VERSION_STRING = "2.19.3";

    public static final SearchVersion DEFAULT_VERSION = SearchVersion.create(SearchVersion.Distribution.OPENSEARCH, Version.parse(DEFAULT_VERSION_STRING));

    private SearchServer() {
    }
}
