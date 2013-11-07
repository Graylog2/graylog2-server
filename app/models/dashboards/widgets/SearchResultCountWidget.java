/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package models.dashboards.widgets;

import com.google.common.collect.Maps;
import lib.timeranges.TimeRange;
import models.dashboards.Dashboard;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SearchResultCountWidget extends DashboardWidget {

    private final String query;
    private final TimeRange timerange;

    public SearchResultCountWidget(Dashboard dashboard, String query, TimeRange timerange) {
        this(dashboard, null, query, timerange);
    }

    public SearchResultCountWidget(Dashboard dashboard, String id, String query, TimeRange timerange) {
        super(Type.SEARCH_RESULT_COUNT, id, dashboard);

        this.query = query;
        this.timerange = timerange;
    }

    @Override
    public Map<String, Object> getConfig() {
        Map<String, Object> config = Maps.newHashMap();
        config.putAll(timerange.getQueryParams());
        config.put("query", query);

        return config;
    }

}
