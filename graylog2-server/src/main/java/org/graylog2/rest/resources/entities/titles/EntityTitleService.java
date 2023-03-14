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
package org.graylog2.rest.resources.entities.titles;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import org.apache.shiro.subject.Subject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog2.database.DbEntity;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.dbcatalog.DbEntitiesCatalog;
import org.graylog2.database.dbcatalog.DbEntityCatalogEntry;
import org.graylog2.rest.resources.entities.titles.model.EntitiesTitleResponse;
import org.graylog2.rest.resources.entities.titles.model.EntityIdentifier;
import org.graylog2.rest.resources.entities.titles.model.EntityTitleRequest;
import org.graylog2.rest.resources.entities.titles.model.EntityTitleResponse;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static org.graylog2.database.DbEntity.NO_TITLE;

public class EntityTitleService {

    private final MongoConnection mongoConnection;
    private final DbEntitiesCatalog entitiesCatalog;

    static final String TITLE_IF_NOT_PERMITTED = "<not permitted to view>";

    @Inject
    public EntityTitleService(final MongoConnection mongoConnection,
                              final DbEntitiesCatalog entitiesCatalog) {
        this.mongoConnection = mongoConnection;
        this.entitiesCatalog = entitiesCatalog;
    }

    public EntitiesTitleResponse getTitles(final Subject subject, final EntityTitleRequest request) {

        final Map<String, List<EntityIdentifier>> groupedByType = request.entities()
                .stream()
                .collect(groupingBy(EntityIdentifier::type));

        final Optional<EntitiesTitleResponse> entitiesTitleResponse = groupedByType.entrySet()
                .stream()
                .map(entry -> getTitlesForEntitiesFromSingleCollection(subject, entry.getKey(), entry.getValue()))
                .reduce(EntitiesTitleResponse::merge);

        return entitiesTitleResponse.orElse(new EntitiesTitleResponse(List.of()));
    }

    public EntitiesTitleResponse getTitlesForEntitiesFromSingleCollection(final Subject subject,
                                                                          final String collection,
                                                                          final List<EntityIdentifier> entities) {
        final Optional<DbEntityCatalogEntry> dbEntityCatalogEntry = this.entitiesCatalog.getByCollectionName(collection);
        if (dbEntityCatalogEntry.isEmpty() || entities.isEmpty()) {
            return new EntitiesTitleResponse(List.of());
        }

        final String titleField = dbEntityCatalogEntry.get().titleField();
        if (titleField.equals(NO_TITLE)) {
            return new EntitiesTitleResponse(
                    entities.stream()
                            .map(e -> new EntityTitleResponse(e.id(), e.type(), ""))
                            .collect(Collectors.toList())
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

        final List<EntityTitleResponse> titles = documents
                .map(doc ->
                        {
                            final String idAsString = doc.getObjectId("_id").toString();
                            return new EntityTitleResponse(
                                    idAsString,
                                    collection,
                                    getTitle(subject, dbEntityCatalogEntry.get().readPermission(), titleField, doc, idAsString)
                            );
                        }
                )
                .into(new ArrayList<>());

        return new EntitiesTitleResponse(titles);
    }

    private String getTitle(Subject subject,
                            String readPermission,
                            String titleField,
                            Document doc,
                            String idAsString) {
        if (DbEntity.NO_PERMISSION.equals(readPermission) || subject.isPermitted(readPermission + ":" + idAsString)) {
            return doc.getString(titleField);
        } else {
            return TITLE_IF_NOT_PERMITTED;
        }
    }
}
