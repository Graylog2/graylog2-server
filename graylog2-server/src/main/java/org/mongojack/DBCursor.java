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
package org.mongojack;

import com.google.common.primitives.Ints;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

/**
 * Compatibility layer to support existing code interacting with the Mongojack 2.x API.
 *
 * @deprecated use {@link org.graylog2.database.MongoCollections} as an entrypoint for interacting with MongoDB.
 */
@NotThreadSafe
@Deprecated
public class DBCursor<T> implements Closeable, Iterator<T>, Iterable<T> {
    private final JacksonMongoCollection<T> collection;
    private final FindIterable<T> findIterable;
    private final Bson filter;
    private MongoCursor<T> cursor;

    public DBCursor(JacksonMongoCollection<T> collection, Bson filter, Supplier<FindIterable<T>> findIterableSupplier) {
        this.collection = collection;
        this.findIterable = findIterableSupplier.get();
        this.filter = filter;
    }

    public DBCursor<T> sort(Bson sort) {
        findIterable.sort(sort);
        refreshCursor();
        return this;
    }

    public DBCursor<T> limit(int limit) {
        findIterable.limit(limit);
        refreshCursor();
        return this;
    }

    public DBCursor<T> skip(int skip) {
        findIterable.skip(skip);
        refreshCursor();
        return this;
    }

    @Override
    public void close() {
        if (this.cursor != null) {
            this.cursor.close();
        }
    }

    public List<T> toArray() {
        return findIterable.into(new ArrayList<>());
    }

    @Override
    public boolean hasNext() {
        return getCursor().hasNext();
    }

    @Override
    public T next() {
        return getCursor().next();
    }

    private MongoCursor<T> getCursor() {
        if (this.cursor == null) {
            this.cursor = findIterable.iterator();
        }
        return cursor;
    }

    private void refreshCursor() {
        if (this.cursor != null) {
            cursor.close();
            cursor = null;
        }
    }

    public int count() {
        return Ints.saturatedCast(collection.countDocuments(filter));
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return new DBCursor<>(collection, filter, () -> findIterable);
    }

    public Document explain() {
        return findIterable.explain();
    }
}
