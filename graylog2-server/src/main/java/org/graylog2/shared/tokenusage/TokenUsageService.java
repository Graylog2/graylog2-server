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
package org.graylog2.shared.tokenusage;

import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.models.tokenusage.TokenUsageDTO;
import org.graylog2.search.SearchQuery;

public interface TokenUsageService {

    /**
     * Loads entries of {@link TokenUsageDTO} for the given parameters.
     * <p>
     * The idea is that all tokens are listed, enriched with information about its owner. This provides an overview of
     * possibly expired tokens or those whose owner doesn't exist on the system anymore.
     *
     * @param page        The page to load, starting at 1.
     * @param perPage     Number of items per page.
     * @param searchQuery The search to perform - only items matching this query are returned.
     * @param sort        Sort by this given field.
     * @param order       The order of sorting ({@code asc} or {@code desc}.
     * @return A page of matching {@link TokenUsageDTO}, sorted by the specified field and order.
     */
    PaginatedList<TokenUsageDTO> loadTokenUsage(int page,
                                                int perPage,
                                                SearchQuery searchQuery,
                                                String sort,
                                                SortOrder order);
}
