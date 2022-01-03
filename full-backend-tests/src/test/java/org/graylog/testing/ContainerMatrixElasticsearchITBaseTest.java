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
package org.graylog.testing;

import org.graylog.storage.elasticsearch7.ElasticsearchClient;
import org.graylog.storage.elasticsearch7.testing.ElasticsearchInstanceES7;
import org.graylog.storage.elasticsearch7.testing.OpensearchInstance;
import org.graylog.testing.elasticsearch.ContainerMatrixElasticsearchBaseTest;
import org.graylog.testing.elasticsearch.SearchServerInstance;

public abstract class ContainerMatrixElasticsearchITBaseTest extends ContainerMatrixElasticsearchBaseTest {
    public ContainerMatrixElasticsearchITBaseTest(SearchServerInstance elasticsearch) {
        super(elasticsearch);
    }

    protected ElasticsearchClient elasticsearchClient() {
        return elasticsearch() instanceof ElasticsearchInstanceES7
                ? ((ElasticsearchInstanceES7) elasticsearch()).elasticsearchClient()
                : ((OpensearchInstance) elasticsearch()).elasticsearchClient();
    }
}
