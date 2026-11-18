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
package org.graylog2.database.aggregation;

import jakarta.annotation.Nullable;
import org.bson.conversions.Bson;

/**
 * Where the value that a document is grouped by comes from.
 * <p>
 * A source can yield several values for one document, when its path leads through arrays. The document then belongs
 * to one bucket per distinct value, and to the missing bucket if there is none.
 */
public sealed interface ValueSource {

    /**
     * A field of the document itself.
     */
    static ValueSource.Field field(final String path) {
        return new Field(path);
    }

    /**
     * A field of the documents in another collection that the document references.
     *
     * @see Related
     */
    static ValueSource.Related related(final String localField,
                                       final String collection,
                                       final String foreignField,
                                       final String path) {
        return new Related(localField, collection, foreignField, path, null, false);
    }

    /**
     * @param path the field path, which may lead through arrays of embedded documents, e.g. {@code assets.id}
     */
    record Field(String path) implements ValueSource {
        public Field {
            FieldPaths.requireFieldPath(path);
        }
    }

    /**
     * A field of the documents in {@code collection} whose {@code foreignField} equals the document's
     * {@code localField}. Either side may be an array, in which case any shared element links the documents.
     * Documents without a reference, or whose related documents don't exist, count as missing.
     * <p>
     * The lookup runs once per matched document, so {@code foreignField} should be indexed and the request filter
     * selective. Each distinct related value counts once per document, even if several related documents share it.
     *
     * @param localField           the field of the grouped document holding the reference
     * @param collection           the related collection, in the same database
     * @param foreignField         the field of the related documents that the reference points to, e.g. {@code _id}
     * @param path                 the field of the related documents to group by
     * @param filter               restricts the related documents considered, e.g. to those the user may see.
     *                             A document whose related documents all fail it counts as missing.
     * @param localFieldAsObjectId whether to convert the reference to an ObjectId before matching. Graylog usually
     *                             stores references as strings, while {@code _id} holds ObjectIds; values that
     *                             aren't valid ObjectIds don't match anything.
     */
    record Related(String localField,
                   String collection,
                   String foreignField,
                   String path,
                   @Nullable Bson filter,
                   boolean localFieldAsObjectId) implements ValueSource {
        public Related {
            FieldPaths.requireFieldPath(localField);
            FieldPaths.requireCollectionName(collection);
            FieldPaths.requireFieldPath(foreignField);
            FieldPaths.requireFieldPath(path);
        }

        public Related withFilter(final Bson filter) {
            return new Related(localField, collection, foreignField, path, filter, localFieldAsObjectId);
        }

        public Related withLocalFieldAsObjectId() {
            return new Related(localField, collection, foreignField, path, filter, true);
        }
    }
}
