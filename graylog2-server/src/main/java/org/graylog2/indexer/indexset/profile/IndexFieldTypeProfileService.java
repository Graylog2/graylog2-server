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
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.filtering.DbQueryCreator;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.rest.resources.entities.EntityDefaults;
import org.graylog2.rest.resources.entities.Sorting;
import org.mongojack.WriteResult;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.indexer.indexset.profile.IndexFieldTypeProfile.CUSTOM_MAPPINGS_FIELD_NAME;
import static org.graylog2.indexer.indexset.profile.IndexFieldTypeProfile.DESCRIPTION_FIELD_NAME;
import static org.graylog2.indexer.indexset.profile.IndexFieldTypeProfile.ID_FIELD_NAME;
import static org.graylog2.indexer.indexset.profile.IndexFieldTypeProfile.NAME_FIELD_NAME;
import static org.graylog2.plugin.Message.FIELDS_UNCHANGEABLE_BY_CUSTOM_MAPPINGS;


public class IndexFieldTypeProfileService extends PaginatedDbService<IndexFieldTypeProfile> {

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

    private final MongoCollection<IndexFieldTypeProfile> profileCollection;
    private final DbQueryCreator dbQueryCreator;
    private final IndexFieldTypeProfileUsagesService indexFieldTypeProfileUsagesService;
    private final IndexSetService indexSetService;

    @Inject
    public IndexFieldTypeProfileService(final MongoConnection mongoConnection,
                                        final MongoJackObjectMapperProvider mapper,
                                        final MongoCollections mongoCollections,
                                        final IndexFieldTypeProfileUsagesService indexFieldTypeProfileUsagesService,
                                        final IndexSetService indexSetService) {
        super(mongoConnection, mapper, IndexFieldTypeProfile.class, INDEX_FIELD_TYPE_PROFILE_MONGO_COLLECTION_NAME);
        this.db.createIndex(new BasicDBObject(IndexFieldTypeProfile.NAME_FIELD_NAME, 1), new BasicDBObject("unique", false));
        this.profileCollection = mongoCollections.get(INDEX_FIELD_TYPE_PROFILE_MONGO_COLLECTION_NAME, IndexFieldTypeProfile.class);
        this.indexSetService = indexSetService;
        this.dbQueryCreator = new DbQueryCreator(IndexFieldTypeProfile.NAME_FIELD_NAME, ATTRIBUTES);
        this.indexFieldTypeProfileUsagesService = indexFieldTypeProfileUsagesService;
    }

    public Optional<IndexFieldTypeProfileWithUsages> getWithUsages(final String profileId) {
        final Optional<IndexFieldTypeProfile> indexFieldTypeProfile = this.get(profileId);

        return indexFieldTypeProfile.map(profile ->
                new IndexFieldTypeProfileWithUsages(
                        profile,
                        indexFieldTypeProfileUsagesService.usagesOfProfile(profile.id())
                )
        );
    }

    @Override
    public IndexFieldTypeProfile save(final IndexFieldTypeProfile indexFieldTypeProfile) {
        indexFieldTypeProfile.customFieldMappings().forEach(mapping -> checkFieldTypeCanBeChanged(mapping.fieldName()));
        return super.save(indexFieldTypeProfile);
    }

    @Override
    public int delete(final String id) {
        int numRemoved = super.delete(id);
        indexSetService.removeReferencesToProfile(id);
        return numRemoved;
    }

    public boolean update(final String profileId, final IndexFieldTypeProfile updatedProfile) {
        updatedProfile.customFieldMappings().forEach(mapping -> checkFieldTypeCanBeChanged(mapping.fieldName()));
        final WriteResult<IndexFieldTypeProfile, ObjectId> writeResult = db.updateById(new ObjectId(profileId), updatedProfile);
        return writeResult.getN() > 0;
    }

    public PageListResponse<IndexFieldTypeProfileWithUsages> getPaginated(final String query,
                                                                          final List<String> filters,
                                                                          final int page,
                                                                          final int perPage,
                                                                          final String sortField,
                                                                          final String order) {

        final Bson dbQuery = dbQueryCreator.createDbQuery(filters, query);
        final Bson dbSort = "desc".equalsIgnoreCase(order) ? Sorts.descending(sortField) : Sorts.ascending(sortField);

        final long total = profileCollection.countDocuments(dbQuery);
        List<IndexFieldTypeProfile> singlePageOfProfiles = new ArrayList<>(perPage);
        profileCollection.find(dbQuery)
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
        return profileCollection.find()
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
