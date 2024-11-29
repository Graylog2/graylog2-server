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
package org.graylog2.indexer.indexset.profile;

import com.google.common.primitives.Ints;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.filtering.DbQueryCreator;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.rest.resources.entities.EntityDefaults;
import org.graylog2.rest.resources.entities.Sorting;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.database.utils.MongoUtils.idEq;
import static org.graylog2.database.utils.MongoUtils.insertedIdAsString;
import static org.graylog2.indexer.indexset.profile.IndexFieldTypeProfile.CUSTOM_MAPPINGS_FIELD_NAME;
import static org.graylog2.indexer.indexset.profile.IndexFieldTypeProfile.DESCRIPTION_FIELD_NAME;
import static org.graylog2.indexer.indexset.profile.IndexFieldTypeProfile.ID_FIELD_NAME;
import static org.graylog2.indexer.indexset.profile.IndexFieldTypeProfile.NAME_FIELD_NAME;
import static org.graylog2.plugin.Message.FIELDS_UNCHANGEABLE_BY_CUSTOM_MAPPINGS;


public class IndexFieldTypeProfileService {

    static final String INDEX_FIELD_TYPE_PROFILE_MONGO_COLLECTION_NAME = "index_field_type_profiles";

    private static final List<EntityAttribute> ATTRIBUTES = List.of(
            EntityAttribute.builder().id(ID_FIELD_NAME).title("Profile Id").hidden(true).sortable(true).build(),
            EntityAttribute.builder().id(NAME_FIELD_NAME).title("Profile Name")
                    .sortable(true)
                    .filterable(true)
                    .searchable(true)
                    .build(),
            EntityAttribute.builder().id(DESCRIPTION_FIELD_NAME).title("Profile Description")
                    .sortable(false)
                    .filterable(true)
                    .searchable(true)
                    .build(),
            EntityAttribute.builder().id(CUSTOM_MAPPINGS_FIELD_NAME).title("Custom Field Mappings").sortable(false).build()
    );

    private static final EntityDefaults DEFAULTS = EntityDefaults.builder()
            .sort(Sorting.create(IndexFieldTypeProfile.NAME_FIELD_NAME, Sorting.Direction.valueOf("asc".toUpperCase(Locale.ROOT))))
            .build();

    private final MongoCollection<IndexFieldTypeProfile> collection;
    private final DbQueryCreator dbQueryCreator;
    private final IndexFieldTypeProfileUsagesService indexFieldTypeProfileUsagesService;
    private final IndexSetService indexSetService;
    private final MongoUtils<IndexFieldTypeProfile> mongoUtils;

    @Inject
    public IndexFieldTypeProfileService(final MongoCollections mongoCollections,
                                        final IndexFieldTypeProfileUsagesService indexFieldTypeProfileUsagesService,
                                        final IndexSetService indexSetService) {
        this.indexSetService = indexSetService;
        this.dbQueryCreator = new DbQueryCreator(IndexFieldTypeProfile.NAME_FIELD_NAME, ATTRIBUTES);
        this.indexFieldTypeProfileUsagesService = indexFieldTypeProfileUsagesService;

        collection = mongoCollections.collection(INDEX_FIELD_TYPE_PROFILE_MONGO_COLLECTION_NAME, IndexFieldTypeProfile.class);
        mongoUtils = mongoCollections.utils(collection);

        collection.createIndex(Indexes.ascending(IndexFieldTypeProfile.NAME_FIELD_NAME));
    }

    public Optional<IndexFieldTypeProfile> get(final String profileId) {
        if (!ObjectId.isValid(profileId)) {
            return Optional.empty();
        }
        return mongoUtils.getById(profileId);
    }

    public Optional<IndexFieldTypeProfileWithUsages> getWithUsages(final String profileId) {
        if (!ObjectId.isValid(profileId)) {
            return Optional.empty();
        }
        final Optional<IndexFieldTypeProfile> indexFieldTypeProfile = this.get(profileId);

        return indexFieldTypeProfile.map(profile ->
                new IndexFieldTypeProfileWithUsages(
                        profile,
                        indexFieldTypeProfileUsagesService.usagesOfProfile(profile.id())
                )
        );
    }

    public IndexFieldTypeProfile save(final IndexFieldTypeProfile profile) {
        profile.customFieldMappings().forEach(mapping -> checkFieldTypeCanBeChanged(mapping.fieldName()));

        final var id = profile.id();
        if (id == null) {
            final var insertedId = insertedIdAsString(collection.insertOne(profile));
            return new IndexFieldTypeProfile(insertedId, profile.name(), profile.description(),
                    profile.customFieldMappings());
        } else {
            collection.replaceOne(idEq(id), profile, new ReplaceOptions().upsert(true));
            return profile;
        }
    }

    public int delete(final String id) {
        if (!ObjectId.isValid(id)) {
            return 0;
        }
        int numRemoved = mongoUtils.deleteById(id) ? 1 : 0;
        indexSetService.removeReferencesToProfile(id);
        return numRemoved;
    }

    public boolean update(final String profileId, final IndexFieldTypeProfile updatedProfile) {
        if (!ObjectId.isValid(profileId)) {
            return false;
        }
        updatedProfile.customFieldMappings().forEach(mapping -> checkFieldTypeCanBeChanged(mapping.fieldName()));
        return collection.replaceOne(idEq(profileId), updatedProfile).getMatchedCount() > 0;
    }

    public PageListResponse<IndexFieldTypeProfileWithUsages> getPaginated(final String query,
                                                                          final List<String> filters,
                                                                          final int page,
                                                                          final int perPage,
                                                                          final String sortField,
                                                                          final String order) {

        final Bson dbQuery = dbQueryCreator.createDbQuery(filters, query);
        final Bson dbSort = "desc".equalsIgnoreCase(order) ? Sorts.descending(sortField) : Sorts.ascending(sortField);

        final long total = collection.countDocuments(dbQuery);
        List<IndexFieldTypeProfile> singlePageOfProfiles = new ArrayList<>(perPage);
        collection.find(dbQuery)
                .sort(dbSort)
                .limit(perPage)
                .skip(perPage * Math.max(0, page - 1))
                .into(singlePageOfProfiles);

        final Map<String, Set<String>> profileUsagesInIndexSets = indexFieldTypeProfileUsagesService.usagesOfProfiles(singlePageOfProfiles.stream().map(IndexFieldTypeProfile::id).collect(Collectors.toSet()));

        final PaginatedList<IndexFieldTypeProfileWithUsages> paginated = new PaginatedList<>(
                singlePageOfProfiles.stream()
                        .map(profile -> new IndexFieldTypeProfileWithUsages(profile, profileUsagesInIndexSets.get(profile.id())))
                        .collect(Collectors.toList()),
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

    @Deprecated
    //This method has been introduced only because of technical debt in FE. Do not use it elsewhere! Paginated access is the proper way to go.
    public List<IndexFieldTypeProfileIdAndName> getAll() {
        return collection.find()
                .projection(Projections.include(ID_FIELD_NAME, NAME_FIELD_NAME))
                .sort(Sorts.ascending(NAME_FIELD_NAME))
                .map(profile -> new IndexFieldTypeProfileIdAndName(profile.id(), profile.name()))
                .into(new LinkedList<>());
    }

    private void checkFieldTypeCanBeChanged(final String fieldName) {
        if (FIELDS_UNCHANGEABLE_BY_CUSTOM_MAPPINGS.contains(fieldName)) {
            throw new BadRequestException("Unable to change field type of " + fieldName + ", not allowed to change type of these fields: " + FIELDS_UNCHANGEABLE_BY_CUSTOM_MAPPINGS);
        }
    }

}
