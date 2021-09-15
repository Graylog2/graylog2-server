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

import com.github.zafarkhaja.semver.Version;

public class EventIndexTemplateProvider implements IndexTemplateProvider {

    public static final String EVENT_TEMPLATE_TYPE = "events";

    @Override
    public IndexMappingTemplate forVersion(Version elasticsearchVersion) {
        if (elasticsearchVersion.satisfies("^5.0.0 | ^6.0.0")) {
            return new EventsIndexMapping6();
        } else if (elasticsearchVersion.satisfies("^7.0.0")) {
            return new EventsIndexMapping7();
        } else {
            throw new ElasticsearchException("Unsupported Elasticsearch version: " + elasticsearchVersion);
        }
    }
}
