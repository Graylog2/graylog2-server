/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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
        final PaginatedList<ViewDTO> result = viewService.searchPaginatedByType(ViewDTO.Type.DASHBOARD, new SearchQuery(""), dashboard -> true, "ASC", null, 1, 0);
        return result.grandTotal().orElseThrow(() -> new IllegalStateException("Missing grand total in response when counting dashboards!"));
    }
}
