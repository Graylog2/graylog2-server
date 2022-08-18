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
package org.graylog.plugins.views.search.engine;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.graylog2.indexer.searches.SearchesClusterConfig;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.joda.time.Period;

import java.util.Optional;

public class SearchConfigProvider implements Provider<SearchConfig> {

    private final ClusterConfigService clusterConfigService;

    @Inject
    public SearchConfigProvider(ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public SearchConfig get() {
        final SearchesClusterConfig searchesClusterConfig = clusterConfigService.get(SearchesClusterConfig.class);
        final Period queryTimeRangeLimit = Optional.ofNullable(searchesClusterConfig).map(SearchesClusterConfig::queryTimeRangeLimit).orElse(Period.ZERO);
        return new SearchConfig(queryTimeRangeLimit);
    }
}
