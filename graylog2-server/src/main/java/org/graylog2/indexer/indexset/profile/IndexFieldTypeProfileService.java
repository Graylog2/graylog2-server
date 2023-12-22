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

import com.mongodb.BasicDBObject;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.rest.resources.entities.EntityDefaults;
import org.graylog2.rest.resources.entities.Sorting;
import org.mongojack.DBQuery;
import org.mongojack.WriteResult;

import java.util.List;
import java.util.Locale;

import static org.graylog2.indexer.indexset.profile.IndexFieldTypeProfile.CUSTOM_MAPPINGS_FIELD_NAME;
import static org.graylog2.indexer.indexset.profile.IndexFieldTypeProfile.DESCRIPTION_FIELD_NAME;
import static org.graylog2.indexer.indexset.profile.IndexFieldTypeProfile.ID_FIELD_NAME;
import static org.graylog2.indexer.indexset.profile.IndexFieldTypeProfile.NAME_FIELD_NAME;


public class IndexFieldTypeProfileService extends PaginatedDbService<IndexFieldTypeProfile> {

    static final String INDEX_FIELD_TYPE_PROFILE_MONGO_COLLECTION_NAME = "index_field_type_profiles";

    private static final List<EntityAttribute> ATTRIBUTES = List.of(
            EntityAttribute.builder().id(ID_FIELD_NAME).title("Profile Id").hidden(true).sortable(true).build(),
            EntityAttribute.builder().id(NAME_FIELD_NAME).title("Profile Name").sortable(true).build(),
            EntityAttribute.builder().id(DESCRIPTION_FIELD_NAME).title("Profile Description").sortable(false).build(),
            EntityAttribute.builder().id(CUSTOM_MAPPINGS_FIELD_NAME).title("Custom Field Mappings").sortable(false).build()
    );

    private static final EntityDefaults DEFAULTS = EntityDefaults.builder()
            .sort(Sorting.create(IndexFieldTypeProfile.NAME_FIELD_NAME, Sorting.Direction.valueOf("asc".toUpperCase(Locale.ROOT))))
            .build();

    @Inject
    public IndexFieldTypeProfileService(final MongoConnection mongoConnection,
                                        final MongoJackObjectMapperProvider mapper) {
        super(mongoConnection, mapper, IndexFieldTypeProfile.class, INDEX_FIELD_TYPE_PROFILE_MONGO_COLLECTION_NAME);
        this.db.createIndex(new BasicDBObject(IndexFieldTypeProfile.NAME_FIELD_NAME, 1), new BasicDBObject("unique", false));
    }

    public boolean update(final String profileId, final IndexFieldTypeProfile updatedProfile) {
        final WriteResult<IndexFieldTypeProfile, ObjectId> writeResult = db.updateById(new ObjectId(profileId), updatedProfile);
        return writeResult.getN() > 0;
    }

    public PageListResponse<IndexFieldTypeProfile> getPaginated(final int page,
                                                                final int perPage,
                                                                final String sortField,
                                                                final String order) {

        final PaginatedList<IndexFieldTypeProfile> paginated = findPaginatedWithQueryAndSort(
                DBQuery.empty(),
                getSortBuilder(order, sortField),
                page,
                perPage);
        final int total = paginated.grandTotal().orElse(0L).intValue();
        return PageListResponse.create("",
                paginated,
                sortField,
                order,
                ATTRIBUTES,
                DEFAULTS);
    }

}
