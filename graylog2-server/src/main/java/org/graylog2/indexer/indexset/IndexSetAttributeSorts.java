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
package org.graylog2.indexer.indexset;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Variable;
import org.bson.Document;
import org.graylog2.rest.resources.entities.AttributeSortSpec;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamServiceImpl;

import java.util.List;

import static org.graylog2.indexer.indexset.fields.FieldTypeProfileField.FIELD_PROFILE_ID;
import static org.graylog2.indexer.indexset.profile.IndexFieldTypeProfile.NAME_FIELD_NAME;
import static org.graylog2.indexer.indexset.profile.IndexFieldTypeProfileService.INDEX_FIELD_TYPE_PROFILE_MONGO_COLLECTION_NAME;

/**
 * Sort specifications for index set attributes whose sort key lives in another collection.
 * <p>
 * Each spec joins the other collection before the sort, writes the sort key into a temporary field, and
 * removes that field after the sort. The page then deserializes into plain {@link IndexSetConfig} documents.
 * Each join runs once per index set. The collection holds tens to hundreds of documents, so the cost is small.
 */
public final class IndexSetAttributeSorts {
    static final String PROFILE_TITLE_SORT_FIELD = "_sort_field_type_profile";
    static final String STREAM_COUNT_SORT_FIELD = "_sort_stream_count";
    private static final String JOINED_PROFILE = "_profile";
    private static final String JOINED_STREAMS = "_streams";

    private IndexSetAttributeSorts() {
    }

    /** Sorts by the name of the referenced field type profile. Index sets without a profile sort as null. */
    public static AttributeSortSpec profileTitle() {
        // The reference is a hex string. Convert it to an ObjectId for the join. A missing or malformed value
        // yields null instead of failing the page.
        final Document profileObjectId = new Document("$convert", new Document("input", "$" + FIELD_PROFILE_ID)
                .append("to", "objectId")
                .append("onError", null)
                .append("onNull", null));
        return new AttributeSortSpec(
                List.of(
                        Aggregates.lookup(INDEX_FIELD_TYPE_PROFILE_MONGO_COLLECTION_NAME,
                                List.of(new Variable<>("profileId", profileObjectId)),
                                List.of(Aggregates.match(new Document("$expr", new Document("$eq", List.of("$_id", "$$profileId")))),
                                        Aggregates.project(Projections.include(NAME_FIELD_NAME))),
                                JOINED_PROFILE),
                        Aggregates.set(new Field<>(PROFILE_TITLE_SORT_FIELD, new Document("$first", "$" + JOINED_PROFILE + "." + NAME_FIELD_NAME))),
                        Aggregates.unset(JOINED_PROFILE)),
                PROFILE_TITLE_SORT_FIELD,
                List.of(Aggregates.unset(PROFILE_TITLE_SORT_FIELD)));
    }

    /** Sorts by the number of streams routed to the index set. */
    public static AttributeSortSpec streamCount() {
        return new AttributeSortSpec(
                List.of(
                        Aggregates.lookup(StreamServiceImpl.COLLECTION_NAME,
                                List.of(new Variable<>("indexSetId", new Document("$toString", "$_id"))),
                                List.of(Aggregates.match(new Document("$expr", new Document("$eq", List.of("$" + StreamImpl.FIELD_INDEX_SET_ID, "$$indexSetId")))),
                                        Aggregates.project(Projections.include("_id"))),
                                JOINED_STREAMS),
                        Aggregates.set(new Field<>(STREAM_COUNT_SORT_FIELD, new Document("$size", "$" + JOINED_STREAMS))),
                        Aggregates.unset(JOINED_STREAMS)),
                STREAM_COUNT_SORT_FIELD,
                List.of(Aggregates.unset(STREAM_COUNT_SORT_FIELD)));
    }
}
