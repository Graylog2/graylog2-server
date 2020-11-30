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
        final PaginatedList<ViewDTO> result = viewService.searchPaginatedByType(ViewDTO.Type.DASHBOARD, new SearchQuery(""), dashboard -> true, "ASC", ViewDTO.FIELD_ID, 1, 0);
        return result.grandTotal().orElseThrow(() -> new IllegalStateException("Missing grand total in response when counting dashboards!"));
    }
}
