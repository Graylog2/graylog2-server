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
import models.dashboards.Dashboard;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SearchResultFieldValueChartWidget extends DashboardWidget {

    private static final int WIDTH = 2;
    private static final int HEIGHT = 2;

    protected SearchResultFieldValueChartWidget(String id, String description, int cacheTime, Dashboard dashboard) {
        super(Type.SEARCH_RESULT_FIELD_VALUE, id, description, cacheTime, dashboard);
    }

    @Override
    public Map<String, Object> getConfig() {
        return Maps.newHashMap();
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

}
