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
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import org.graylog2.indexer.searches.timeranges.TimeRange;
import org.graylog2.plugin.database.EmbeddedPersistable;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public abstract class DashboardWidget implements EmbeddedPersistable {
    public static final String FIELD_ID = "id";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_CACHE_TIME = "cache_time";
    public static final String FIELD_CREATOR_USER_ID = "creator_user_id";
    public static final String FIELD_CONFIG = "config";

    public enum Type {
        SEARCH_RESULT_COUNT,
        STREAM_SEARCH_RESULT_COUNT,
        FIELD_CHART,
        QUICKVALUES,
        SEARCH_RESULT_CHART,
        STATS_COUNT,
        STACKED_CHART
    }

    private final MetricRegistry metricRegistry;
    private final Type type;
    private final String id;
    private final TimeRange timeRange;
    private final Map<String, Object> config;
    private final String creatorUserId;
    private int cacheTime;
    private String description;
    private Supplier<ComputationResult> cachedResult;

    protected DashboardWidget(MetricRegistry metricRegistry, Type type, String id, TimeRange timeRange, String description, WidgetCacheTime cacheTime, Map<String, Object> config, String creatorUserId) {
        this.metricRegistry = metricRegistry;
        this.type = type;
        this.id = id;
        this.timeRange = timeRange;
        this.config = config;
        this.creatorUserId = creatorUserId;
        this.description = description;
        this.cacheTime = cacheTime.getCacheTime();
        this.cachedResult = Suppliers.memoizeWithExpiration(new ComputationResultSupplier(), this.cacheTime, TimeUnit.SECONDS);
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

    public TimeRange getTimeRange() {
        Preconditions.checkArgument(this.timeRange != null, "Invalid time range provided");
        return timeRange;
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
                .put(FIELD_TYPE, type.toString().toLowerCase(Locale.ENGLISH))
                .put(FIELD_DESCRIPTION, description)
                .put(FIELD_CACHE_TIME, cacheTime)
                .put(FIELD_CREATOR_USER_ID, creatorUserId)
                .put(FIELD_CONFIG, getPersistedConfig())
                .build();
    }

    public ComputationResult getComputationResult() throws ExecutionException {
        return cachedResult.get();
    }

    public Map<String, Object> getPersistedConfig() {
        return ImmutableMap.<String, Object>of("timerange", this.getTimeRange().getPersistedConfig());
    }

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