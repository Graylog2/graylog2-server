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

import com.google.common.collect.ImmutableMap;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.dashboards.widgets.InvalidWidgetConfigurationException;
import org.graylog2.indexer.results.HistogramResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.plugin.dashboards.widgets.ComputationResult;
import org.graylog2.plugin.dashboards.widgets.WidgetStrategy;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

public class StackedChartWidgetStrategy extends ChartWidgetStrategy {
    public interface Factory extends WidgetStrategy.Factory<StackedChartWidgetStrategy> {
        @Override
        StackedChartWidgetStrategy create(Map<String, Object> config, TimeRange timeRange, String widgetId);
    }

    private static final Logger LOG = LoggerFactory.getLogger(StackedChartWidgetStrategy.class);

    private final List<Series> chartSeries;
    private final Searches searches;
    private final TimeRange timeRange;
    private final String widgetId;

    @AssistedInject
    public StackedChartWidgetStrategy(Searches searches, @Assisted Map<String, Object> config, @Assisted TimeRange timeRange, @Assisted String widgetId) throws InvalidWidgetConfigurationException {
        super(config);
        this.searches = searches;
        this.timeRange = timeRange;
        this.widgetId = widgetId;

        if (!checkConfig(config)) {
            throw new InvalidWidgetConfigurationException("Missing or invalid widget configuration. Provided config was: " + config.toString());
        }

        final Object persistedSeries = config.get("series");

        if (persistedSeries instanceof List) {
            final List chartSeries = (List) persistedSeries;
            this.chartSeries = new ArrayList<>(chartSeries.size());

            for (final Object series : chartSeries) {
                this.chartSeries.add(Series.fromMap((Map<String, Object>) series));
            }
        } else {
            throw new InvalidWidgetConfigurationException("Invalid widget configuration, 'series' should be a list: " + config.toString());
        }
    }

    @Override
    public ComputationResult compute() {
        String filter = null;
        if (!isNullOrEmpty(streamId)) {
            filter = "streams:" + streamId;
        }

        final List<Map> results = new ArrayList<>(chartSeries.size());
        DateTime from = null;
        DateTime to = null;
        long tookMs = 0;

        for (Series series : chartSeries) {
            try {
                final HistogramResult histogramResult = searches.fieldHistogram(
                        series.query,
                        series.field,
                        Searches.DateHistogramInterval.valueOf(interval.toString().toUpperCase(Locale.ENGLISH)),
                        filter,
                        this.timeRange,
                        "cardinality".equalsIgnoreCase(series.statisticalFunction));

                if (from == null) {
                    from = histogramResult.getHistogramBoundaries().getFrom();
                }

                to = histogramResult.getHistogramBoundaries().getTo();

                results.add(histogramResult.getResults());
                tookMs += histogramResult.took().millis();
            } catch (Searches.FieldTypeException e) {
                String msg = "Could not calculate [" + this.getClass().getCanonicalName() + "] widget <" + widgetId + ">. Not a numeric field? The field was [" + series.field + "]";
                LOG.error(msg, e);
                throw new RuntimeException(msg, e);
            }
        }

        final AbsoluteRange computationTimeRange = AbsoluteRange.create(from, to);

        return new ComputationResult(results, tookMs, computationTimeRange);
    }

    private boolean checkConfig(Map<String, Object> config) {
        return config.containsKey("renderer")
                && config.containsKey("interpolation")
                && config.containsKey("interval")
                && config.containsKey("series");
    }

    private static class Series {
        final String field;
        final String query;
        final String statisticalFunction;

        public static Series fromMap(Map<String, Object> fields) {
            return new Series((String) fields.get("query"), (String) fields.get("field"), (String) fields.get("statistical_function"));
        };

        Series(String query, String field, String statisticalFunction) {
            if (query == null || query.trim().isEmpty()) {
                this.query = "*";
            } else {
                this.query = query;
            }
            this.field = field;
            this.statisticalFunction = statisticalFunction;
        }

        public Map<String, Object> toMap() {
            ImmutableMap.Builder<String, Object> mapBuilder = ImmutableMap.<String, Object>builder()
                    .put("query", query)
                    .put("field", field)
                    .put("statistical_function", statisticalFunction);

            return mapBuilder.build();
        }
    }
}