package org.graylog2.shared.tokenusage;

import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.models.tokenusage.TokenUsage;
import org.graylog2.search.SearchQuery;

public interface TokenUsageService {
    PaginatedList<TokenUsage> loadTokenUsage(int page,
                                             int perPage,
                                             SearchQuery searchQuery,
                                             String sort,
                                             SortOrder order);
}
