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

import javax.annotation.Nonnull;

import static org.graylog2.storage.SearchVersion.Distribution.ELASTICSEARCH;
import static org.graylog2.storage.SearchVersion.Distribution.OPENSEARCH;

public class MessageIndexTemplateProvider implements IndexTemplateProvider {

    public static final String MESSAGE_TEMPLATE_TYPE = "messages";

    @Override
    public IndexMapping create(@Nonnull SearchVersion searchVersion, @Nonnull IndexSetConfig indexSetConfig) {
        if (searchVersion.satisfies(ELASTICSEARCH, "^7.0.0") ||  searchVersion.satisfies(OPENSEARCH, "^1.0.0")) {
            return new IndexMapping7();
        } else {
            throw new ElasticsearchException("Unsupported Search version: " + searchVersion);
        }
    }
}
