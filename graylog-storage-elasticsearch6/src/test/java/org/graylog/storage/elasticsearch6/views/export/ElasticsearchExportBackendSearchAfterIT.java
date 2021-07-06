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

import org.graylog.plugins.views.search.export.ExportMessagesCommand;
import org.graylog.storage.elasticsearch6.testing.ElasticsearchInstanceES6;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.junit.Rule;
import org.junit.Test;

import static org.graylog.storage.elasticsearch6.testing.TestUtils.jestClient;

public class ElasticsearchExportBackendSearchAfterIT extends ElasticsearchExportBackendITBase {
    @Rule
    public final ElasticsearchInstance elasticsearch = ElasticsearchInstanceES6.create();

    @Override
    protected ElasticsearchInstance elasticsearch() {
        return this.elasticsearch;
    }

    @Override
    protected RequestStrategy requestStrategy() {
        return new SearchAfter(new JestWrapper(jestClient(elasticsearch)));
    }

    @Test
    public void sortsByTimestampAscending() {
        importFixture("messages.json");

        ExportMessagesCommand command = commandBuilderWithAllStreams().build();

        runWithExpectedResult(command, "timestamp,source,message",
                "graylog_0, 2015-01-01T01:00:00.000Z, source-1, Ha",
                "graylog_1, 2015-01-01T02:00:00.000Z, source-2, He",
                "graylog_0, 2015-01-01T03:00:00.000Z, source-1, Hi",
                "graylog_0, 2015-01-01T04:00:00.000Z, source-2, Ho");
    }
}
