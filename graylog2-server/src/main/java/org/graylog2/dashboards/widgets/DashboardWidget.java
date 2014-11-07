/**
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
 */
package org.graylog2.dashboards.widgets;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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
import org.graylog2.rest.resources.dashboards.requests.AddWidgetRequest;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public abstract class DashboardWidget implements EmbeddedPersistable {

    private final MetricRegistry metricRegistry;

    public enum Type {
        SEARCH_RESULT_COUNT,
        STREAM_SEARCH_RESULT_COUNT,
        FIELD_CHART,
        QUICKVALUES,
        SEARCH_RESULT_CHART
    }

    private static final String RESULT_CACHE_KEY = "result";

    public static final int DEFAULT_CACHE_TIME = 10;

    private final Type type;
    private final String id;
    private final Map<String, Object> config;
    private final String creatorUserId;
    private int cacheTime;
    private String description;

    private Cache<String, ComputationResult> cache;

    protected DashboardWidget(MetricRegistry metricRegistry, Type type, String id, String description, int cacheTimeS, Map<String, Object> config, String creatorUserId) {
        this.metricRegistry = metricRegistry;
        this.type = type;
        this.id = id;
        this.config = config;
        this.creatorUserId = creatorUserId;
        this.description = description;

        if (cacheTimeS < 1) {
            this.cacheTime = DEFAULT_CACHE_TIME;
        } else {
            this.cacheTime = cacheTimeS;
        }

        this.cache = buildCache(this.cacheTime);
    }

    private Cache<String, ComputationResult> buildCache(int cacheTime) {
        return CacheBuilder.newBuilder()
                .maximumSize(1)
                .expireAfterWrite(cacheTime, TimeUnit.SECONDS)
                .build();
    }

    public static DashboardWidget fromRequest(MetricRegistry metricRegistry, Searches searches, AddWidgetRequest awr, String userId) throws NoSuchWidgetTypeException, InvalidRangeParametersException, InvalidWidgetConfigurationException {
        Type type;
        try {
            type = Type.valueOf(awr.type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new NoSuchWidgetTypeException("No such widget type <" + awr.type + ">");
        }

        String id = UUID.randomUUID().toString();

        // Build timerange.
        TimeRange timeRange;

        if (!awr.config.containsKey("range_type")) {
            throw new InvalidRangeParametersException("range_type not set");
        }

        String rangeType = (String) awr.config.get("range_type");
        if (rangeType.equals("relative")) {
            timeRange = new RelativeRange(Integer.parseInt((String) awr.config.get("range")));
        } else if (rangeType.equals("keyword")) {
            timeRange = new KeywordRange((String) awr.config.get("keyword"));
        } else if (rangeType.equals("absolute")) {
            timeRange = new AbsoluteRange((String) awr.config.get("from"), (String) awr.config.get("to"));
        } else {
            throw new InvalidRangeParametersException("range_type not recognized");
        }

        return buildDashboardWidget(type, metricRegistry, searches, id, awr.description, 0, awr.config,
                (String) awr.config.get("query"), timeRange, userId);
    }

    public static DashboardWidget fromPersisted(MetricRegistry metricRegistry, Searches searches, BasicDBObject fields) throws NoSuchWidgetTypeException, InvalidRangeParametersException, InvalidWidgetConfigurationException {
        Type type;
        try {
            type = Type.valueOf(((String) fields.get("type")).toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new NoSuchWidgetTypeException();
        }

        BasicDBObject config = (BasicDBObject) fields.get("config");

        // Build timerange.
        BasicDBObject timerangeConfig = (BasicDBObject) config.get("timerange");
        TimeRange timeRange;

        if (!timerangeConfig.containsField("type")) {
            throw new InvalidRangeParametersException("range type not set");
        }

        String rangeType = (String) timerangeConfig.get("type");

        if (rangeType.equals("relative")) {
            timeRange = new RelativeRange((Integer) timerangeConfig.get("range"));
        } else if (rangeType.equals("keyword")) {
            timeRange = new KeywordRange((String) timerangeConfig.get("keyword"));
        } else if (rangeType.equals("absolute")) {

            String from = new DateTime(timerangeConfig.get("from"), DateTimeZone.UTC).toString(Tools.ES_DATE_FORMAT);
            String to = new DateTime(timerangeConfig.get("to"), DateTimeZone.UTC).toString(Tools.ES_DATE_FORMAT);

            timeRange = new AbsoluteRange(from, to);
        } else {
            throw new InvalidRangeParametersException("range_type not recognized");
        }

        // Is a description set?
        String description = null;
        if (fields.containsField("description")) {
            description = (String) fields.get("description");
        }

        // Do we have a configured cache time?
        int cacheTime = 0;
        if (fields.containsField("cache_time")) {
            cacheTime = (Integer) fields.get("cache_time");
        }

        return buildDashboardWidget(type, metricRegistry, searches, (String) fields.get("id"), description, cacheTime,
                config, (String) config.get("query"), timeRange, (String) fields.get("creator_user_id"));
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
        this.cache = buildCache(cacheTime);
        this.cacheTime = cacheTime;
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
                .put("id", id)
                .put("type", type.toString().toLowerCase())
                .put("description", description)
                .put("cache_time", cacheTime)
                .put("creator_user_id", creatorUserId)
                .put("config", getPersistedConfig())
                .build();
    }

    public ComputationResult getComputationResult() throws ExecutionException {
        return cache.get(RESULT_CACHE_KEY, new Callable<ComputationResult>() {
            @Override
            public ComputationResult call() throws Exception {
                ComputationResult result;

                Timer.Context timer = getCalculationTimer().time();
                try {
                    result = compute();
                } finally {
                    timer.stop();
                }

                getCalculationMeter().mark();

                return result;
            }
        });
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
}