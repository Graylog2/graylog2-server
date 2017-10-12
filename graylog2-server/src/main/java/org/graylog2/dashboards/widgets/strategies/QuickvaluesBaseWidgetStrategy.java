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

import com.google.common.base.Splitter;
import org.graylog2.dashboards.widgets.InvalidWidgetConfigurationException;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.plugin.dashboards.widgets.WidgetStrategy;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Strings.isNullOrEmpty;

public abstract class QuickvaluesBaseWidgetStrategy implements WidgetStrategy {
    protected final String query;
    @Nullable
    protected final String streamId;
    protected final String field;
    protected final Searches searches;
    protected final TimeRange timeRange;
    protected final String sortOrder;
    protected final List<String> stackedFields;

    QuickvaluesBaseWidgetStrategy(Searches searches, TimeRange timeRange, Map<String, Object> config, String widgetId) throws InvalidWidgetConfigurationException {
        this.searches = searches;
        this.timeRange = timeRange;

        if (!checkConfig(config)) {
            throw new InvalidWidgetConfigurationException("Missing or invalid widget configuration. Provided config was: " + config.toString());
        }

        this.query = (String)config.get("query");

        this.field = (String) config.get("field");
        this.streamId = (String) config.get("stream_id");

        this.sortOrder = (String) firstNonNull(config.get("sort_order"), "desc");
        this.stackedFields = getStackedFields(config.get("stacked_fields"));
    }

    private static List<String> getStackedFields(@Nullable Object value) {
        final String stackedFieldsString = (String) firstNonNull(value, "");
        return Splitter.on(',').trimResults().omitEmptyStrings().splitToList(stackedFieldsString);
    }

    protected static Sorting.Direction getSortingDirection(String sort) {
        if (isNullOrEmpty(sort)) {
            return Sorting.Direction.DESC;
        }

        try {
            return Sorting.Direction.valueOf(sort.toUpperCase(Locale.ENGLISH));
        } catch (Exception e) {
            return Sorting.Direction.DESC;
        }
    }

    private boolean checkConfig(Map<String, Object> config) {
        return config.containsKey("field");
    }
}
