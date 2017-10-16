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

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.dashboards.widgets.InvalidWidgetConfigurationException;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.plugin.dashboards.widgets.ComputationResult;
import org.graylog2.plugin.dashboards.widgets.WidgetStrategy;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.rest.models.search.responses.TermsHistogramResult;
import org.graylog2.utilities.SearchUtils;

import java.util.Map;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.graylog2.utilities.SearchUtils.buildTermsHistogramResult;

public class QuickvaluesHistogramWidgetStrategy extends QuickvaluesBaseWidgetStrategy {
    public interface Factory extends WidgetStrategy.Factory<QuickvaluesHistogramWidgetStrategy> {
        @Override
        QuickvaluesHistogramWidgetStrategy create(Map<String, Object> config, TimeRange timeRange, String widgetId);
    }

    private final int limit;
    private final Searches.DateHistogramInterval interval;

    @AssistedInject
    public QuickvaluesHistogramWidgetStrategy(Searches searches,
                                              @Assisted Map<String, Object> config,
                                              @Assisted TimeRange timeRange,
                                              @Assisted String widgetId) throws InvalidWidgetConfigurationException {
        super(searches, timeRange, config, widgetId);

        this.limit = (int) firstNonNull(config.get("limit"), 5);
        this.interval = SearchUtils.buildInterval((String) config.get("interval"), timeRange);
    }

    @Override
    public ComputationResult compute() {
        String filter = null;
        if (!isNullOrEmpty(streamId)) {
            filter = "streams:" + streamId;
        }

        final Sorting.Direction sortDirection = getSortingDirection(sortOrder);
        final TermsHistogramResult termsHistogram = buildTermsHistogramResult(searches.termsHistogram(field, stackedFields, limit, query, filter, timeRange, interval, sortDirection));

        return new ComputationResult(termsHistogram, termsHistogram.time());
    }
}