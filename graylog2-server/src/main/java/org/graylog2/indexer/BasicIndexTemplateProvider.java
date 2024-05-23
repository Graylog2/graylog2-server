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
package org.graylog2.indexer;

import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.storage.SearchVersion;
import jakarta.annotation.Nonnull;

import static org.graylog2.storage.SearchVersion.Distribution.DATANODE;
import static org.graylog2.storage.SearchVersion.Distribution.ELASTICSEARCH;
import static org.graylog2.storage.SearchVersion.Distribution.OPENSEARCH;

public abstract class BasicIndexTemplateProvider<T extends IndexMappingTemplate> implements IndexTemplateProvider<T> {

    @Nonnull
    @Override
    public T create(@Nonnull SearchVersion searchVersion,
                                       IndexSetConfig indexSetConfig) throws IgnoreIndexTemplate {
        if (isProperSearchVersion(searchVersion)) {
            return createTemplateInstance();
        } else {
            throw new ElasticsearchException("Unsupported Search version: " + searchVersion);
        }
    }

    private boolean isProperSearchVersion(@Nonnull SearchVersion searchVersion) {
        return searchVersion.satisfies(ELASTICSEARCH, "^7.0.0")
                || searchVersion.satisfies(OPENSEARCH, "^1.0.0 | ^2.0.0")
                || searchVersion.satisfies(DATANODE, "^5.2.0");
    }

    protected abstract T createTemplateInstance();
}
