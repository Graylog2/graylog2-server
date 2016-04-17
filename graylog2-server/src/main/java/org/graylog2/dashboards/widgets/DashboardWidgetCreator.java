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

package org.graylog2.dashboards.widgets;

import com.mongodb.BasicDBObject;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.rest.models.dashboards.requests.AddWidgetRequest;
import org.graylog2.timeranges.TimeRangeFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Strings.isNullOrEmpty;

public class DashboardWidgetCreator {
    private final WidgetCacheTime.Factory cacheTimeFactory;
    private final TimeRangeFactory timeRangeFactory;

    @Inject
    public DashboardWidgetCreator(WidgetCacheTime.Factory cacheTimeFactory, TimeRangeFactory timeRangeFactory) {
        this.cacheTimeFactory = cacheTimeFactory;
        this.timeRangeFactory = timeRangeFactory;
    }

    public DashboardWidget fromRequest(AddWidgetRequest awr, String userId) throws DashboardWidget.NoSuchWidgetTypeException, InvalidRangeParametersException, InvalidWidgetConfigurationException {
        return fromRequest(null, awr, userId);
    }

    public DashboardWidget fromRequest(String widgetId, AddWidgetRequest awr, String userId) throws DashboardWidget.NoSuchWidgetTypeException, InvalidRangeParametersException, InvalidWidgetConfigurationException {
        final String id = isNullOrEmpty(widgetId) ? UUID.randomUUID().toString() : widgetId;

        // Build timerange.
        final Map<String, Object> timerangeConfig = (Map<String, Object>)awr.config().get("timerange");
        final TimeRange timeRange = timeRangeFactory.create(timerangeConfig);

        return buildDashboardWidget(awr.type(), id, awr.description(), 0, awr.config(),
                timeRange, userId);
    }

    public DashboardWidget fromPersisted(BasicDBObject fields) throws DashboardWidget.NoSuchWidgetTypeException, InvalidRangeParametersException, InvalidWidgetConfigurationException {
        final String type = (String)fields.get(DashboardWidget.FIELD_TYPE);
        final BasicDBObject config = (BasicDBObject) fields.get(DashboardWidget.FIELD_CONFIG);

        final String widgetId = (String) fields.get(DashboardWidget.FIELD_ID);

        // Build timerange.
        final BasicDBObject timerangeConfig = (BasicDBObject) config.get("timerange");
        final TimeRange timeRange = timeRangeFactory.create(timerangeConfig);

        final String description = (String) fields.get(DashboardWidget.FIELD_DESCRIPTION);
        final int cacheTime = (int) firstNonNull(fields.get(DashboardWidget.FIELD_CACHE_TIME), 0);

        return buildDashboardWidget(type, widgetId, description, cacheTime,
                config, timeRange, (String) fields.get(DashboardWidget.FIELD_CREATOR_USER_ID));
    }

    public DashboardWidget buildDashboardWidget(
            final String type,
            final String widgetId,
            final String description,
            final int requestedCacheTime,
            final Map<String, Object> config,
            final TimeRange timeRange,
            final String creatorUserId) throws DashboardWidget.NoSuchWidgetTypeException, InvalidWidgetConfigurationException {

        final WidgetCacheTime cacheTime = cacheTimeFactory.create(requestedCacheTime);
        return new DashboardWidget(type, widgetId, timeRange, description, cacheTime, config, creatorUserId);
    }
}
