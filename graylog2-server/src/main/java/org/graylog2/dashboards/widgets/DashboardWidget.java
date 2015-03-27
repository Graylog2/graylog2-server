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

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.mongodb.BasicDBObject;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.indexer.searches.timeranges.KeywordRange;
import org.graylog2.indexer.searches.timeranges.RelativeRange;
import org.graylog2.indexer.searches.timeranges.TimeRange;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.database.EmbeddedPersistable;
import org.graylog2.rest.models.dashboards.requests.AddWidgetRequest;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.MoreObjects.firstNonNull;

public abstract class DashboardWidget implements EmbeddedPersistable {
    private static final String FIELD_ID = "id";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_CACHE_TIME = "cache_time";
    private static final String FIELD_CREATOR_USER_ID = "creator_user_id";
    private static final String FIELD_CONFIG = "config";

    public enum Type {
        SEARCH_RESULT_COUNT,
        STREAM_SEARCH_RESULT_COUNT,
        FIELD_CHART,
        QUICKVALUES,
        SEARCH_RESULT_CHART,
        STATS_COUNT
    }

    public static final int DEFAULT_CACHE_TIME = 10;

    private final MetricRegistry metricRegistry;
    private final Type type;
    private final String id;
    private final Map<String, Object> config;
    private final String creatorUserId;
    private int cacheTime;
    private String description;
    private Supplier<ComputationResult> cachedResult;

    protected DashboardWidget(MetricRegistry metricRegistry, Type type, String id, String description, int cacheTimeS, Map<String, Object> config, String creatorUserId) {
        this.metricRegistry = metricRegistry;
        this.type = type;
        this.id = id;
        this.config = config;
        this.creatorUserId = creatorUserId;
        this.description = description;
        this.cacheTime = cacheTimeS < 1 ? DEFAULT_CACHE_TIME : cacheTimeS;
        this.cachedResult = Suppliers.memoizeWithExpiration(new ComputationResultSupplier(), this.cacheTime, TimeUnit.SECONDS);
    }

    public static DashboardWidget fromRequest(MetricRegistry metricRegistry, Searches searches, AddWidgetRequest awr, String userId) throws NoSuchWidgetTypeException, InvalidRangeParametersException, InvalidWidgetConfigurationException {
        Type type;
        try {
            type = Type.valueOf(awr.type().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new NoSuchWidgetTypeException("No such widget type <" + awr.type() + ">");
        }

        String id = UUID.randomUUID().toString();

        // Build timerange.
        final String rangeType = (String) awr.config().get("range_type");
        if (rangeType == null) {
            throw new InvalidRangeParametersException("range_type not set");
        }

        final TimeRange timeRange;
        switch (rangeType) {
            case "relative":
                timeRange = new RelativeRange(Integer.parseInt((String) awr.config().get("range")));
                break;
            case "keyword":
                timeRange = new KeywordRange((String) awr.config().get("keyword"), true);
                break;
            case "absolute":
                timeRange = new AbsoluteRange((String) awr.config().get("from"), (String) awr.config().get("to"));
                break;
            default:
                throw new InvalidRangeParametersException("range_type not recognized");
        }

        return buildDashboardWidget(type, metricRegistry, searches, id, awr.description(), 0, awr.config(),
                (String) awr.config().get("query"), timeRange, userId);
    }

    public static DashboardWidget fromPersisted(MetricRegistry metricRegistry, Searches searches, BasicDBObject fields) throws NoSuchWidgetTypeException, InvalidRangeParametersException, InvalidWidgetConfigurationException {
        Type type;
        try {
            type = Type.valueOf(((String) fields.get(FIELD_TYPE)).toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new NoSuchWidgetTypeException();
        }

        BasicDBObject config = (BasicDBObject) fields.get(FIELD_CONFIG);

        // Build timerange.
        BasicDBObject timerangeConfig = (BasicDBObject) config.get("timerange");

        final String rangeType = (String) timerangeConfig.get(FIELD_TYPE);
        if (rangeType == null) {
            throw new InvalidRangeParametersException("range type not set");
        }

        TimeRange timeRange;
        switch (rangeType) {
            case "relative":
                timeRange = new RelativeRange((Integer) timerangeConfig.get("range"));
                break;
            case "keyword":
                timeRange = new KeywordRange((String) timerangeConfig.get("keyword"), true);
                break;
            case "absolute":
                String from = new DateTime(timerangeConfig.get("from"), DateTimeZone.UTC).toString(Tools.ES_DATE_FORMAT);
                String to = new DateTime(timerangeConfig.get("to"), DateTimeZone.UTC).toString(Tools.ES_DATE_FORMAT);

                timeRange = new AbsoluteRange(from, to);
                break;
            default:
                throw new InvalidRangeParametersException("range_type not recognized");
        }

        final String description = (String) fields.get(FIELD_DESCRIPTION);
        final int cacheTime = (int) firstNonNull(fields.get(FIELD_CACHE_TIME), 0);

        return buildDashboardWidget(type, metricRegistry, searches, (String) fields.get(FIELD_ID), description, cacheTime,
                config, (String) config.get("query"), timeRange, (String) fields.get(FIELD_CREATOR_USER_ID));
    }

    public static DashboardWidget buildDashboardWidget(
            final Type type,
            final MetricRegistry metricRegistry,
            final Searches searches,
            final String widgetId,
            final String description,
            final int cacheTime,
            final Map<String, Object> config,
            final String query,
            final TimeRange timeRange,
            final String creatorUserId) throws NoSuchWidgetTypeException, InvalidWidgetConfigurationException {
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
            default:
                throw new NoSuchWidgetTypeException();
        }
    }

    public Type getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCacheTime(int cacheTime) {
        this.cacheTime = cacheTime;
        this.cachedResult = Suppliers.memoizeWithExpiration(new ComputationResultSupplier(), this.cacheTime, TimeUnit.SECONDS);
    }

    public int getCacheTime() {
        return cacheTime;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public String getCreatorUserId() {
        return creatorUserId;
    }

    public Map<String, Object> getPersistedFields() {
        return ImmutableMap.<String, Object>builder()
                .put(FIELD_ID, id)
                .put(FIELD_TYPE, type.toString().toLowerCase())
                .put(FIELD_DESCRIPTION, description)
                .put(FIELD_CACHE_TIME, cacheTime)
                .put(FIELD_CREATOR_USER_ID, creatorUserId)
                .put(FIELD_CONFIG, getPersistedConfig())
                .build();
    }

    public ComputationResult getComputationResult() throws ExecutionException {
        return cachedResult.get();
    }

    public abstract Map<String, Object> getPersistedConfig();

    protected abstract ComputationResult compute();


    private Timer getCalculationTimer() {
        return metricRegistry.timer(MetricRegistry.name(this.getClass(), getId(), "calculationTime"));
    }

    private Meter getCalculationMeter() {
        return metricRegistry.meter(MetricRegistry.name(this.getClass(), getId(), "calculations"));
    }

    public static class NoSuchWidgetTypeException extends Exception {
        public NoSuchWidgetTypeException() {
            super();
        }

        public NoSuchWidgetTypeException(String msg) {
            super(msg);
        }
    }

    private class ComputationResultSupplier implements Supplier<ComputationResult> {
        @Override
        public ComputationResult get() {
            try (Timer.Context timer = getCalculationTimer().time()) {
                return compute();
            } finally {
                getCalculationMeter().mark();
            }
        }
    }
}