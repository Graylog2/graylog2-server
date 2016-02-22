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

package org.graylog2.dashboards.widgets.strategies;

import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.Searches.DateHistogramInterval;
import org.graylog2.plugin.dashboards.widgets.WidgetStrategy;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;

public abstract class ChartWidgetStrategy implements WidgetStrategy {

    @Nullable
    protected final String streamId;
    protected final DateHistogramInterval interval;

    protected ChartWidgetStrategy(Map<String, Object> config) {
        this.streamId = (String) config.get("stream_id");

        if (config.containsKey("interval")) {
            this.interval = Searches.DateHistogramInterval.valueOf(((String) config.get("interval")).toUpperCase(Locale.ENGLISH));
        } else {
            this.interval = Searches.DateHistogramInterval.MINUTE;
        }
    }
}
