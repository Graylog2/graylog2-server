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

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Collation;
import org.bson.conversions.Bson;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoEntity;
import org.graylog2.database.PaginatedList;

import java.util.List;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkArgument;
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
    private final Bson projection;
    private final int perPage;
    private final boolean includeGrandTotal;
    private final Bson grandTotalFilter;
    private final Collation collation;
    private final List<Bson> pipeline;
    private final boolean includeSourceMetadata;

    public DefaultMongoPaginationHelper(MongoCollection<T> collection) {
        this(collection, null, null, null, 0, false, null, null, List.of(), false);
    }

    private DefaultMongoPaginationHelper(MongoCollection<T> collection,
                                         Bson filter,
                                         Bson sort,
                                         Bson projection,
                                         int perPage,
                                         boolean includeGrandTotal,
                                         Bson grandTotalFilter,
                                         Collation collation,
                                         List<Bson> pipeline,
                                         boolean includeSourceMetadata) {
        this.collection = collection;
        this.filter = filter;
        this.sort = sort;
        this.projection = projection;
        this.perPage = perPage;
        this.includeGrandTotal = includeGrandTotal;
        this.grandTotalFilter = grandTotalFilter;
        this.collation = collation;
        this.pipeline = pipeline;
        this.includeSourceMetadata = includeSourceMetadata;
    }

    @Override
    public MongoPaginationHelper<T> filter(Bson filter) {
        return new DefaultMongoPaginationHelper<>(collection, filter, sort, projection, perPage, includeGrandTotal,
                grandTotalFilter, collation, pipeline, includeSourceMetadata);
    }

    @Override
    public MongoPaginationHelper<T> sort(Bson sort) {
        return new DefaultMongoPaginationHelper<>(collection, filter, sort, projection, perPage, includeGrandTotal,
                grandTotalFilter, collation, pipeline, includeSourceMetadata);
    }

    @Override
    public MongoPaginationHelper<T> projection(Bson projection) {
        return new DefaultMongoPaginationHelper<>(collection, filter, sort, projection, perPage, includeGrandTotal,
                grandTotalFilter, collation, pipeline, includeSourceMetadata);
    }

    @Override
    public MongoPaginationHelper<T> perPage(int perPage) {
        return new DefaultMongoPaginationHelper<>(collection, filter, sort, projection, perPage, includeGrandTotal,
                grandTotalFilter, collation, pipeline, includeSourceMetadata);
    }

    @Override
    public MongoPaginationHelper<T> includeGrandTotal(boolean includeGrandTotal) {
        return new DefaultMongoPaginationHelper<>(collection, filter, sort, projection, perPage, includeGrandTotal,
                grandTotalFilter, collation, pipeline, includeSourceMetadata);
    }

    @Override
    public MongoPaginationHelper<T> grandTotalFilter(Bson grandTotalFilter) {
        return new DefaultMongoPaginationHelper<>(collection, filter, sort, projection, perPage, includeGrandTotal,
                grandTotalFilter, collation, pipeline, includeSourceMetadata);
    }

    @Override
    public MongoPaginationHelper<T> collation(Collation collation) {
        return new DefaultMongoPaginationHelper<>(collection, filter, sort, projection, perPage, includeGrandTotal,
                grandTotalFilter, collation, pipeline, includeSourceMetadata);
    }

    @Override
    public MongoPaginationHelper<T> pipeline(List<Bson> pipeline) {
        checkArgument(pipeline != null && !pipeline.isEmpty(), "Pipeline must be non-null and not empty.");
        return new DefaultMongoPaginationHelper<>(collection, filter, sort, projection, perPage, includeGrandTotal,
                grandTotalFilter, collation, pipeline, includeSourceMetadata);
    }

    @Override
    public MongoPaginationHelper<T> includeSourceMetadata(boolean includeSourceMetadata) {
        return new DefaultMongoPaginationHelper<>(collection, filter, sort, projection, perPage, includeGrandTotal,
                grandTotalFilter, collation, pipeline, includeSourceMetadata);
    }

    @Override
    public PaginatedList<T> page(int pageNumber) {
        final List<T> documents;
        try (final var stream = stream(getFindIterableBase(pageNumber, perPage))) {
            documents = stream.toList();
        }
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
        final int total;
        try (final var stream = stream(getIterableForAllDocuments())) {
            total = Ints.saturatedCast(stream.filter(selector).count());
        }

        final List<T> documents;
        try (final var stream = stream(getIterableForAllDocuments())) {
            documents = stream
                    .filter(selector)
                    .skip(perPage > 0 ? perPage * Math.max(0L, pageNumber - 1) : 0)
                    .limit(perPage == 0 ? Integer.MAX_VALUE : perPage)
                    .toList();
        }

        if (includeGrandTotal) {
            final long grandTotal = collection.countDocuments(grandTotalFilter);
            return new PaginatedList<>(documents, total, pageNumber, perPage, grandTotal);
        } else {
            return new PaginatedList<>(documents, total, pageNumber, perPage);
        }
    }

    private MongoIterable<T> getIterableForAllDocuments() {
        return getFindIterableBase(1, 0);
    }

    private MongoIterable<T> getFindIterableBase(int pageNumber, int pageSize) {
        final var skip = pageSize * Math.max(0, pageNumber - 1);

        if (pipeline.isEmpty() && !includeSourceMetadata) {
            FindIterable<T> findIterable = collection.find()
                    .filter(filter)
                    .sort(sort)
                    .collation(collation);
            if (projection != null) {
                findIterable = findIterable.projection(projection);
            }
            if (skip > 0) {
                findIterable = findIterable.skip(skip);
            }
            if (pageSize > 0) {
                findIterable = findIterable.limit(pageSize);
            }
            return findIterable;
        }
        final var finalPipeline = ImmutableList.<Bson>builder()
                .addAll(pipeline);
        if (filter != null) {
            finalPipeline.add(Aggregates.match(filter));
        }
        if (includeSourceMetadata) {
            finalPipeline.addAll(EntitySourceLookup.ENTITY_SOURCE_LOOKUP);
        }
        if (sort != null) {
            finalPipeline.add(Aggregates.sort(sort));
        }
        if (projection != null) {
            finalPipeline.add(Aggregates.project(projection));
        }
        if (skip > 0) {
            finalPipeline.add(Aggregates.skip(skip));
        }
        if (pageSize > 0) {
            finalPipeline.add(Aggregates.limit(pageSize));
        }
        return collection.aggregate(finalPipeline.build()).collation(collation);
    }

}
