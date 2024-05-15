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

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Sorts;
import jakarta.inject.Inject;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.filtering.DbQueryCreator;
import org.graylog2.database.jackson.legacy.LegacyDeleteResult;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.rest.resources.entities.EntityDefaults;
import org.graylog2.rest.resources.entities.Sorting;
import org.graylog2.search.SearchQueryField;
import org.mongojack.DBQuery;
import org.mongojack.WriteResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.graylog2.indexer.indexset.template.IndexSetTemplate.BUILT_IN_FIELD_NAME;
import static org.graylog2.indexer.indexset.template.IndexSetTemplate.DESCRIPTION_FIELD_NAME;
import static org.graylog2.indexer.indexset.template.IndexSetTemplate.ID_FIELD_NAME;
import static org.graylog2.indexer.indexset.template.IndexSetTemplate.INDEX_SET_CONFIG_FIELD_NAME;
import static org.graylog2.indexer.indexset.template.IndexSetTemplate.TITLE_FIELD_NAME;

public class IndexSetTemplateService extends PaginatedDbService<IndexSetTemplate> {
    static final String INDEX_SET_TEMPLATE_MONGO_COLLECTION_NAME = "index_set_templates";

    private static final List<EntityAttribute> ATTRIBUTES = List.of(
            EntityAttribute.builder().id(ID_FIELD_NAME).type(SearchQueryField.Type.OBJECT_ID).title("Template Id").hidden(true).sortable(true).build(),
            EntityAttribute.builder().id(TITLE_FIELD_NAME).title("Template Name").sortable(true).filterable(true).searchable(true).build(),
            EntityAttribute.builder().id(DESCRIPTION_FIELD_NAME).title("Template Description").sortable(false).build(),
            EntityAttribute.builder().id(BUILT_IN_FIELD_NAME).type(SearchQueryField.Type.BOOLEAN).title("Is Built-in").sortable(false).build(),
            EntityAttribute.builder().id(INDEX_SET_CONFIG_FIELD_NAME).title("Custom Config").sortable(false).build()
    );

    private static final EntityDefaults DEFAULTS = EntityDefaults.builder()
            .sort(Sorting.create(IndexSetTemplate.TITLE_FIELD_NAME, Sorting.Direction.valueOf("asc".toUpperCase(Locale.ROOT))))
            .build();

    private final MongoCollection<IndexSetTemplate> templateCollection;
    private final DbQueryCreator dbQueryCreator;

    @Inject
    public IndexSetTemplateService(final MongoConnection mongoConnection,
                                   final MongoJackObjectMapperProvider mapper,
                                   final MongoCollections mongoCollections) {
        super(mongoConnection, mapper, IndexSetTemplate.class, INDEX_SET_TEMPLATE_MONGO_COLLECTION_NAME);
        this.db.createIndex(new BasicDBObject(TITLE_FIELD_NAME, 1), new BasicDBObject("unique", false));
        this.templateCollection = mongoCollections.get(INDEX_SET_TEMPLATE_MONGO_COLLECTION_NAME, IndexSetTemplate.class);
        this.dbQueryCreator = new DbQueryCreator(IndexSetTemplate.TITLE_FIELD_NAME, ATTRIBUTES);
    }

    @Override
    public Optional<IndexSetTemplate> get(final String templateId) {
        if (!ObjectId.isValid(templateId)) {
            return Optional.empty();
        }
        return super.get(templateId);
    }

    @Override
    public int delete(final String id) {
        if (!ObjectId.isValid(id)) {
            return 0;
        }
        return super.delete(id);
    }

    public int deleteBuiltIns() {
        final LegacyDeleteResult<IndexSetTemplate, ObjectId> removed = db.remove(DBQuery.is(BUILT_IN_FIELD_NAME, true));
        return removed.getN();
    }

    public boolean update(final String templateId, final IndexSetTemplate updatedTemplate) {
        if (!ObjectId.isValid(templateId)) {
            return false;
        }
        final WriteResult<IndexSetTemplate, ObjectId> writeResult = db.updateById(new ObjectId(templateId), updatedTemplate);
        return writeResult.getN() > 0;
    }

    public PageListResponse<IndexSetTemplate> getPaginated(final String query,
                                                           final List<String> filters,
                                                           final int page,
                                                           final int perPage,
                                                           final String sortField,
                                                           final String order) {

        final Bson dbQuery = dbQueryCreator.createDbQuery(filters, query);
        final Bson dbSort = "desc".equalsIgnoreCase(order) ? Sorts.descending(sortField) : Sorts.ascending(sortField);

        final long total = templateCollection.countDocuments(dbQuery);
        List<IndexSetTemplate> singlePageOfTemplates = new ArrayList<>(perPage);
        templateCollection.find(dbQuery)
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

    public Collection<IndexSetTemplate> getBuiltIns() {
        return findByQuery(DBQuery.is(BUILT_IN_FIELD_NAME, true));
    }

    private Collection<IndexSetTemplate> findByQuery(DBQuery.Query query) {
        return ImmutableList.copyOf((Iterable<IndexSetTemplate>) db.find(query));
    }
}
