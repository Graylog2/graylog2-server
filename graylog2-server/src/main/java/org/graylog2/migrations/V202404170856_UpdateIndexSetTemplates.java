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
package org.graylog2.migrations;

import jakarta.inject.Inject;
import org.graylog2.indexer.indexset.template.IndexSetTemplate;
import org.graylog2.indexer.indexset.template.IndexSetTemplateProvider;
import org.graylog2.indexer.indexset.template.IndexSetTemplateService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;

/**
 * Update the set of built-in indexset templates.
 */
public class V202404170856_UpdateIndexSetTemplates extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V202404170856_UpdateIndexSetTemplates.class);
    private final IndexSetTemplateService indexSetTemplateService;
    private final IndexSetTemplateProvider indexSetTemplateProvider;

    @Inject
    public V202404170856_UpdateIndexSetTemplates(final ClusterConfigService clusterConfigService,
                                                 IndexSetTemplateService indexSetTemplateService,
                                                 IndexSetTemplateProvider indexSetTemplateProvider) {
        this.indexSetTemplateService = indexSetTemplateService;
        this.indexSetTemplateProvider = indexSetTemplateProvider;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2024-04-17T08:56:00Z");
    }

    @Override
    public void upgrade() {
        deleteBuiltInTemplates();
        saveBuiltInTemplates();
    }

    private void deleteBuiltInTemplates() {
        indexSetTemplateService.deleteBuiltIns();
    }

    private void saveBuiltInTemplates() {
        for (IndexSetTemplate indexSetTemplate : indexSetTemplateProvider.get()) {
            indexSetTemplateService.save(indexSetTemplate);
        }
    }
}
