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

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.shared.utilities.StringUtils.f;

@Singleton
public class IndexMappingFactory {
    private final Node node;
    private final Set<IndexTemplateProvider> providers;

    @Inject
    public IndexMappingFactory(Node node, Set<IndexTemplateProvider> providers) {
        this.node = node;
        this.providers = providers;
    }

    public IndexMappingTemplate createIndexMapping(@Nonnull String templateType) {
        final Version elasticsearchVersion = node.getVersion()
                .orElseThrow(() -> new ElasticsearchException("Unable to retrieve Elasticsearch version."));

        return resolveIndexMappingTemplateProvider(templateType)
                .forVersion(elasticsearchVersion);
    }

    private IndexTemplateProvider resolveIndexMappingTemplateProvider(@Nonnull String templateType) {
        final List<IndexTemplateProvider> matching = providers.stream()
                .filter(p -> p.templateType().equals(templateType))
                .collect(Collectors.toList());

        if (matching.isEmpty()) {
            throw new IllegalStateException(f("No index mapping template provider found for type '%s'", templateType));
        }

        if (matching.size() > 1) {
            throw new IllegalStateException(f("Found %s mapping template providers matching type '%s'",
                    matching.size(), templateType));
        }

        return matching.get(0);
    }
}
