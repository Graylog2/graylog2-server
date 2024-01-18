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

/**
 * Custom implementation of a DBCursor to support legacy code. Can only be used for find operations.
 *
 * @param <T>
 */
@NotThreadSafe
public class DBCursor<T> implements Closeable, Iterator<T>, Iterable<T> {
    private final JacksonMongoCollection<T> collection;
    private final FindIterable<T> findIterable;
    private final Bson filter;
    private MongoCursor<T> cursor;

    public DBCursor(JacksonMongoCollection<T> collection) {
        this(collection, null);
    }

    public DBCursor(JacksonMongoCollection<T> collection, Bson filter) {
        this.collection = collection;
        this.findIterable = filter == null ? collection.find() : collection.find(filter);
        this.filter = filter;
    }

    public DBCursor<T> sort(Bson sort) {
        findIterable.sort(sort);
        return this;
    }

    public DBCursor<T> limit(int limit) {
        findIterable.limit(limit);
        return this;
    }

    public DBCursor<T> skip(int skip) {
        findIterable.skip(skip);
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

    public int count() {
        return Ints.saturatedCast(collection.countDocuments(filter));
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return this;
    }

    public Document explain() {
        return findIterable.explain();
    }
}
