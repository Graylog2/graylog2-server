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

import com.google.common.collect.ImmutableList;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.DeleteResult;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog.plugins.pipelineprocessor.db.PipelineRulesMetadataDao;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.resources.streams.responses.StreamPipelineRulesResponse;
import org.graylog2.rest.resources.streams.responses.StreamReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static org.graylog.plugins.pipelineprocessor.db.PipelineRulesMetadataDao.FIELD_CONNECTED_STREAM_TITLES;
import static org.graylog.plugins.pipelineprocessor.db.PipelineRulesMetadataDao.FIELD_PIPELINE_ID;
import static org.graylog.plugins.pipelineprocessor.db.PipelineRulesMetadataDao.FIELD_PIPELINE_TITLE;
import static org.graylog.plugins.pipelineprocessor.db.PipelineRulesMetadataDao.FIELD_ROUTED_STREAMS;
import static org.graylog.plugins.pipelineprocessor.db.PipelineRulesMetadataDao.FIELD_ROUTING_RULES;
import static org.graylog.plugins.pipelineprocessor.db.PipelineRulesMetadataDao.FIELD_RULE_TITLES;

/**
 * Persists information on pipeline and rules to avoid parsing these repeatedly
 */
public class MongoDbPipelineMetadataService {
    private static final Logger LOG = LoggerFactory.getLogger(MongoDbPipelineMetadataService.class);
    public static final String RULES_COLLECTION_NAME = "pipeline_processor_rules_meta";
    private final MongoCollection<PipelineRulesMetadataDao> collection;

    @Inject
    public MongoDbPipelineMetadataService(MongoCollections mongoCollections) {
        this.collection = mongoCollections.collection(RULES_COLLECTION_NAME, PipelineRulesMetadataDao.class);
        collection.createIndex(Indexes.ascending(FIELD_PIPELINE_ID), new IndexOptions().unique(true));
    }

    public ImmutableList<PipelineRulesMetadataDao> getAll() {
        return ImmutableList.copyOf(collection.find());
    }

    public PipelineRulesMetadataDao get(final String pipelineId) throws NotFoundException {
        final PipelineRulesMetadataDao dao = collection.find(eq(FIELD_PIPELINE_ID, pipelineId)).first();
        if (dao == null) {
            throw new NotFoundException("No pipeline found with id: " + pipelineId);
        }
        return dao;
    }

    public Map<String, PipelineRulesMetadataDao> get(Set<String> pipelineIds) {
        return collection.find(Filters.in(PipelineRulesMetadataDao.FIELD_PIPELINE_ID, pipelineIds))
                .into(new ArrayList<>())
                .stream()
                .collect(Collectors.toMap(PipelineRulesMetadataDao::pipelineId, dao -> dao));
    }

    public Set<PipelineRulesMetadataDao> getRoutingPipelines(String streamId) {
        return collection.find(Filters.exists(PipelineRulesMetadataDao.FIELD_ROUTED_STREAMS + "." + streamId, true))
                .into(new HashSet<>());
    }

    public Set<String> getPipelinesByRule(final String ruleId) {
        return collection.find(eq(PipelineRulesMetadataDao.FIELD_RULES, ruleId))
                .map(PipelineRulesMetadataDao::pipelineId)
                .into(new HashSet<>());
    }

    public Set<String> getPipelinesByRules(final Set<String> ruleIds) {
        return ruleIds.stream()
                .flatMap(ruleId -> getPipelinesByRule(ruleId).stream())
                .collect(Collectors.toSet());
    }

    public Set<PipelineRulesMetadataDao> getReferencingPipelines() {
        return collection.find(eq(PipelineRulesMetadataDao.FIELD_HAS_INPUT_REFERENCES, true))
                .into(new HashSet<>());
    }

    public void save(List<PipelineRulesMetadataDao> ruleRecords, boolean upsert) {
        if (!ruleRecords.isEmpty()) {
            LOG.debug("Inserting/Updating {} pipeline rules metadata records.", ruleRecords.size());
            if (upsert) {
                List<ReplaceOneModel<PipelineRulesMetadataDao>> ops = ruleRecords.stream()
                        .map(ruleRecord -> new ReplaceOneModel<>(
                                eq(FIELD_PIPELINE_ID, ruleRecord.pipelineId()),
                                ruleRecord,
                                new ReplaceOptions().upsert(true)
                        ))
                        .toList();
                collection.bulkWrite(ops);
            } else {
                collection.insertMany(ruleRecords);
            }
        }
    }

    public void delete(Collection<String> pipelineIds) {
        final DeleteResult deleteResult = collection.deleteMany(Filters.in(FIELD_PIPELINE_ID, pipelineIds));
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
            String streamId, String query, String sortField, SortOrder order, int page, int perPage) {

        final String routingRulesArray = "routing_rules_array";

        // Stage 1: Match documents where routed_streams.<streamId> exists
        final Bson matchRoutedStream = Aggregates.match(
                Filters.exists(FIELD_ROUTED_STREAMS + "." + streamId, true));

        // Stage 2: Convert routing_rules map to array
        final Bson setRoutingRulesArray = new Document("$set",
                new Document(routingRulesArray,
                        new Document("$objectToArray", "$" + FIELD_ROUTING_RULES)));

        // Stage 3: Unwind the routing_rules_array
        final Bson unwind = Aggregates.unwind("$" + routingRulesArray);

        // Stage 4: Keep only rules where routing_rules_array.v contains the streamId
        final Bson matchRuleForStream = Aggregates.match(
                Filters.eq(routingRulesArray + ".v", streamId));

        // Stage 5: Reshape to response fields
        // Look up rule title by filtering the rule_titles map (converted to array) by the current rule key.
        // $getField does not support dynamic field names, so we use $arrayElemAt + $filter instead.
        final Document ruleTitleExpr = new Document("$ifNull", List.of(
                new Document("$arrayElemAt", List.of(
                        new Document("$map", new Document("input",
                                new Document("$filter", new Document("input",
                                        new Document("$objectToArray", "$" + FIELD_RULE_TITLES))
                                        .append("as", "rt")
                                        .append("cond", new Document("$eq",
                                                List.of("$$rt.k", "$" + routingRulesArray + ".k")))))
                                .append("as", "matched")
                                .append("in", "$$matched.v")),
                        0)),
                "Unknown"));
        final Bson setResponseFields = new Document("$set", new Document()
                .append("rule_id", "$" + routingRulesArray + ".k")
                .append("id", "$" + routingRulesArray + ".k")
                .append("rule", ruleTitleExpr)
                .append("pipeline", new Document("$ifNull", List.of("$" + FIELD_PIPELINE_TITLE, "Unknown")))
                .append("connected_streams", new Document("$map", new Document("input",
                        new Document("$objectToArray", "$" + FIELD_CONNECTED_STREAM_TITLES))
                        .append("as", "cs")
                        .append("in", new Document("id", "$$cs.k").append("title", "$$cs.v")))));

        // Stage 6 (optional): Regex search on rule and pipeline fields
        final List<Bson> queryStages = new ArrayList<>();
        if (query != null && !query.isBlank()) {
            final String escapedQuery = Pattern.quote(query);
            queryStages.add(Aggregates.match(Filters.or(
                    Filters.regex("rule", escapedQuery, "i"),
                    Filters.regex("pipeline", escapedQuery, "i")
            )));
        }

        // Stage 7: Project final shape
        final Bson project = Aggregates.project(new Document("_id", 0)
                .append("id", 1)
                .append("pipeline_id", "$" + FIELD_PIPELINE_ID)
                .append("pipeline", 1)
                .append("rule_id", 1)
                .append("rule", 1)
                .append("connected_streams", 1));

        // Build the base pipeline (stages 1-7)
        final List<Bson> basePipeline = new ArrayList<>();
        basePipeline.add(matchRoutedStream);
        basePipeline.add(setRoutingRulesArray);
        basePipeline.add(unwind);
        basePipeline.add(matchRuleForStream);
        basePipeline.add(setResponseFields);
        basePipeline.addAll(queryStages);
        basePipeline.add(project);

        // Count pipeline: base stages + $count
        final List<Bson> countPipeline = new ArrayList<>(basePipeline);
        countPipeline.add(new Document("$count", "total"));

        final Document countResult = collection.aggregate(countPipeline, Document.class).first();
        final int total = (countResult != null) ? countResult.getInteger("total", 0) : 0;

        // Data pipeline: base stages + sort, skip, limit
        final int skip = Math.max(0, page - 1) * perPage;
        final List<Bson> dataPipeline = new ArrayList<>(basePipeline);
        dataPipeline.add(Aggregates.sort(order.toBsonSort(sortField)));
        dataPipeline.add(Aggregates.skip(skip));
        dataPipeline.add(Aggregates.limit(perPage));

        final List<StreamPipelineRulesResponse> results = new ArrayList<>();
        collection.aggregate(dataPipeline, Document.class)
                .forEach(doc -> results.add(toStreamPipelineRulesResponse(doc)));

        return new PaginatedList<>(results, total, page, perPage);
    }

    @SuppressWarnings("unchecked")
    private static StreamPipelineRulesResponse toStreamPipelineRulesResponse(Document doc) {
        final List<?> rawList = doc.get("connected_streams", List.class);
        final List<StreamReference> connectedStreams = (rawList != null ? rawList : List.of()).stream()
                .map(element -> {
                    final Map<String, Object> cs = (Map<String, Object>) element;
                    return new StreamReference((String) cs.get("id"), (String) cs.get("title"));
                })
                .toList();
        return new StreamPipelineRulesResponse(
                doc.getString("id"),
                doc.getString("pipeline_id"),
                doc.getString("pipeline"),
                doc.getString("rule_id"),
                doc.getString("rule"),
                connectedStreams);
    }
}
