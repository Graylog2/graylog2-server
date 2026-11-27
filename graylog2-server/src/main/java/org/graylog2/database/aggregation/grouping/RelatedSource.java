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
 * A field of the documents in another collection that are joined to the grouped document, like a column of a joined
 * table: the related documents whose {@code relatedKey} equals the document's {@code localKey}. The reference may
 * point either way, e.g. from an event's {@code owner} to a user's {@code _id}, or from an investigation's
 * {@code events} to an event's id.
 * <p>
 * Unlike foreign keys in a relational database, nothing guarantees that a reference resolves. Either key may be an
 * array, in which case any shared element joins the documents. Documents without a reference, or whose related
 * documents don't exist, count as missing.
 * <p>
 * The lookup runs once per matched document, so {@code relatedKey} should be indexed and the request filter
 * selective. Each distinct related value counts once per document, even if several related documents share it.
 *
 * @param localKey           the grouped document's join key
 * @param relatedCollection  the collection joined to, in the same database
 * @param relatedKey         the related documents' join key, e.g. {@code _id}
 * @param groupByField       the field of the related documents whose values become buckets
 * @param relatedFilter      restricts the related documents joined, e.g. to those the user may see. A document whose
 *                           related documents all fail it counts as missing.
 * @param localKeyAsObjectId whether to convert the local key to an ObjectId before joining. Graylog usually stores
 *                           references as strings, while {@code _id} holds ObjectIds; values that aren't valid
 *                           ObjectIds don't join anything.
 */
public record RelatedSource(String localKey,
                            String relatedCollection,
                            String relatedKey,
                            String groupByField,
                            @Nullable Bson relatedFilter,
                            boolean localKeyAsObjectId) implements GroupedValueSource {
    public RelatedSource {
        FieldPaths.requireFieldPath(localKey);
        FieldPaths.requireCollectionName(relatedCollection);
        FieldPaths.requireFieldPath(relatedKey);
        FieldPaths.requireFieldPath(groupByField);
    }

    public RelatedSource withRelatedFilter(final Bson relatedFilter) {
        return new RelatedSource(localKey, relatedCollection, relatedKey, groupByField, relatedFilter,
                localKeyAsObjectId);
    }

    public RelatedSource withLocalKeyAsObjectId() {
        return new RelatedSource(localKey, relatedCollection, relatedKey, groupByField, relatedFilter, true);
    }
}
