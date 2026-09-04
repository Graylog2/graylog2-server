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
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.DeleteResult;
import jakarta.inject.Inject;
import org.graylog.plugins.pipelineprocessor.db.PipelineInputsMetadataDao;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.mongodb.client.model.Filters.eq;
import static org.graylog.plugins.pipelineprocessor.db.PipelineInputsMetadataDao.FIELD_INPUT_ID;
import static org.graylog.plugins.pipelineprocessor.db.PipelineInputsMetadataDao.PATH_PIPELINE_ID;
import static org.graylog.plugins.pipelineprocessor.db.PipelineInputsMetadataDao.PATH_RULE_ID;

/**
 * Persists information on inputs and pipeline rules that reference them
 */
public class MongoDbInputsMetadataService {
    private static final Logger LOG = LoggerFactory.getLogger(MongoDbInputsMetadataService.class);
    public static final String INPUTS_COLLECTION_NAME = "pipeline_processor_inputs_meta";
    private final MongoCollection<PipelineInputsMetadataDao> collection;

    @Inject
    public MongoDbInputsMetadataService(MongoCollections mongoCollections) {
        this.collection = mongoCollections.collection(INPUTS_COLLECTION_NAME, PipelineInputsMetadataDao.class);
        collection.createIndex(Indexes.ascending(FIELD_INPUT_ID), new IndexOptions().unique(true));
    }

    public ImmutableList<PipelineInputsMetadataDao> getAll() {
        return ImmutableList.copyOf(collection.find());
    }

    public PipelineInputsMetadataDao getByInputId(final String inputId) throws NotFoundException {
        final PipelineInputsMetadataDao dao = collection.find(eq(FIELD_INPUT_ID, inputId)).first();
        if (dao == null) {
            throw new NotFoundException("No input found with id: " + inputId);
        }
        return dao;
    }

    public ImmutableList<PipelineInputsMetadataDao> getByInputIds(List<String> inputIds) {
        return ImmutableList.copyOf(collection.find(Filters.in(FIELD_INPUT_ID, inputIds)).into(new ArrayList<>()));
    }

    public void save(Map<String, Set<PipelineInputsMetadataDao.MentionedInEntry>> inputMentions, boolean merge) {
        final List<PipelineInputsMetadataDao> inputRecords = new ArrayList<>();
        inputMentions.forEach((inputId, mentionedInEntries) -> {
            final PipelineInputsMetadataDao inputMetadata = PipelineInputsMetadataDao.builder()
                    .inputId(inputId)
                    .mentionedIn(new ArrayList<>(mentionedInEntries))
                    .build();
            inputRecords.add(inputMetadata);
        });

        if (!inputRecords.isEmpty()) {
            if (merge) {
                List<PipelineInputsMetadataDao> mergedRecords = mergeMetadata(inputRecords);
                LOG.info("Updating {} pipeline inputs metadata records.", mergedRecords.size());
                List<ReplaceOneModel<PipelineInputsMetadataDao>> ops = mergedRecords.stream()
                        .map(inputRecord -> new ReplaceOneModel<>(
                                eq(FIELD_INPUT_ID, inputRecord.inputId()),
                                inputRecord,
                                new ReplaceOptions().upsert(true)
                        ))
                        .toList();
                collection.bulkWrite(ops);
            } else {
                LOG.info("Inserting {} pipeline inputs metadata records.", inputRecords.size());
                collection.insertMany(inputRecords);
            }
        }
    }

    List<PipelineInputsMetadataDao> mergeMetadata(List<PipelineInputsMetadataDao> inputRecords) {
        LOG.info("Merging {} pipeline inputs metadata records.", inputRecords.size());
        List<PipelineInputsMetadataDao> mergedRecords = new ArrayList<>();
        for (PipelineInputsMetadataDao newRecord : inputRecords) {
            try {
                PipelineInputsMetadataDao existingRecord = getByInputId(newRecord.inputId());
                PipelineInputsMetadataDao mergedRecord = PipelineInputsMetadataDao.builder()
                        .id(existingRecord.id())
                        .inputId(existingRecord.inputId())
                        .mentionedIn(mergeMentions(newRecord, existingRecord))
                        .build();
                mergedRecords.add(mergedRecord);
            } catch (NotFoundException e) {
                // No existing record, use the new one as is
                mergedRecords.add(newRecord);
            }
        }
        return mergedRecords;
    }

    private static List<PipelineInputsMetadataDao.MentionedInEntry> mergeMentions(PipelineInputsMetadataDao newRecord,
                                                                                  PipelineInputsMetadataDao existingRecord) {
        Map<String, PipelineInputsMetadataDao.MentionedInEntry> mergedMentionsMap = new HashMap<>();
        for (PipelineInputsMetadataDao.MentionedInEntry entry : existingRecord.mentionedIn()) {
            mergedMentionsMap.put(getKey(entry), entry);
        }
        for (PipelineInputsMetadataDao.MentionedInEntry newEntry : newRecord.mentionedIn()) {
            final String key = getKey(newEntry);
            if (mergedMentionsMap.containsKey(key)) {
                PipelineInputsMetadataDao.MentionedInEntry existingEntry = mergedMentionsMap.get(key);
                PipelineInputsMetadataDao.MentionedInEntry updatedEntry = new PipelineInputsMetadataDao.MentionedInEntry(
                        existingEntry.pipelineId(), existingEntry.ruleId(), newEntry.connectedStreams());
                mergedMentionsMap.put(key, updatedEntry);
            } else {
                mergedMentionsMap.put(key, newEntry);
            }
        }
        return new ArrayList<>(mergedMentionsMap.values());
    }

    private static String getKey(PipelineInputsMetadataDao.MentionedInEntry entry) {
        return entry.ruleId() + ":" + entry.pipelineId();
    }

    public void deleteInput(String id) {
        final DeleteResult deleteResult = collection.deleteOne(eq(FIELD_INPUT_ID, id));
        if (deleteResult.getDeletedCount() == 1) {
            LOG.info("Deleted pipeline inputs metadata record for input {}", id);
        } else {
            LOG.warn("No pipeline inputs metadata record found for input {}", id);
        }
    }

    public void deleteInputMentionsByRuleId(String ruleId) {
        deleteInputMentionsByPath(PATH_RULE_ID, ruleId, entry -> !entry.ruleId().equals(ruleId));
    }

    public void deleteInputMentionsByPipelineId(String pipelineId) {
        deleteInputMentionsByPath(PATH_PIPELINE_ID, pipelineId, entry -> !entry.pipelineId().equals(pipelineId));
    }

    private void deleteInputMentionsByPath(String path, String id,
                                           java.util.function.Predicate<PipelineInputsMetadataDao.MentionedInEntry> filter) {
        collection.find(eq(path, id))
                .forEach(dao -> {
                    List<PipelineInputsMetadataDao.MentionedInEntry> updatedMentions = dao.mentionedIn().stream()
                            .filter(filter)
                            .toList();
                    if (updatedMentions.isEmpty()) {
                        collection.deleteOne(eq(FIELD_INPUT_ID, dao.inputId()));
                        LOG.info("Deleted pipeline inputs metadata record for input {} as it has no more mentions", dao.inputId());
                        return;
                    }
                    int diff = dao.mentionedIn().size() - updatedMentions.size();
                    if (diff > 0) {
                        PipelineInputsMetadataDao updatedDao = PipelineInputsMetadataDao.builder()
                                .id(dao.id())
                                .inputId(dao.inputId())
                                .mentionedIn(updatedMentions)
                                .build();
                        collection.replaceOne(eq(FIELD_INPUT_ID, dao.inputId()), updatedDao);
                        LOG.info("Removed {} mentions from input {}", diff, dao.inputId());
                    }
                });
    }
}
