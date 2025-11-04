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
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.DeleteResult;
import jakarta.inject.Inject;
import org.graylog.plugins.pipelineprocessor.db.PipelineInputsMetadataDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineRulesMetadataDao;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.NotFoundException;
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
import static org.graylog.plugins.pipelineprocessor.db.PipelineInputsMetadataDao.FIELD_INPUT_ID;
import static org.graylog.plugins.pipelineprocessor.db.PipelineRulesMetadataDao.FIELD_PIPELINE_ID;

/**
 * Persists information on pipeline and rules to avoid parsing these repeatedly
 */
public class MongoDbPipelineMetadataService {
    private static final Logger LOG = LoggerFactory.getLogger(MongoDbPipelineMetadataService.class);
    public static final String RULES_COLLECTION_NAME = "pipeline_processor_rules_meta";
    public static final String INPUTS_COLLECTION_NAME = "pipeline_processor_inputs_meta";
    private final MongoCollection<PipelineRulesMetadataDao> rulesCollection;
    private final MongoCollection<PipelineInputsMetadataDao> inputsCollection;

    @Inject
    public MongoDbPipelineMetadataService(MongoCollections mongoCollections) {
        this.rulesCollection = mongoCollections.collection(RULES_COLLECTION_NAME, PipelineRulesMetadataDao.class);
        rulesCollection.createIndex(Indexes.ascending(FIELD_PIPELINE_ID), new IndexOptions().unique(true));

        this.inputsCollection = mongoCollections.collection(INPUTS_COLLECTION_NAME, PipelineInputsMetadataDao.class);
        inputsCollection.createIndex(Indexes.ascending(FIELD_INPUT_ID), new IndexOptions().unique(true));
    }

    public ImmutableList<PipelineRulesMetadataDao> getAll() {
        return ImmutableList.copyOf(rulesCollection.find());
    }

    public PipelineRulesMetadataDao getByPipelineId(final String pipelineId) throws NotFoundException {
        final PipelineRulesMetadataDao dao = rulesCollection.find(eq(FIELD_PIPELINE_ID, pipelineId)).first();
        if (dao == null) {
            throw new NotFoundException("No pipeline found with id: " + pipelineId);
        }
        return dao;
    }

    public PipelineInputsMetadataDao getByInputId(final String inputId) throws NotFoundException {
        final PipelineInputsMetadataDao dao = inputsCollection.find(eq(FIELD_INPUT_ID, inputId)).first();
        if (dao == null) {
            throw new NotFoundException("No input found with id: " + inputId);
        }
        return dao;
    }

    public Set<String> getPipelinesByRule(final String ruleId) {
        return rulesCollection.find(eq(PipelineRulesMetadataDao.FIELD_RULES, ruleId))
                .map(PipelineRulesMetadataDao::pipelineId)
                .into(new HashSet<>());
    }

    public Set<String> getPipelinesByRules(final Set<String> ruleIds) {
        return ruleIds.stream()
                .flatMap(ruleId -> getPipelinesByRule(ruleId).stream())
                .collect(Collectors.toSet());
    }

    public void saveRulesMetadata(List<PipelineRulesMetadataDao> ruleRecords, boolean upsert) {
        if (!ruleRecords.isEmpty()) {
            LOG.info("Inserting/Updating {} pipeline rules metadata records.", ruleRecords.size());
            if (upsert) {
                List<ReplaceOneModel<PipelineRulesMetadataDao>> ops = ruleRecords.stream()
                        .map(ruleRecord -> new ReplaceOneModel<>(
                                eq(FIELD_PIPELINE_ID, ruleRecord.pipelineId()),
                                ruleRecord,
                                new ReplaceOptions().upsert(true)
                        ))
                        .toList();
                rulesCollection.bulkWrite(ops);
            } else {
                rulesCollection.insertMany(ruleRecords);
            }
        }
    }

    public void saveInputsMetadata(Map<String, Set<PipelineInputsMetadataDao.MentionedInEntry>> inputMentions, boolean upsert) {
        final List<PipelineInputsMetadataDao> inputRecords = new ArrayList<>();
        inputMentions.forEach((inputId, mentionedInEntries) -> {
            final PipelineInputsMetadataDao inputMetadata = PipelineInputsMetadataDao.builder()
                    .inputId(inputId)
                    .mentionedIn(new ArrayList<>(mentionedInEntries))
                    .build();
            inputRecords.add(inputMetadata);
        });

        if (!inputRecords.isEmpty()) {
            LOG.info("Inserting/Updating {} pipeline inputs metadata records.", inputRecords.size());
            if (upsert) {
                List<ReplaceOneModel<PipelineInputsMetadataDao>> ops = inputRecords.stream()
                        .map(inputRecord -> new ReplaceOneModel<>(
                                eq(FIELD_INPUT_ID, inputRecord.inputId()),
                                inputRecord,
                                new ReplaceOptions().upsert(true)
                        ))
                        .toList();
                inputsCollection.bulkWrite(ops);
            } else {
                inputsCollection.insertMany(inputRecords);
            }
        }
    }

    public void deleteRulesMetadataByPipelineId(Collection<String> pipelineIds) {
        final DeleteResult deleteResult = rulesCollection.deleteMany(eq(FIELD_PIPELINE_ID, pipelineIds));
        if (deleteResult.getDeletedCount() == 0) {
            LOG.warn("No pipeline rules metadata records found for pipelines {}", pipelineIds);
        } else {
            LOG.info("Deleted {} pipeline rules metadata records.", deleteResult.getDeletedCount());
        }
    }

    public void deleteByInputId(String id) {
        final DeleteResult deleteResult = inputsCollection.deleteOne(eq(FIELD_INPUT_ID, id));
        if (deleteResult.getDeletedCount() == 1) {
            LOG.info("Deleted pipeline inputs metadata record for input {}", id);
        } else {
            LOG.warn("No pipeline inputs metadata record found for input {}", id);
        }
    }
}
