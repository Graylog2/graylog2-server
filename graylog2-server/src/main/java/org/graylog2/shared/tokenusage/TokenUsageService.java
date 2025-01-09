package org.graylog2.shared.tokenusage;

import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.models.tokenusage.TokenUsage;
import org.graylog2.search.SearchQuery;

public interface TokenUsageService {

    /**
     * Loads entries of {@link TokenUsage} for the given parameters.
     * <p>
     * The idea is that all tokens are listed, enriched with information about its owner. This provides an overview of
     * possibly expired tokens or those whose owner doesn't exist on the system anymore.
     *
     * @param page        The page to load, starting at 1.
     * @param perPage     Number of items per page.
     * @param searchQuery The search to perform - only items matching this query are returned.
     * @param sort        Sort by this given field.
     * @param order       The order of sorting ({@code asc} or {@code desc}.
     * @return A page of matching {@link TokenUsage}, sorted by the specified field and order.
     */
    PaginatedList<TokenUsage> loadTokenUsage(int page,
                                             int perPage,
                                             SearchQuery searchQuery,
                                             String sort,
                                             SortOrder order);
}
