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
package org.graylog.storage.elasticsearch6.views.export;

import io.searchbox.action.Action;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import org.graylog.plugins.views.search.export.ExportException;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog.storage.elasticsearch6.jest.JestUtils;

import javax.inject.Inject;
import java.util.Optional;
import java.util.function.Supplier;

import static org.graylog.storage.elasticsearch6.jest.JestUtils.checkForFailedShards;

public class JestWrapper {
    private final JestClient jestClient;

    @Inject
    public JestWrapper(JestClient jestClient) {
        this.jestClient = jestClient;
    }

    public <T extends JestResult> T execute(Action<T> action, Supplier<String> errorMessageSupplier) {
        final T result = JestUtils.execute(jestClient, action, errorMessageSupplier);
        Optional<ElasticsearchException> elasticsearchException = checkForFailedShards(result);
        if (elasticsearchException.isPresent()) {
            throw new ExportException(errorMessageSupplier.get(), elasticsearchException.get());
        }
        return result;
    }
}
