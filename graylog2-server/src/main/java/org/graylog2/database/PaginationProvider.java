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
package org.graylog2.database;

import org.bson.conversions.Bson;

import java.util.function.Predicate;

public interface PaginationProvider<T> {
    /**
     * Sets the query filter to apply to the query.
     *
     * @param filter the filter, which may be null.
     * @return this
     */
    PaginationProvider<T> filter(Bson filter);

    /**
     * Sets the sort criteria to apply to the query.
     *
     * @param sort the sort criteria, which may be null.
     * @return this
     */
    PaginationProvider<T> sort(Bson sort);

    /**
     * Sets the sort criteria to apply to the query.
     *
     * @param fieldName the name of the field to sort by.
     * @param order     "desc" to request descending sort order. Otherwise, by default, ascending order is used.
     * @return this
     */
    PaginationProvider<T> sort(String fieldName, String order);

    /**
     * Sets the page size
     *
     * @param perPage the number of documents to put on one page
     * @return this
     */
    PaginationProvider<T> perPage(int perPage);

    /**
     * Specifies whether to include a grand total number of all documents in the collection. No filters, except, if set,
     * the {@link #grandTotalFilter(Bson)} will be applied to the count query.
     *
     * @param includeGrandTotal true if a grand total should be included. Otherwise, by default, no grand total will
     *                          be included.
     * @return this
     */
    PaginationProvider<T> includeGrandTotal(boolean includeGrandTotal);

    /**
     * Sets a filter to be applied to the query to count the grand total of documents in the collection.
     *
     * @param grandTotalFilter the filter, which may be null
     * @return this
     */
    PaginationProvider<T> grandTotalFilter(Bson grandTotalFilter);

    /**
     * Perform the MongoDB request and return the specified page.
     *
     * @param pageNumber The number of the page to be returned.
     * @return a paginated list of documents
     */
    PaginatedList<T> page(int pageNumber);

    /**
     * Perform the MongoDB request but post process the list of results by only keeping elements matching
     * the given selector.
     * <p>
     * <b>This is a potentially expensive operation because the selector can only be applied after the documents
     * have been fetched from MongoDB and this might result in a full collection scan. Use with care!</b>
     *
     * @param pageNumber The number of the page to be returned.
     * @param selector   predicate to filter documents after fetching them from MongoDB.
     * @return a paginated list of documents
     */
    PaginatedList<T> postProcessedPage(int pageNumber, Predicate<T> selector);
}