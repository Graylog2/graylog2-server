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
import com.google.common.collect.Maps;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.dashboards.widgets.InvalidWidgetConfigurationException;
import org.graylog2.indexer.results.TermsResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.plugin.dashboards.widgets.ComputationResult;
import org.graylog2.plugin.dashboards.widgets.WidgetStrategy;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Strings.isNullOrEmpty;

public class QuickvaluesWidgetStrategy implements WidgetStrategy {

    public interface Factory extends WidgetStrategy.Factory<QuickvaluesWidgetStrategy> {
        @Override
        QuickvaluesWidgetStrategy create(Map<String, Object> config, TimeRange timeRange, String widgetId);
    }

    private final String query;
    @Nullable
    private final String streamId;

    private final String field;
    private final Searches searches;
    private final TimeRange timeRange;
    private final String sortOrder;
    private final int dataTableLimit;
    private final List<String> stackedFields;

    @AssistedInject
    public QuickvaluesWidgetStrategy(Searches searches, @Assisted Map<String, Object> config, @Assisted TimeRange timeRange, @Assisted String widgetId) throws InvalidWidgetConfigurationException {
        this.searches = searches;
        this.timeRange = timeRange;

        if (!checkConfig(config)) {
            throw new InvalidWidgetConfigurationException("Missing or invalid widget configuration. Provided config was: " + config.toString());
        }

        this.query = (String)config.get("query");

        this.field = (String) config.get("field");
        this.streamId = (String) config.get("stream_id");

        this.sortOrder = (String) firstNonNull(config.get("sort_order"), "desc");
        this.dataTableLimit = (int) firstNonNull(config.get("data_table_limit"), 50);
        this.stackedFields = getStackedFields(config.get("stacked_fields"));
    }

    private static List<String> getStackedFields(@Nullable Object value) {
        final String stackedFieldsString = (String) firstNonNull(value, "");
        return Splitter.on(',').trimResults().omitEmptyStrings().splitToList(stackedFieldsString);
    }

    private static Sorting.Direction getSortingDirection(String sort) {
        if (isNullOrEmpty(sort)) {
            return Sorting.Direction.DESC;
        }

        try {
            return Sorting.Direction.valueOf(sort.toUpperCase(Locale.ENGLISH));
        } catch (Exception e) {
            return Sorting.Direction.DESC;
        }
    }

    @Override
    public ComputationResult compute() {
        String filter = null;
        if (!isNullOrEmpty(streamId)) {
            filter = "streams:" + streamId;
        }

        final Sorting.Direction sortDirection = getSortingDirection(sortOrder);
        final TermsResult terms = searches.terms(field, stackedFields, dataTableLimit, query, filter, this.timeRange, sortDirection);

        Map<String, Object> result = Maps.newHashMap();
        result.put("terms", terms.getTerms());
        result.put("total", terms.getTotal());
        result.put("other", terms.getOther());
        result.put("missing", terms.getMissing());

        return new ComputationResult(result, terms.tookMs());
    }

    private boolean checkConfig(Map<String, Object> config) {
        return config.containsKey("field");
    }
}