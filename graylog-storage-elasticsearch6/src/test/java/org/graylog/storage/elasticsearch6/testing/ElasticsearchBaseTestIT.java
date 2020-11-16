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
package org.graylog.storage.elasticsearch6.testing;

import org.graylog.testing.elasticsearch.ElasticsearchBaseTest;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ElasticsearchBaseTestIT extends ElasticsearchBaseTest {
    @Rule
    public final ElasticsearchInstance elasticsearch = ElasticsearchInstanceES6.create();

    @Override
    protected ElasticsearchInstance elasticsearch() {
        return this.elasticsearch;
    }

    @Test
    public void clientsAreConstructed() {
        assertThat(((ElasticsearchInstanceES6)elasticsearch).jestClient()).isNotNull();
        assertThat(client()).isNotNull();
    }
}
