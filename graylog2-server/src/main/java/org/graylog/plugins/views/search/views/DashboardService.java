package org.graylog.plugins.views.search.views;

import org.graylog2.database.PaginatedList;
import org.graylog2.search.SearchQuery;

import javax.inject.Inject;

public class DashboardService {
    private final ViewService viewService;

    @Inject
    public DashboardService(ViewService viewService) {
        this.viewService = viewService;
    }

    public long count() {
        final PaginatedList<ViewDTO> result = viewService.searchPaginatedByType(ViewDTO.Type.DASHBOARD, new SearchQuery(""), dashboard -> true, "ASC", null, 1, 0);
        return result.grandTotal().orElseThrow(() -> new IllegalStateException("Missing grand total in response when counting dashboards!"));
    }
}
