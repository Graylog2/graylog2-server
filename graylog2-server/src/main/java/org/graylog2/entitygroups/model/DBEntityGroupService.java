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
package org.graylog2.entitygroups.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationStrength;
import com.mongodb.client.model.IndexOptions;
import org.bson.conversions.Bson;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.entities.EntityScopeService;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.database.utils.ScopedEntityMongoUtils;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;

import jakarta.inject.Inject;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Filters.in;


public class DBEntityGroupService {
    public static final String COLLECTION_NAME = "entity_groups";

    private static final ImmutableMap<String, SearchQueryField> ALLOWED_FIELDS = ImmutableMap.<String, SearchQueryField>builder()
            .put(EntityGroup.FIELD_NAME, SearchQueryField.create(EntityGroup.FIELD_NAME))
            .build();

    private final MongoCollection<EntityGroup> collection;
    private final MongoUtils<EntityGroup> mongoUtils;
    private final ScopedEntityMongoUtils<EntityGroup> scopedEntityMongoUtils;
    private final MongoPaginationHelper<EntityGroup> paginationHelper;
    private final SearchQueryParser searchQueryParser;

    @Inject
    public DBEntityGroupService(MongoCollections mongoCollections,
                                EntityScopeService entityScopeService) {
        this.collection = mongoCollections.collection(COLLECTION_NAME, EntityGroup.class);
        this.mongoUtils = mongoCollections.utils(collection);
        this.scopedEntityMongoUtils = mongoCollections.scopedEntityUtils(collection, entityScopeService);
        this.paginationHelper = mongoCollections.paginationHelper(collection);
        this.searchQueryParser = new SearchQueryParser(EntityGroup.FIELD_ENTITIES, ALLOWED_FIELDS);

        final IndexOptions caseInsensitiveOptions = new IndexOptions()
                .collation(Collation.builder().locale("en").collationStrength(CollationStrength.SECONDARY).build())
                .unique(true);
        collection.createIndex(new BasicDBObject(EntityGroup.FIELD_NAME, 1), caseInsensitiveOptions);
    }

    public Optional<EntityGroup> get(String id) {
        return mongoUtils.getById(id);
    }

    public PaginatedList<EntityGroup> findPaginated(String query, int page, int perPage, Bson sort, Predicate<EntityGroup> filter) {
        final SearchQuery searchQuery = searchQueryParser.parse(query);
        return filter == null ?
                paginationHelper.filter(searchQuery.toBson()).sort(sort).perPage(perPage).page(page) :
                paginationHelper.filter(searchQuery.toBson()).sort(sort).perPage(perPage).page(page, filter);
    }

    public EntityGroup save(EntityGroup entityGroup) {
        if (entityGroup.id() != null) {
            return scopedEntityMongoUtils.update(entityGroup);
        }
        String newId = scopedEntityMongoUtils.create(entityGroup);
        return entityGroup.toBuilder().id(newId).build();
    }

    public Optional<EntityGroup> getByName(String name) {
        final Bson query = eq(EntityGroup.FIELD_NAME, name);

        return Optional.ofNullable(collection.find(query).first());
    }

    public List<EntityGroup> getAllForEntity(String type, String entityId) {
        final Bson query = and(
                exists(typeField(type)),
                in(typeField(type), entityId)
        );
        return MongoUtils.stream(collection.find(query)).toList();
    }

    // TODO: can we make this better..?
    public Map<String, Collection<EntityGroup>> getAllForEntities(String type, Collection<String> entityIds) {
        final Bson query = and(
                exists(typeField(type)),
                in(typeField(type), entityIds)
        );
        final List<EntityGroup> groups = MongoUtils.stream(collection.find(query)).toList();
        final Multimap<String, EntityGroup> entityToGroupsMap = MultimapBuilder.hashKeys().hashSetValues().build();
        for (EntityGroup group : groups) {
            for (Map.Entry<String, Set<String>> StringEntry : group.entities().entrySet()) {
                if (StringEntry.getKey().equals(type)) {
                    for (String entityId : StringEntry.getValue()) {
                        if (entityIds.contains(entityId)) {
                            entityToGroupsMap.put(entityId, group);
                        }
                    }
                }
            }
        }
        return entityToGroupsMap.asMap();
    }

    public long delete(String id) {
        return collection.deleteOne(MongoUtils.idEq(id)).getDeletedCount();
    }

    private String typeField(String type) {
        return EntityGroup.FIELD_ENTITIES + "." + type;
    }
}
