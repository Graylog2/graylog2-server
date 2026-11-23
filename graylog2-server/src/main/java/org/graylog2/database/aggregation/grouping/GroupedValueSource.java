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

/**
 * Where the value that a document is grouped by comes from.
 * <p>
 * A source can yield several values for one document, when its path leads through arrays. The document then belongs
 * to one bucket per distinct value, and to the missing bucket if there is none.
 */
public sealed interface GroupedValueSource permits FieldSource, RelatedSource {

    /**
     * A field of the document itself.
     */
    static FieldSource field(final String path) {
        return new FieldSource(path);
    }

    /**
     * A field of the documents in another collection that the document references.
     *
     * @see RelatedSource
     */
    static RelatedSource related(final String localField,
                                 final String collection,
                                 final String foreignField,
                                 final String path) {
        return new RelatedSource(localField, collection, foreignField, path, null, false);
    }
}
