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
import org.graylog2.indexer.FieldTypeException;
import org.graylog2.indexer.results.HistogramResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.plugin.dashboards.widgets.ComputationResult;
import org.graylog2.plugin.dashboards.widgets.WidgetStrategy;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

public class FieldChartWidgetStrategy extends ChartWidgetStrategy {
    public interface Factory extends WidgetStrategy.Factory<FieldChartWidgetStrategy> {
        @Override
        FieldChartWidgetStrategy create(Map<String, Object> config, TimeRange timeRange, String widgetId);
    }

    private static final Logger LOG = LoggerFactory.getLogger(FieldChartWidgetStrategy.class);

    private final String query;
    private final String field;
    private final String statisticalFunction;
    private final Searches searches;
    private final TimeRange timeRange;
    private final String widgetId;

    @AssistedInject
    public FieldChartWidgetStrategy(Searches searches, @Assisted Map<String, Object> config, @Assisted TimeRange timeRange, @Assisted String widgetId) throws InvalidWidgetConfigurationException {
        super(config);
        this.searches = searches;
        this.timeRange = timeRange;
        this.widgetId = widgetId;

        if (!checkConfig(config)) {
            throw new InvalidWidgetConfigurationException("Missing or invalid widget configuration. Provided config was: " + config.toString());
        }

        final String query = (String)config.get("query");

        if (query == null || query.trim().isEmpty()) {
            this.query = "*";
        } else {
            this.query = query;
        }

        this.field = (String) config.get("field");
        this.statisticalFunction = (String) config.get("valuetype");
    }

    @Override
    public ComputationResult compute() {
        String filter = null;
        if (!isNullOrEmpty(streamId)) {
            filter = "streams:" + streamId;
        }

        final boolean shouldCalculateCardinality = "cardinality".equalsIgnoreCase(statisticalFunction);
        try {
            final HistogramResult histogramResult = searches.fieldHistogram(
                    query,
                    field,
                    Searches.DateHistogramInterval.valueOf(interval.toString().toUpperCase(Locale.ENGLISH)),
                    filter,
                    this.timeRange,
                    !shouldCalculateCardinality,
                    shouldCalculateCardinality);

            return new ComputationResult(histogramResult.getResults(), histogramResult.tookMs(), histogramResult.getHistogramBoundaries());
        } catch (FieldTypeException e) {
            try {
                // Try again not calculating statistics
                final HistogramResult histogramResult = searches.fieldHistogram(
                        query,
                        field,
                        Searches.DateHistogramInterval.valueOf(interval.toString().toUpperCase(Locale.ENGLISH)),
                        filter,
                        this.timeRange,
                        false,
                        shouldCalculateCardinality);

                return new ComputationResult(histogramResult.getResults(), histogramResult.tookMs(), histogramResult.getHistogramBoundaries());
            } catch (FieldTypeException e1) {
                String msg = "Could not calculate [" + this.getClass().getCanonicalName() + "] widget <" + this.widgetId + ">. The field was [" + field + "]";
                LOG.error(msg, e1);
                throw new RuntimeException(msg, e1);
            }
        }
    }

    private boolean checkConfig(Map<String, Object> config) {
        return config.containsKey("field") && config.containsKey("valuetype")
                && config.containsKey("renderer")
                && config.containsKey("interpolation")
                && config.containsKey("interval");

    }
}
