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
package org.graylog.testing.containermatrix;

import org.graylog2.storage.SearchVersion;

import static org.graylog2.storage.SearchVersion.Distribution.ELASTICSEARCH;
import static org.graylog2.storage.SearchVersion.Distribution.OPENSEARCH;

public enum SearchServer {
    ES7(ELASTICSEARCH, "7.10.2"),
    ES6(ELASTICSEARCH, "6.8.4"),
    OS1(OPENSEARCH, "1.3.1");

    public static final SearchServer DEFAULT_VERSION = OS1;

    private final SearchVersion searchVersion;

    SearchServer(SearchVersion.Distribution distribution, String version) {
        this.searchVersion = SearchVersion.create(distribution, com.github.zafarkhaja.semver.Version.valueOf(version));
    }

    public SearchVersion getSearchVersion() {
        return searchVersion;
    }
}
