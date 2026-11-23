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
package org.graylog2.database.aggregation.grouping;

import jakarta.annotation.Nullable;
import org.bson.conversions.Bson;

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
public record RelatedSource(String localField,
                            String collection,
                            String foreignField,
                            String path,
                            @Nullable Bson filter,
                            boolean localFieldAsObjectId) implements GroupedValueSource {
    public RelatedSource {
        FieldPaths.requireFieldPath(localField);
        FieldPaths.requireCollectionName(collection);
        FieldPaths.requireFieldPath(foreignField);
        FieldPaths.requireFieldPath(path);
    }

    public RelatedSource withFilter(final Bson filter) {
        return new RelatedSource(localField, collection, foreignField, path, filter, localFieldAsObjectId);
    }

    public RelatedSource withLocalFieldAsObjectId() {
        return new RelatedSource(localField, collection, foreignField, path, filter, true);
    }
}
