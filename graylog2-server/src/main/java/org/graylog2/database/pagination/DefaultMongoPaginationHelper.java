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
import com.mongodb.client.model.Sorts;
import org.bson.conversions.Bson;
import org.graylog2.database.PaginatedList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.graylog2.database.utils.MongoUtils.stream;

public class DefaultMongoPaginationHelper<T> implements MongoPaginationHelper<T> {

    private final MongoCollection<T> collection;
    private Bson filter;
    private Bson sort;
    private int perPage;
    private boolean includeGrandTotal;
    private Bson grandTotalFilter;

    public DefaultMongoPaginationHelper(MongoCollection<T> collection) {
        this.collection = collection;
    }

    @Override
    public MongoPaginationHelper<T> filter(Bson filter) {
        this.filter = filter;
        return this;
    }

    @Override
    public MongoPaginationHelper<T> sort(Bson sort) {
        this.sort = sort;
        return this;
    }

    @Override
    public MongoPaginationHelper<T> sort(String fieldName, String order) {
        if ("desc".equalsIgnoreCase(order)) {
            this.sort = Sorts.descending(fieldName);
        } else {
            this.sort = Sorts.ascending(fieldName);
        }
        return this;
    }

    @Override
    public MongoPaginationHelper<T> perPage(int perPage) {
        this.perPage = perPage;
        return this;
    }

    @Override
    public MongoPaginationHelper<T> includeGrandTotal(boolean includeGrandTotal) {
        this.includeGrandTotal = includeGrandTotal;
        return this;
    }

    @Override
    public MongoPaginationHelper<T> grandTotalFilter(Bson grandTotalFilter) {
        this.grandTotalFilter = grandTotalFilter;
        return this;
    }

    @Override
    public PaginatedList<T> page(int pageNumber) {
        final List<T> documents = collection.find()
                .filter(filter)
                .sort(sort)
                .skip(perPage * Math.max(0, pageNumber - 1))
                .limit(perPage)
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
    public PaginatedList<T> postProcessedPage(int pageNumber, Predicate<T> selector) {
        final int total = Ints.saturatedCast(stream(collection.find()
                .filter(filter)
                .sort(sort)).filter(selector).count());

        final List<T> documents;
        if (perPage > 0) {
            documents = stream(collection.find().filter(filter).sort(sort))
                    .filter(selector)
                    .skip(perPage * Math.max(0L, pageNumber - 1))
                    .limit(perPage)
                    .toList();
        } else {
            documents = stream(collection.find().filter(filter).sort(sort))
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
