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
package org.graylog2.indexer.indexset.template;

import com.google.common.primitives.Ints;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import jakarta.inject.Inject;
import org.bson.conversions.Bson;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.filtering.DbQueryCreator;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.datatiering.hotonly.HotOnlyDataTieringConfig;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.rest.resources.entities.EntityDefaults;
import org.graylog2.rest.resources.entities.Sorting;
import org.graylog2.search.SearchQueryField;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import static org.graylog2.database.utils.MongoUtils.idEq;
import static org.graylog2.database.utils.MongoUtils.insertedIdAsString;
import static org.graylog2.database.utils.MongoUtils.stream;
import static org.graylog2.indexer.indexset.template.IndexSetTemplate.BUILT_IN_FIELD_NAME;
import static org.graylog2.indexer.indexset.template.IndexSetTemplate.DESCRIPTION_FIELD_NAME;
import static org.graylog2.indexer.indexset.template.IndexSetTemplate.ID_FIELD_NAME;
import static org.graylog2.indexer.indexset.template.IndexSetTemplate.INDEX_SET_CONFIG_FIELD_NAME;
import static org.graylog2.indexer.indexset.template.IndexSetTemplate.TITLE_FIELD_NAME;

public class IndexSetTemplateService {
    static final String INDEX_SET_TEMPLATE_MONGO_COLLECTION_NAME = "index_set_templates";

    private static final List<EntityAttribute> ATTRIBUTES = List.of(
            EntityAttribute.builder().id(ID_FIELD_NAME).type(SearchQueryField.Type.OBJECT_ID).title("Template Id").hidden(true).sortable(true).build(),
            EntityAttribute.builder().id(TITLE_FIELD_NAME).title("Template Name").sortable(true).searchable(true).build(),
            EntityAttribute.builder().id(DESCRIPTION_FIELD_NAME).title("Template Description").sortable(false).build(),
            EntityAttribute.builder().id(BUILT_IN_FIELD_NAME).type(SearchQueryField.Type.BOOLEAN).title("Is Built-in").hidden(true).sortable(false).searchable(true).build(),
            EntityAttribute.builder().id(INDEX_SET_CONFIG_FIELD_NAME).title("Custom Config").hidden(true).sortable(false).build()
    );

    private static final EntityDefaults DEFAULTS = EntityDefaults.builder()
            .sort(Sorting.create(IndexSetTemplate.TITLE_FIELD_NAME, Sorting.Direction.valueOf("asc".toUpperCase(Locale.ROOT))))
            .build();
    public static final Bson BUILT_IN_FILTER = Filters.eq(BUILT_IN_FIELD_NAME, true);

    private final MongoCollection<IndexSetTemplate> collection;
    private final DbQueryCreator dbQueryCreator;
    private final MongoUtils<IndexSetTemplate> mongoUtils;

    @Inject
    public IndexSetTemplateService(final MongoCollections mongoCollections) {
        this.collection = mongoCollections.collection(INDEX_SET_TEMPLATE_MONGO_COLLECTION_NAME, IndexSetTemplate.class);
        collection.createIndex(Indexes.ascending(TITLE_FIELD_NAME));
        this.mongoUtils = mongoCollections.utils(collection);
        this.dbQueryCreator = new DbQueryCreator(IndexSetTemplate.TITLE_FIELD_NAME, ATTRIBUTES);
    }

    public Optional<IndexSetTemplate> get(final String templateId) {
        return mongoUtils.getById(templateId);
    }

    public long delete(final String id) {
        return collection.deleteOne(idEq(id)).getDeletedCount();
    }

    public long deleteBuiltIns() {
        return collection.deleteMany(BUILT_IN_FILTER).getDeletedCount();
    }

    public IndexSetTemplate save(IndexSetTemplate indexSetTemplate) {
        final var id = insertedIdAsString(collection.insertOne(indexSetTemplate));
        return new IndexSetTemplate(
                id,
                indexSetTemplate.title(),
                indexSetTemplate.description(),
                indexSetTemplate.isBuiltIn(),
                indexSetTemplate.indexSetConfig()
        );
    }

    public boolean update(final String templateId, final IndexSetTemplate updatedTemplate) {
        return collection.replaceOne(idEq(templateId), updatedTemplate).wasAcknowledged();
    }

    public PageListResponse<IndexSetTemplate> getPaginated(final String query,
                                                           final List<String> filters,
                                                           final int page,
                                                           final int perPage,
                                                           final String sortField,
                                                           final String order) {

        final Bson dbQuery = Filters.and(dbQueryCreator.createDbQuery(filters, query), Filters.ne(BUILT_IN_FIELD_NAME, true));
        final Bson dbSort = "desc".equalsIgnoreCase(order) ? Sorts.descending(sortField) : Sorts.ascending(sortField);

        final long total = collection.countDocuments(dbQuery);
        List<IndexSetTemplate> singlePageOfTemplates = new ArrayList<>(perPage);
        collection.find(dbQuery)
                .sort(dbSort)
                .limit(perPage)
                .skip(perPage * Math.max(0, page - 1))
                .into(singlePageOfTemplates);

        final PaginatedList<IndexSetTemplate> paginated = new PaginatedList<>(
                singlePageOfTemplates.stream().toList(),
                Ints.saturatedCast(total),
                page,
                perPage);

        return PageListResponse.create(query,
                paginated,
                sortField,
                order,
                ATTRIBUTES,
                DEFAULTS);
    }

    public Stream<IndexSetTemplate> streamAll() {
        return stream(collection.find());
    }

    public List<IndexSetTemplate> getBuiltIns(boolean warmTierEnabled) {

        String fieldWarmTierEnabled = "index_set_config.data_tiering.warm_tier_enabled";
        Bson filter;
        if (warmTierEnabled) {
            filter = Filters.eq(fieldWarmTierEnabled, true);
        } else {
            filter = Filters.or(
                    Filters.eq("index_set_config.data_tiering.type", HotOnlyDataTieringConfig.TYPE),
                    Filters.eq(fieldWarmTierEnabled, false)
            );
        }

        ArrayList<IndexSetTemplate> templates = collection.find(Filters.and(
                        BUILT_IN_FILTER,
                        Filters.eq("index_set_config.use_legacy_rotation", false),
                        filter
                ))
                .into(new ArrayList<>());
        templates.sort(Comparator.comparing(indexSetTemplate -> indexSetTemplate.indexSetConfig().dataTieringConfig().indexLifetimeMax().getMillis()));
        return templates;
    }
}
