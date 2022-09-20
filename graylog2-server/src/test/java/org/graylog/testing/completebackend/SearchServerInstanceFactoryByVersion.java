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
package org.graylog.testing.completebackend;

import org.apache.commons.lang3.NotImplementedException;
import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.graylog2.storage.SearchVersion;
import org.testcontainers.containers.Network;

import java.lang.reflect.Method;

import static org.graylog2.storage.SearchVersion.Distribution.ELASTICSEARCH;
import static org.graylog2.storage.SearchVersion.Distribution.OPENSEARCH;

public class SearchServerInstanceFactoryByVersion implements SearchServerInstanceFactory {

    private final SearchVersion version;

    public SearchServerInstanceFactoryByVersion(SearchVersion searchVersion) {
        this.version = searchVersion;
    }

    @Override
    public SearchServerInstance create(Network network) {
        if (version.satisfies(ELASTICSEARCH, "^7.0.0")) {
            return doCreate("org.graylog.storage.elasticsearch7.testing.ElasticsearchInstanceES7", version, network);
        } else if (version.satisfies(OPENSEARCH, "^1.0.0")) {
            return doCreate("org.graylog.storage.elasticsearch7.testing.OpensearchInstance", version, network);
        } else if (version.satisfies(OPENSEARCH, "^2.0.0")) {
            return doCreate("org.graylog.storage.opensearch2.testing.OpenSearchInstance", version, network);
        } else {
            throw new NotImplementedException("Search version " + version + " not supported.");
        }
    }

    private SearchServerInstance doCreate(final String cName, final SearchVersion version, final Network network) {
        try {
            Class<?> clazz = Class.forName(cName);
            Method method = clazz.getMethod("create", SearchVersion.class, Network.class);
            return (SearchServerInstance) method.invoke(null, version, network);
        } catch (Exception ex) {
            throw new NotImplementedException("Could not create Search instance.", ex);
        }
    }

    @Override
    public SearchVersion getVersion() {
        return version;
    }
}
