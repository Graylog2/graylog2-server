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
package org.graylog.storage.elasticsearch7.testing;

import org.graylog.testing.completebackend.SearchServerBuilder;
import org.graylog2.storage.SearchVersion;

public class Elasticsearch7InstanceBuilder  extends SearchServerBuilder<ElasticsearchInstanceES7> {
    public Elasticsearch7InstanceBuilder(SearchVersion version) {
        super(version);
    }

    public static Elasticsearch7InstanceBuilder builder() {
        return new Elasticsearch7InstanceBuilder(SearchVersion.elasticsearch("7.10.2"));
    }

    @Override
    protected ElasticsearchInstanceES7 instantiate() {
        return new ElasticsearchInstanceES7(getVersion(), getHostname(), getNetwork(), getHeapSize(), getFeatureFlags()).init();
    }
}
