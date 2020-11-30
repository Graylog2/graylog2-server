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
import org.graylog2.indexer.cluster.Node;
import org.graylog2.indexer.indexset.IndexSetConfig;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class IndexMappingFactory {
    private final Node node;

    @Inject
    public IndexMappingFactory(Node node) {
        this.node = node;
    }

    public IndexMappingTemplate createIndexMapping(IndexSetConfig.TemplateType templateType) {
        final Version elasticsearchVersion = node.getVersion().orElseThrow(() -> new ElasticsearchException("Unable to retrieve Elasticsearch version."));

        switch (templateType) {
            case MESSAGES: return indexMappingFor(elasticsearchVersion);
            case EVENTS: return eventsIndexMappingFor(elasticsearchVersion);
            case GIM_V1: return gimMappingFor(elasticsearchVersion);
            default: throw new IllegalStateException("Invalid index template type: " + templateType);
        }
    }

    private IndexMapping gimMappingFor(Version elasticsearchVersion) {
        if (elasticsearchVersion.satisfies("^6.0.0")) {
            return new GIMMapping6();
        } else if (elasticsearchVersion.satisfies("^7.0.0")) {
            return new GIMMapping7();
        } else {
            throw new ElasticsearchException("Unsupported Elasticsearch version: " + elasticsearchVersion);
        }
    }

    public static IndexMapping indexMappingFor(Version elasticsearchVersion) {
        if (elasticsearchVersion.satisfies("^5.0.0")) {
            return new IndexMapping5();
        } else if (elasticsearchVersion.satisfies("^6.0.0")) {
            return new IndexMapping6();
        } else if (elasticsearchVersion.satisfies("^7.0.0")) {
            return new IndexMapping7();
        } else {
            throw new ElasticsearchException("Unsupported Elasticsearch version: " + elasticsearchVersion);
        }
    }

    public static IndexMappingTemplate eventsIndexMappingFor(Version elasticsearchVersion) {
        if (elasticsearchVersion.satisfies("^5.0.0 | ^6.0.0")) {
            return new EventsIndexMapping6();
        } else if (elasticsearchVersion.satisfies("^7.0.0")) {
            return new EventsIndexMapping7();
        } else {
            throw new ElasticsearchException("Unsupported Elasticsearch version: " + elasticsearchVersion);
        }
    }
}
