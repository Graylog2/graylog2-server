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
package org.graylog2.i18n.feedback;

import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.Updates;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.rest.models.SortOrder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.graylog2.database.utils.MongoUtils.idEq;
import static org.graylog2.database.utils.MongoUtils.insertedIdAsString;
import static org.graylog2.database.utils.MongoUtils.stringIdsIn;
import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.FIELD_CREATED_AT;
import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.FIELD_KEY;
import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.FIELD_LANGUAGE;
import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.FIELD_NAMESPACE;
import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.FIELD_RESOLUTION;
import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.FIELD_STATUS;
import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.FIELD_UPDATED_AT;

@Singleton
public class TranslationFeedbackService {
    public static final String COLLECTION_NAME = "translation_feedback";

    private final MongoCollection<TranslationFeedbackDTO> collection;
    private final MongoPaginationHelper<TranslationFeedbackDTO> paginationHelper;
    private final MongoUtils<TranslationFeedbackDTO> mongoUtils;

    @Inject
    public TranslationFeedbackService(MongoCollections mongoCollections) {
        this.collection = mongoCollections.collection(COLLECTION_NAME, TranslationFeedbackDTO.class);
        this.paginationHelper = mongoCollections.paginationHelper(collection);
        this.mongoUtils = mongoCollections.utils(collection);

        collection.createIndex(Indexes.ascending(FIELD_LANGUAGE));
        collection.createIndex(Indexes.ascending(FIELD_STATUS));
        collection.createIndex(Indexes.ascending(FIELD_NAMESPACE, FIELD_KEY));
    }

    public TranslationFeedbackDTO create(CreateTranslationFeedbackRequest request, String reporter) {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final TranslationFeedbackDTO feedback = new TranslationFeedbackDTO(
                null,
                request.language(),
                blankToNull(request.namespace()),
                blankToNull(request.key()),
                request.text(),
                blankToNull(request.englishText()),
                blankToNull(request.suggestion()),
                blankToNull(request.comment()),
                request.category(),
                blankToNull(request.path()),
                blankToNull(request.domPath()),
                reporter,
                TranslationFeedbackDTO.STATUS_OPEN,
                null,
                now,
                now);
        final String id = insertedIdAsString(collection.insertOne(feedback));

        return get(id).orElseThrow();
    }

    public Optional<TranslationFeedbackDTO> get(String id) {
        return mongoUtils.getById(id);
    }

    public PaginatedList<TranslationFeedbackDTO> findPaginated(Bson query, String sortField, SortOrder order, int page, int perPage) {
        return paginationHelper
                .filter(query)
                .sort(order.toBsonSort(sortField))
                .perPage(perPage)
                .page(page);
    }

    public Optional<TranslationFeedbackDTO> updateStatus(String id, String status) {
        return Optional.ofNullable(collection.findOneAndUpdate(
                idEq(id),
                Updates.combine(Updates.set(FIELD_STATUS, status), Updates.set(FIELD_UPDATED_AT, DateTime.now(DateTimeZone.UTC))),
                new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)));
    }

    public boolean delete(String id) {
        return mongoUtils.deleteById(id);
    }

    public BulkTranslationFeedbackResponse updateStatus(Collection<String> ids, String status) {
        final Set<String> validIds = validIds(ids);
        final long changed = validIds.isEmpty() ? 0 : collection.updateMany(
                stringIdsIn(validIds),
                Updates.combine(Updates.set(FIELD_STATUS, status), Updates.set(FIELD_UPDATED_AT, DateTime.now(DateTimeZone.UTC))))
                .getMatchedCount();

        return new BulkTranslationFeedbackResponse(changed, Set.copyOf(ids).size() - changed);
    }

    public BulkTranslationFeedbackResponse delete(Collection<String> ids) {
        final Set<String> validIds = validIds(ids);
        final long deleted = validIds.isEmpty() ? 0 : collection.deleteMany(stringIdsIn(validIds)).getDeletedCount();

        return new BulkTranslationFeedbackResponse(deleted, Set.copyOf(ids).size() - deleted);
    }

    /**
     * Sets status and note of each report. A note which is left out keeps the existing one.
     */
    public BulkTranslationFeedbackResponse applyResolutions(List<TranslationFeedbackResolutions.Resolution> resolutions) {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final List<UpdateOneModel<TranslationFeedbackDTO>> updates = resolutions.stream()
                .filter(resolution -> ObjectId.isValid(resolution.id()))
                .map(resolution -> new UpdateOneModel<TranslationFeedbackDTO>(idEq(resolution.id()), Updates.combine(Stream.of(
                                Updates.set(FIELD_STATUS, resolution.status()),
                                Updates.set(FIELD_UPDATED_AT, now),
                                resolution.note() == null ? null : Updates.set(FIELD_RESOLUTION, blankToNull(resolution.note())))
                        .filter(Objects::nonNull)
                        .toList())))
                .toList();
        final long changed = updates.isEmpty() ? 0 : collection.bulkWrite(updates).getMatchedCount();

        return new BulkTranslationFeedbackResponse(changed, resolutions.size() - changed);
    }

    private static Set<String> validIds(Collection<String> ids) {
        return ids.stream().filter(ObjectId::isValid).collect(Collectors.toSet());
    }

    /**
     * Returns all reports matching the query, ordered so reports of the same text are next to each other.
     */
    public List<TranslationFeedbackDTO> findAll(Bson query) {
        return collection.find(query)
                .sort(Sorts.ascending(FIELD_LANGUAGE, FIELD_NAMESPACE, FIELD_KEY, FIELD_CREATED_AT))
                .into(new ArrayList<>());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
