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
package org.graylog.storage.elasticsearch7.fieldtypes.streams;

import org.graylog.storage.elasticsearch7.testing.ElasticsearchInstanceES7;
import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.graylog2.indexer.fieldtypes.StreamsForFieldRetrieverIT;
import org.graylog2.indexer.fieldtypes.streamfiltered.esadapters.StreamsForFieldRetriever;
import org.junit.Rule;

public class StreamsForFieldRetrieverES7IT extends StreamsForFieldRetrieverIT {

    @Rule
    public final ElasticsearchInstanceES7 elasticsearch = ElasticsearchInstanceES7.create();

    @Override
    protected SearchServerInstance searchServer() {
        return this.elasticsearch;
    }

    @Override
    protected StreamsForFieldRetriever getRetriever() {
        return new StreamsForFieldRetrieverES7(elasticsearch.elasticsearchClient());
    }
}
