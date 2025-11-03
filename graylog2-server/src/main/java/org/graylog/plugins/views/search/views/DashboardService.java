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
package org.graylog.plugins.views.search.views;

import com.mongodb.client.model.Filters;
import jakarta.inject.Inject;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.search.SearchQuery;

import java.util.Map;

public class DashboardService {
    private final ViewService viewService;

    @Inject
    public DashboardService(ViewService viewService) {
        this.viewService = viewService;
    }

    public long count() {
        final PaginatedList<ViewDTO> result = viewService.searchPaginatedByType(ViewDTO.Type.DASHBOARD, new SearchQuery(""), dashboard -> true, SortOrder.ASCENDING, ViewDTO.FIELD_ID, 1, 0);
        return result.grandTotal().orElseThrow(() -> new IllegalStateException("Missing grand total in response when counting dashboards!"));
    }

    /**
     * @return a map with counts of dashboards grouped by source (Illuminate vs user-created).
     */
    public Map<String, Long> countBySource() {
        long illuminateDashboardCount = viewService.collection().countDocuments(
                Filters.and(
                        Filters.regex(ViewDTO.FIELD_TITLE, "^Illuminate:"),
                        Filters.eq(ViewDTO.FIELD_TYPE, ViewDTO.Type.DASHBOARD)
                )
        );

        long userDashboardCount = viewService.collection().countDocuments(
                Filters.eq(ViewDTO.FIELD_TYPE, ViewDTO.Type.DASHBOARD)
        ) - illuminateDashboardCount;

        return Map.of(
                "illuminate_dashboards", illuminateDashboardCount,
                "user_dashboards", userDashboardCount
        );
    }
}
