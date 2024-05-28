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

import com.google.common.primitives.Ints;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Collation;
import org.bson.conversions.Bson;
import org.graylog2.database.MongoEntity;
import org.graylog2.database.PaginatedList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.graylog2.database.utils.MongoUtils.stream;

/**
 * Default implementation for pagination support.
 * <p>
 * Objects of this class are immutable and can be re-used.
 *
 * @param <T> Type of documents in the underlying MongoDB collection.
 */
public class DefaultMongoPaginationHelper<T extends MongoEntity> implements MongoPaginationHelper<T> {

    private final MongoCollection<T> collection;
    private final Bson filter;
    private final Bson sort;
    private final int perPage;
    private final boolean includeGrandTotal;
    private final Bson grandTotalFilter;
    private final Collation collation;

    public DefaultMongoPaginationHelper(MongoCollection<T> collection) {
        this(collection, null, null, 0, false, null, null);
    }

    private DefaultMongoPaginationHelper(MongoCollection<T> collection, Bson filter, Bson sort, int perPage,
                                         boolean includeGrandTotal, Bson grandTotalFilter, Collation collation) {
        this.collection = collection;
        this.filter = filter;
        this.sort = sort;
        this.perPage = perPage;
        this.includeGrandTotal = includeGrandTotal;
        this.grandTotalFilter = grandTotalFilter;
        this.collation = collation;
    }

    @Override
    public MongoPaginationHelper<T> filter(Bson filter) {
        return new DefaultMongoPaginationHelper<>(collection, filter, sort, perPage, includeGrandTotal,
                grandTotalFilter, collation);
    }

    @Override
    public MongoPaginationHelper<T> sort(Bson sort) {
        return new DefaultMongoPaginationHelper<>(collection, filter, sort, perPage, includeGrandTotal,
                grandTotalFilter, collation);
    }

    @Override
    public MongoPaginationHelper<T> perPage(int perPage) {
        return new DefaultMongoPaginationHelper<>(collection, filter, sort, perPage, includeGrandTotal,
                grandTotalFilter, collation);
    }

    @Override
    public MongoPaginationHelper<T> includeGrandTotal(boolean includeGrandTotal) {
        return new DefaultMongoPaginationHelper<>(collection, filter, sort, perPage, includeGrandTotal,
                grandTotalFilter, collation);
    }

    @Override
    public MongoPaginationHelper<T> grandTotalFilter(Bson grandTotalFilter) {
        return new DefaultMongoPaginationHelper<>(collection, filter, sort, perPage, includeGrandTotal,
                grandTotalFilter, collation);
    }

    @Override
    public MongoPaginationHelper<T> collation(Collation collation) {
        return new DefaultMongoPaginationHelper<>(collection, filter, sort, perPage, includeGrandTotal,
                grandTotalFilter, collation);
    }

    @Override
    public PaginatedList<T> page(int pageNumber) {
        final List<T> documents = collection.find()
                .filter(filter)
                .sort(sort)
                .skip(perPage * Math.max(0, pageNumber - 1))
                .limit(perPage)
                .collation(collation)
                .into(new ArrayList<>());
        final int total = Ints.saturatedCast(collection.countDocuments(filter));

        if (includeGrandTotal) {
            final long grandTotal = collection.countDocuments(grandTotalFilter);
            return new PaginatedList<>(documents, total, pageNumber, perPage, grandTotal);
        } else {
            return new PaginatedList<>(documents, total, pageNumber, perPage);
        }
    }

    @Override
    public PaginatedList<T> page(int pageNumber, Predicate<T> selector) {
        final int total = Ints.saturatedCast(stream(collection.find()
                .filter(filter)
                .sort(sort)).filter(selector).count());

        final List<T> documents;
        if (perPage > 0) {
            documents = stream(collection.find().filter(filter).sort(sort).collation(collation))
                    .filter(selector)
                    .skip(perPage * Math.max(0L, pageNumber - 1))
                    .limit(perPage)
                    .toList();
        } else {
            documents = stream(collection.find().filter(filter).sort(sort).collation(collation))
                    .filter(selector).toList();
        }

        if (includeGrandTotal) {
            final long grandTotal = collection.countDocuments(grandTotalFilter);
            return new PaginatedList<>(documents, total, pageNumber, perPage, grandTotal);
        } else {
            return new PaginatedList<>(documents, total, pageNumber, perPage);
        }
    }

}
