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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.graylog2.indexer.results.HistogramResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

public class StackedChartWidget extends ChartWidget {

    private static final Logger LOG = LoggerFactory.getLogger(StackedChartWidget.class);

    private final String renderer;
    private final String interpolation;
    private final List<Series> chartSeries;
    private final Searches searches;

    public StackedChartWidget(MetricRegistry metricRegistry, Searches searches, String id, String description, WidgetCacheTime cacheTime, Map<String, Object> config, TimeRange timeRange, String creatorUserId) throws InvalidWidgetConfigurationException {
        super(metricRegistry, Type.STACKED_CHART, id, timeRange, description, cacheTime, config, creatorUserId);
        this.searches = searches;

        if (!checkConfig(config)) {
            throw new InvalidWidgetConfigurationException("Missing or invalid widget configuration. Provided config was: " + config.toString());
        }

        this.renderer = (String) config.get("renderer");
        this.interpolation = (String) config.get("interpolation");

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
    public Map<String, Object> getPersistedConfig() {
        final ImmutableList.Builder<Map<String, Object>> seriesBuilder = ImmutableList.builder();
        for (Series series : chartSeries) {
            seriesBuilder.add(series.toMap());
        }

        final ImmutableMap.Builder<String, Object> persistedConfig = ImmutableMap.<String, Object>builder()
                .putAll(super.getPersistedConfig())
                .put("renderer", renderer)
                .put("interpolation", interpolation)
                .put("series", seriesBuilder.build());


        return persistedConfig.build();
    }

    @Override
    protected ComputationResult compute() {
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
                        this.getTimeRange(),
                        "cardinality".equalsIgnoreCase(series.statisticalFunction));

                if (from == null) {
                    from = histogramResult.getHistogramBoundaries().getFrom();
                }

                to = histogramResult.getHistogramBoundaries().getTo();

                results.add(histogramResult.getResults());
                tookMs += histogramResult.took().millis();
            } catch (Searches.FieldTypeException e) {
                String msg = "Could not calculate [" + this.getClass().getCanonicalName() + "] widget <" + getId() + ">. Not a numeric field? The field was [" + series.field + "]";
                LOG.error(msg, e);
                throw new RuntimeException(msg, e);
            }
        }

        final AbsoluteRange computationTimeRange = new AbsoluteRange(from, to);

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