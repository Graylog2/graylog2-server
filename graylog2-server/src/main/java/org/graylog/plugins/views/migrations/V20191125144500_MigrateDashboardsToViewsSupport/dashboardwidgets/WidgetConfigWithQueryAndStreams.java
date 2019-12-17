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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.AggregationWidget;

import java.util.Collections;
import java.util.Optional;

public interface WidgetConfigWithQueryAndStreams extends WidgetConfigWithTimeRange {
    String query();

    Optional<String> streamId();

    default AggregationWidget.Builder createAggregationWidget(String id) {
        final AggregationWidget.Builder viewWidgetBuilder = AggregationWidget.builder()
                .id(id)
                .query(query())
                .timerange(timerange());
        return streamId().map(streamId -> viewWidgetBuilder.streams(Collections.singleton(streamId))).orElse(viewWidgetBuilder);
    }
}
