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
import com.google.common.collect.ImmutableMap;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.Searches.DateHistogramInterval;
import org.graylog2.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

public abstract class ChartWidget extends DashboardWidget {

    @Nullable
    protected final String streamId;
    protected final DateHistogramInterval interval;

    protected ChartWidget(MetricRegistry metricRegistry, Type type, String id, TimeRange timeRange, String description, WidgetCacheTime cacheTime, Map<String, Object> config, String creatorUserId) {
        super(metricRegistry, type, id, timeRange, description, cacheTime, config, creatorUserId);

        this.streamId = (String) config.get("stream_id");

        if (config.containsKey("interval")) {
            this.interval = Searches.DateHistogramInterval.valueOf(((String) config.get("interval")).toUpperCase(Locale.ENGLISH));
        } else {
            this.interval = Searches.DateHistogramInterval.MINUTE;
        }
    }

    @Override
    public Map<String, Object> getPersistedConfig() {
        final ImmutableMap.Builder<String, Object> persistedConfig = ImmutableMap.<String, Object>builder()
                .putAll(super.getPersistedConfig())
                .put("interval", interval.toString().toLowerCase(Locale.ENGLISH));

        if (!isNullOrEmpty(streamId)) {
            persistedConfig.put("stream_id", streamId);
        }

        return persistedConfig.build();
    }
}
