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
package org.graylog2.database.pagination;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UnwindOptions;
import com.mongodb.client.model.Variable;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog2.database.entities.SourcedMongoEntity;
import org.graylog2.database.entities.source.DBEntitySourceService;
import org.graylog2.database.entities.source.EntitySource;

import java.util.List;

import static com.mongodb.client.model.Aggregates.limit;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Projections.include;

public class EntitySourceLookup {
    private static final List<Bson> LOOKUP_PIPELINE = List.of(
            match(Filters.expr(new Document("$eq", List.of("$" + EntitySource.FIELD_ENTITY_ID, "$$vid")))),
            project(Projections.fields(excludeId(), include(EntitySource.FIELD_SOURCE, EntitySource.FIELD_PARENT_ID))),
            limit(1)
    );

    public static final Bson LOOKUP = Aggregates.lookup(
            DBEntitySourceService.COLLECTION_NAME,
            List.of(new Variable<>("vid", "$_id")),
            LOOKUP_PIPELINE,
            SourcedMongoEntity.FIELD_ENTITY_SOURCE
    );

    public static final Bson UNWIND = Aggregates.unwind("$" + SourcedMongoEntity.FIELD_ENTITY_SOURCE,
            new UnwindOptions().preserveNullAndEmptyArrays(true));

    public static final List<Bson> ENTITY_SOURCE_LOOKUP = List.of(LOOKUP, UNWIND);
}
