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
package org.graylog.plugins.pipelineprocessor.db.mongodb;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.DeleteResult;
import jakarta.inject.Inject;
import org.bson.conversions.Bson;
import org.graylog.plugins.pipelineprocessor.db.PipelineRulesMetadataDao;
import org.graylog.plugins.pipelineprocessor.db.RoutingRuleDao;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.resources.streams.responses.StreamPipelineRulesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static org.graylog.plugins.pipelineprocessor.db.PipelineRulesMetadataDao.FIELD_PIPELINE_ID;
import static org.graylog.plugins.pipelineprocessor.db.RoutingRuleDao.FIELD_ROUTED_STREAM_IDS;

/**
 * Persists information on pipeline and rules to avoid parsing these repeatedly.
 * Uses two collections: one for pipeline-level metadata and one for routing rule records.
 */
public class MongoDbPipelineMetadataService {
    private static final Logger LOG = LoggerFactory.getLogger(MongoDbPipelineMetadataService.class);
    public static final String RULES_COLLECTION_NAME = "pipeline_processor_rules_meta";
    public static final String ROUTING_RULES_COLLECTION_NAME = "pipeline_processor_routing_rules";

    private final MongoCollection<PipelineRulesMetadataDao> collection;
    private final MongoCollection<RoutingRuleDao> routingRulesCollection;

    @Inject
    public MongoDbPipelineMetadataService(MongoCollections mongoCollections) {
        this.collection = mongoCollections.collection(RULES_COLLECTION_NAME, PipelineRulesMetadataDao.class);
        collection.createIndex(Indexes.ascending(FIELD_PIPELINE_ID), new IndexOptions().unique(true));

        this.routingRulesCollection = mongoCollections.collection(ROUTING_RULES_COLLECTION_NAME, RoutingRuleDao.class);
        routingRulesCollection.createIndex(Indexes.ascending(FIELD_ROUTED_STREAM_IDS));
        routingRulesCollection.createIndex(Indexes.ascending(RoutingRuleDao.FIELD_PIPELINE_ID));
    }

    public PipelineRulesMetadataDao get(final String pipelineId) throws NotFoundException {
        final PipelineRulesMetadataDao dao = collection.find(eq(FIELD_PIPELINE_ID, pipelineId)).first();
        if (dao == null) {
            throw new NotFoundException("No pipeline found with id: " + pipelineId);
        }
        return dao;
    }

    public Map<String, PipelineRulesMetadataDao> get(Set<String> pipelineIds) {
        return collection.find(Filters.in(FIELD_PIPELINE_ID, pipelineIds))
                .into(new ArrayList<>())
                .stream()
                .collect(Collectors.toMap(PipelineRulesMetadataDao::pipelineId, dao -> dao));
    }

    public Set<String> getPipelinesByRule(final String ruleId) {
        return collection.find(eq(PipelineRulesMetadataDao.FIELD_RULES, ruleId))
                .map(PipelineRulesMetadataDao::pipelineId)
                .into(new HashSet<>());
    }

    public Set<String> getPipelinesByRules(final Set<String> ruleIds) {
        return collection.find(Filters.in(PipelineRulesMetadataDao.FIELD_RULES, ruleIds))
                .map(PipelineRulesMetadataDao::pipelineId)
                .into(new HashSet<>());
    }

    public Set<PipelineRulesMetadataDao> getReferencingPipelines() {
        return collection.find(eq(PipelineRulesMetadataDao.FIELD_HAS_INPUT_REFERENCES, true))
                .into(new HashSet<>());
    }

    public void save(List<PipelineRulesMetadataDao> pipelineRecords,
                     List<RoutingRuleDao> routingRuleRecords,
                     boolean upsert) {
        if (!pipelineRecords.isEmpty()) {
            LOG.debug("Inserting/Updating {} pipeline metadata records.", pipelineRecords.size());
            if (upsert) {
                final List<ReplaceOneModel<PipelineRulesMetadataDao>> ops = pipelineRecords.stream()
                        .map(record -> new ReplaceOneModel<>(
                                eq(FIELD_PIPELINE_ID, record.pipelineId()),
                                record,
                                new ReplaceOptions().upsert(true)
                        ))
                        .toList();
                collection.bulkWrite(ops);

                // For routing rules, the set of (pipeline, rule) pairs may have changed,
                // so delete existing records for affected pipelines, then insert new ones.
                final Set<String> affectedPipelineIds = pipelineRecords.stream()
                        .map(PipelineRulesMetadataDao::pipelineId)
                        .collect(Collectors.toSet());
                routingRulesCollection.deleteMany(Filters.in(RoutingRuleDao.FIELD_PIPELINE_ID, affectedPipelineIds));
            } else {
                collection.insertMany(pipelineRecords);
            }
        }
        if (!routingRuleRecords.isEmpty()) {
            LOG.debug("Inserting {} routing rule records.", routingRuleRecords.size());
            routingRulesCollection.insertMany(routingRuleRecords);
        }
    }

    public void delete(Collection<String> pipelineIds) {
        final DeleteResult deleteResult = collection.deleteMany(Filters.in(FIELD_PIPELINE_ID, pipelineIds));
        routingRulesCollection.deleteMany(Filters.in(RoutingRuleDao.FIELD_PIPELINE_ID, pipelineIds));
        if (deleteResult.getDeletedCount() == 0) {
            LOG.warn("No pipeline rules metadata records found for pipelines {}", pipelineIds);
        } else {
            LOG.debug("Deleted {} pipeline rules metadata records.", deleteResult.getDeletedCount());
        }
    }

    public Set<String> deprecatedFunctionsPipeline(String pipelineId) {
        try {
            final PipelineRulesMetadataDao dao = get(pipelineId);
            return dao.deprecatedFunctions();
        } catch (NotFoundException ignored) {
            return Set.of();
        }
    }

    public PaginatedList<StreamPipelineRulesResponse> getRoutingRulesPaginated(
            String streamId, Bson additionalFilter, String sortField, SortOrder order, int page, int perPage) {

        final Bson streamFilter = eq(FIELD_ROUTED_STREAM_IDS, streamId);
        final Bson filter = additionalFilter != null
                ? Filters.and(streamFilter, additionalFilter)
                : streamFilter;

        final int total = (int) routingRulesCollection.countDocuments(filter);
        final int skip = Math.max(0, page - 1) * perPage;

        final List<StreamPipelineRulesResponse> results = routingRulesCollection.find(filter)
                .sort(order.toBsonSort(sortField))
                .skip(skip)
                .limit(perPage)
                .map(MongoDbPipelineMetadataService::toStreamPipelineRulesResponse)
                .into(new ArrayList<>());

        return new PaginatedList<>(results, total, page, perPage);
    }

    private static StreamPipelineRulesResponse toStreamPipelineRulesResponse(RoutingRuleDao dao) {
        return new StreamPipelineRulesResponse(
                dao.ruleId(),
                dao.pipelineId(),
                dao.pipelineTitle(),
                dao.ruleId(),
                dao.ruleTitle(),
                dao.connectedStreams());
    }
}
