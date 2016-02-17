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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.plugin.database.EmbeddedPersistable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DashboardWidget implements EmbeddedPersistable {
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

    private final String type;
    private final String id;
    private final TimeRange timeRange;
    private final Map<String, Object> config;
    private final String creatorUserId;
    private int cacheTime;
    private String description;

    protected DashboardWidget(String type, String id, TimeRange timeRange, String description, WidgetCacheTime cacheTime, Map<String, Object> config, String creatorUserId) {
        this.type = type;
        this.id = id;
        this.timeRange = timeRange;
        this.config = config;
        this.creatorUserId = creatorUserId;
        this.description = description;
        this.cacheTime = cacheTime.getCacheTime();
    }

    public String getType() {
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

    public Map<String, Object> getPersistedConfig() {
        final Map<String, Object> config = new HashMap<>(this.getConfig());
        config.put("timerange", this.getTimeRange().getPersistedConfig());
        return config;
    }

    public static class NoSuchWidgetTypeException extends Exception {
        public NoSuchWidgetTypeException() {
            super();
        }

        public NoSuchWidgetTypeException(String msg) {
            super(msg);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DashboardWidget that = (DashboardWidget) o;

        if (cacheTime != that.cacheTime) return false;
        if (type != that.type) return false;
        if (!id.equals(that.id)) return false;
        if (!timeRange.equals(that.timeRange)) return false;
        if (!config.equals(that.config)) return false;
        if (!creatorUserId.equals(that.creatorUserId)) return false;
        return description.equals(that.description);

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}