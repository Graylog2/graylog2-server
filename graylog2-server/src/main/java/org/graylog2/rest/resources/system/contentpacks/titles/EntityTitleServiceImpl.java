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
package org.graylog2.rest.resources.system.contentpacks.titles;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog.plugins.views.search.permissions.EntityPermissions;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.dbcatalog.DbEntitiesCatalog;
import org.graylog2.database.dbcatalog.DbEntityCatalogEntry;
import org.graylog2.rest.resources.system.contentpacks.titles.model.EntitiesTitleResponse;
import org.graylog2.rest.resources.system.contentpacks.titles.model.EntityIdentifier;
import org.graylog2.rest.resources.system.contentpacks.titles.model.EntityTitleRequest;
import org.graylog2.rest.resources.system.contentpacks.titles.model.EntityTitleResponse;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static org.graylog2.database.DbEntity.NO_TITLE;
import static org.graylog2.rest.resources.system.contentpacks.titles.model.EntitiesTitleResponse.EMPTY_RESPONSE;

public class EntityTitleServiceImpl implements EntityTitleService {

    private final MongoConnection mongoConnection;
    private final DbEntitiesCatalog entitiesCatalog;

    static final String TITLE_IF_NOT_PERMITTED = "";

    @Inject
    public EntityTitleServiceImpl(final MongoConnection mongoConnection,
                                  final DbEntitiesCatalog entitiesCatalog) {
        this.mongoConnection = mongoConnection;
        this.entitiesCatalog = entitiesCatalog;
    }

    @Override
    public EntitiesTitleResponse getTitles(final EntityTitleRequest request, final EntityPermissions permissions) {
        if (request == null || request.entities() == null) {
            return EMPTY_RESPONSE;
        }

        final Map<String, List<EntityIdentifier>> groupedByType = request.entities()
                .stream()
                .collect(groupingBy(EntityIdentifier::type));

        final Optional<EntitiesTitleResponse> entitiesTitleResponse = groupedByType.entrySet()
                .stream()
                .map(entry -> getTitlesForEntitiesFromSingleCollection(permissions, entry.getKey(), entry.getValue()))
                .reduce(EntitiesTitleResponse::merge);

        return entitiesTitleResponse.orElse(EMPTY_RESPONSE);
    }

    private EntitiesTitleResponse getTitlesForEntitiesFromSingleCollection(final EntityPermissions permissions,
                                                                           final String collection,
                                                                           final List<EntityIdentifier> entities) {
        final Optional<DbEntityCatalogEntry> dbEntityCatalogEntry = this.entitiesCatalog.getByCollectionName(collection);
        if (dbEntityCatalogEntry.isEmpty() || entities.isEmpty()) {
            return EMPTY_RESPONSE;
        }

        final String titleField = dbEntityCatalogEntry.get().titleField();
        if (titleField.equals(NO_TITLE)) {
            return new EntitiesTitleResponse(
                    entities.stream()
                            .map(e -> new EntityTitleResponse(e.id(), e.type(), NO_TITLE))
                            .collect(Collectors.toSet()),
                    Set.of()
            );
        }

        final MongoCollection<Document> mongoCollection = mongoConnection.getMongoDatabase().getCollection(collection);

        Bson bsonFilter = Filters.or(
                entities.stream()
                        .map(e -> Filters.eq("_id", new ObjectId(e.id())))
                        .collect(Collectors.toList())
        );

        final FindIterable<Document> documents = mongoCollection
                .find(bsonFilter)
                .projection(Projections.include(titleField));

        final Set<EntityTitleResponse> titles = new HashSet<>();
        final Set<String> notPermitted = new HashSet<>();
        documents.forEach(doc ->
                {
                    final String idAsString = doc.getObjectId("_id").toString();
                    final boolean canReadTitle = checkCanReadTitle(permissions, dbEntityCatalogEntry.get().readPermission(), idAsString);
                    titles.add(
                            new EntityTitleResponse(
                                    idAsString,
                                    collection,
                                    canReadTitle ? doc.getString(titleField) : TITLE_IF_NOT_PERMITTED
                            )
                    );
                    if (!canReadTitle) {
                        notPermitted.add(idAsString);
                    }
                }
        );

        return new EntitiesTitleResponse(titles, notPermitted);
    }

    private boolean checkCanReadTitle(final EntityPermissions permissions,
                                      final String readPermission,
                                      final String idAsString) {
        return permissions.canReadTitle(readPermission, idAsString);
    }
}
