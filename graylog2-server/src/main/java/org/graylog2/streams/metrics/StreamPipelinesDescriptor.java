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
package org.graylog2.streams.metrics;

import jakarta.inject.Inject;
import org.graylog.plugins.pipelineprocessor.db.PipelineRulesMetadataDao;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineMetadataService;
import org.graylog.plugins.pipelineprocessor.rest.PipelineRestPermissions;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.metrics.entity.EntityMetric;
import org.graylog2.metrics.entity.EntityUncachedMetricDescriptor;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Provides {@code pipelines} metrics for streams.
 * Returns pipeline IDs connected to each stream, filtered by the user's {@code pipeline:read} permission.
 */
public class StreamPipelinesDescriptor implements EntityUncachedMetricDescriptor<List<String>> {

    public static final String FIELD_NAME = "pipelines";

    private final MongoDbPipelineMetadataService pipelineMetadataService;

    @Inject
    public StreamPipelinesDescriptor(MongoDbPipelineMetadataService pipelineMetadataService) {
        this.pipelineMetadataService = pipelineMetadataService;
    }

    @Override
    public String fieldName() {
        return FIELD_NAME;
    }

    @Override
    public List<EntityMetric<List<String>>> compute(Collection<String> entityIds, SearchUser searchUser) {
        final Set<String> allStreamIds = Set.copyOf(entityIds);
        final List<PipelineRulesMetadataDao> pipelines = pipelineMetadataService.getConnectedToStreams(allStreamIds);

        return entityIds.stream()
                .map(streamId -> {
                    final List<String> pipelineIds = pipelines.stream()
                            .filter(dao -> dao.streams().contains(streamId))
                            .map(PipelineRulesMetadataDao::pipelineId)
                            .filter(id -> searchUser.isPermitted(PipelineRestPermissions.PIPELINE_READ, id))
                            .toList();
                    return new EntityMetric<>(streamId, pipelineIds);
                })
                .toList();
    }
}
