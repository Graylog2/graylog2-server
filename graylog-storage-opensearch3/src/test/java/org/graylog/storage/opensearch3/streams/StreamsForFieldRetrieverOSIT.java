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
package org.graylog.storage.opensearch3.streams;

import org.graylog.storage.opensearch3.fieldtypes.streams.StreamsForFieldRetrieverOS;
import org.graylog.storage.opensearch3.testing.OpenSearchInstance;
import org.graylog.testing.elasticsearch.SearchInstance;
import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.graylog2.indexer.fieldtypes.StreamsForFieldRetrieverIT;
import org.graylog2.indexer.fieldtypes.streamfiltered.esadapters.StreamsForFieldRetriever;

public class StreamsForFieldRetrieverOSIT extends StreamsForFieldRetrieverIT {

    @SearchInstance
    public final OpenSearchInstance openSearchInstance = OpenSearchInstance.create();

    @Override
    protected SearchServerInstance searchServer() {
        return this.openSearchInstance;
    }

    @Override
    protected StreamsForFieldRetriever getRetriever() {
        return new StreamsForFieldRetrieverOS(openSearchInstance.getOfficialOpensearchClient());
    }
}
