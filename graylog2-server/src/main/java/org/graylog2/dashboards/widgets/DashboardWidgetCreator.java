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

import com.codahale.metrics.MetricRegistry;
import com.mongodb.BasicDBObject;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.indexer.searches.timeranges.KeywordRange;
import org.graylog2.indexer.searches.timeranges.RelativeRange;
import org.graylog2.indexer.searches.timeranges.TimeRange;
import org.graylog2.plugin.Tools;
import org.graylog2.rest.models.dashboards.requests.AddWidgetRequest;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Strings.isNullOrEmpty;

public class DashboardWidgetCreator {

    private static final Logger LOG = LoggerFactory.getLogger(DashboardWidgetCreator.class);

    private final MetricRegistry metricRegistry;
    private final WidgetCacheTime.Factory cacheTimeFactory;

    @Inject
    public DashboardWidgetCreator(MetricRegistry metricRegistry, WidgetCacheTime.Factory cacheTimeFactory) {
        this.metricRegistry = metricRegistry;
        this.cacheTimeFactory = cacheTimeFactory;
    }

    public DashboardWidget fromRequest(Searches searches, AddWidgetRequest awr, String userId) throws DashboardWidget.NoSuchWidgetTypeException, InvalidRangeParametersException, InvalidWidgetConfigurationException {
        return fromRequest(searches, null, awr, userId);
    }

    public DashboardWidget fromRequest(Searches searches, String widgetId, AddWidgetRequest awr, String userId) throws DashboardWidget.NoSuchWidgetTypeException, InvalidRangeParametersException, InvalidWidgetConfigurationException {
        DashboardWidget.Type type;
        try {
            type = DashboardWidget.Type.valueOf(awr.type().toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            throw new DashboardWidget.NoSuchWidgetTypeException("No such widget type <" + awr.type() + ">");
        }

        String id = isNullOrEmpty(widgetId) ? UUID.randomUUID().toString() : widgetId;

        // Build timerange.
        final String rangeType = (String) awr.config().get("range_type");
        if (rangeType == null) {
            throw new InvalidRangeParametersException("range_type not set");
        }

        final TimeRange timeRange;
        switch (rangeType) {
            case "relative":
                timeRange = new RelativeRange(Integer.parseInt(String.valueOf(awr.config().get("range"))));
                break;
            case "keyword":
                timeRange = new KeywordRange((String) awr.config().get("keyword"));
                break;
            case "absolute":
                timeRange = new AbsoluteRange((String) awr.config().get("from"), (String) awr.config().get("to"));
                break;
            default:
                throw new InvalidRangeParametersException("range_type not recognized");
        }

        return buildDashboardWidget(type, searches, id, awr.description(), 0, awr.config(),
                (String) awr.config().get("query"), timeRange, userId);
    }

    public DashboardWidget fromPersisted(Searches searches, BasicDBObject fields) throws DashboardWidget.NoSuchWidgetTypeException, InvalidRangeParametersException, InvalidWidgetConfigurationException {
        DashboardWidget.Type type;
        try {
            type = DashboardWidget.Type.valueOf(((String) fields.get(DashboardWidget.FIELD_TYPE)).toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            throw new DashboardWidget.NoSuchWidgetTypeException();
        }

        BasicDBObject config = (BasicDBObject) fields.get(DashboardWidget.FIELD_CONFIG);

        // Build timerange.
        BasicDBObject timerangeConfig = (BasicDBObject) config.get("timerange");

        final String rangeType = (String) timerangeConfig.get(DashboardWidget.FIELD_TYPE);
        if (rangeType == null) {
            throw new InvalidRangeParametersException("range type not set");
        }

        final String widgetId = (String) fields.get(DashboardWidget.FIELD_ID);

        TimeRange timeRange;
        try {
            switch (rangeType) {
                case "relative":
                    timeRange = new RelativeRange((Integer) timerangeConfig.get("range"));
                    break;
                case "keyword":
                    timeRange = new KeywordRange((String) timerangeConfig.get("keyword"));
                    break;
                case "absolute":
                    String from = new DateTime(timerangeConfig.get("from"), DateTimeZone.UTC).toString(Tools.ES_DATE_FORMAT);
                    String to = new DateTime(timerangeConfig.get("to"), DateTimeZone.UTC).toString(Tools.ES_DATE_FORMAT);

                    timeRange = new AbsoluteRange(from, to);
                    break;
                default:
                    throw new InvalidRangeParametersException("range_type not recognized");
            }
        } catch (InvalidRangeParametersException e) {
            LOG.error("Invalid time range provided on widget " + widgetId, e);
            timeRange = null;
        }

        final String description = (String) fields.get(DashboardWidget.FIELD_DESCRIPTION);
        final int cacheTime = (int) firstNonNull(fields.get(DashboardWidget.FIELD_CACHE_TIME), 0);

        return buildDashboardWidget(type, searches, widgetId, description, cacheTime,
                config, (String) config.get("query"), timeRange, (String) fields.get(DashboardWidget.FIELD_CREATOR_USER_ID));
    }

    public DashboardWidget buildDashboardWidget(
            final DashboardWidget.Type type,
            final Searches searches,
            final String widgetId,
            final String description,
            final int requestedCacheTime,
            final Map<String, Object> config,
            final String query,
            final TimeRange timeRange,
            final String creatorUserId) throws DashboardWidget.NoSuchWidgetTypeException, InvalidWidgetConfigurationException {

        final WidgetCacheTime cacheTime = cacheTimeFactory.create(requestedCacheTime);

        switch (type) {
            case SEARCH_RESULT_COUNT:
                return new SearchResultCountWidget(metricRegistry, searches,
                        widgetId,
                        description,
                        cacheTime,
                        config,
                        query,
                        timeRange,
                        creatorUserId);
            case STREAM_SEARCH_RESULT_COUNT:
                return new StreamSearchResultCountWidget(metricRegistry, searches,
                        widgetId,
                        description,
                        cacheTime,
                        config,
                        query,
                        timeRange,
                        creatorUserId);
            case FIELD_CHART:
                return new FieldChartWidget(metricRegistry, searches,
                        widgetId,
                        description,
                        cacheTime,
                        config,
                        query,
                        timeRange,
                        creatorUserId);
            case QUICKVALUES:
                return new QuickvaluesWidget(metricRegistry, searches,
                        widgetId,
                        description,
                        cacheTime,
                        config,
                        query,
                        timeRange,
                        creatorUserId);
            case SEARCH_RESULT_CHART:
                return new SearchResultChartWidget(metricRegistry, searches,
                        widgetId,
                        description,
                        cacheTime,
                        config,
                        query,
                        timeRange,
                        creatorUserId);
            case STATS_COUNT:
                return new StatisticalCountWidget(metricRegistry, searches,
                        widgetId,
                        description,
                        cacheTime,
                        config,
                        query,
                        timeRange,
                        creatorUserId);
            case STACKED_CHART:
                return new StackedChartWidget(metricRegistry, searches,
                        widgetId,
                        description,
                        cacheTime,
                        config,
                        timeRange,
                        creatorUserId);
            default:
                throw new DashboardWidget.NoSuchWidgetTypeException();
        }
    }
}
