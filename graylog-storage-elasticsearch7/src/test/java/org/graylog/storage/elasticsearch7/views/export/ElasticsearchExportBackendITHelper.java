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
package org.graylog.storage.elasticsearch7.views.export;

import org.graylog.plugins.views.search.export.ExportMessagesCommand;
import org.graylog.plugins.views.search.export.SimpleMessageChunk;
import org.graylog.plugins.views.search.searchfilters.db.UsedSearchFiltersToQueryStringsMapper;
import org.graylog.storage.elasticsearch7.ElasticsearchClient;
import org.graylog.storage.views.export.ExportBackendITHelper;

import java.util.LinkedHashSet;

public class ElasticsearchExportBackendITHelper extends ExportBackendITHelper {

    public ElasticsearchExportBackendITHelper(ElasticsearchClient client, UsedSearchFiltersToQueryStringsMapper filters, String... indices) {
        super(new ElasticsearchExportBackend(mockIndexLookup(indices), requestStrategy(client), false, filters));
    }

    private static RequestStrategy requestStrategy(ElasticsearchClient client) {
        final ExportClient exportClient = new ExportClient(client);
        return new SearchAfter(exportClient);
    }

    @Override
    public LinkedHashSet<SimpleMessageChunk> collectChunksFor(final ExportMessagesCommand command) {
        LinkedHashSet<SimpleMessageChunk> allChunks = new LinkedHashSet<>();
        exportBackend.run(command, allChunks::add);
        return allChunks;
    }

}
