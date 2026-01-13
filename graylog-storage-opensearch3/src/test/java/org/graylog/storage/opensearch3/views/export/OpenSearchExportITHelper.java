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
package org.graylog.storage.opensearch3.views.export;

import org.graylog.plugins.views.search.export.ExportMessagesCommand;
import org.graylog.plugins.views.search.export.SimpleMessageChunk;
import org.graylog.plugins.views.search.searchfilters.db.UsedSearchFiltersToQueryStringsMapper;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.graylog.storage.views.export.ExportITHelper;

import java.util.LinkedHashSet;

public class OpenSearchExportITHelper extends ExportITHelper {

    public OpenSearchExportITHelper(OfficialOpensearchClient client, UsedSearchFiltersToQueryStringsMapper filters, String... indices) {
        super(new OpenSearchExportBackend(mockIndexLookup(indices), false, filters, client));
    }

    public LinkedHashSet<SimpleMessageChunk> collectChunksFor(final ExportMessagesCommand command) {
        LinkedHashSet<SimpleMessageChunk> allChunks = new LinkedHashSet<>();
        exportBackend.run(command, allChunks::add);
        return allChunks;
    }


}
