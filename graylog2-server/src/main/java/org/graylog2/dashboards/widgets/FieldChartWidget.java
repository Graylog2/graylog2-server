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
import org.graylog2.indexer.results.HistogramResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.timeranges.TimeRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

public class FieldChartWidget extends ChartWidget {

    private static final Logger LOG = LoggerFactory.getLogger(FieldChartWidget.class);

    private final String query;
    private final String field;
    private final String statisticalFunction;
    private final String renderer;
    private final String interpolation;
    private final Searches searches;

    public FieldChartWidget(MetricRegistry metricRegistry, Searches searches, String id, String description, WidgetCacheTime cacheTime, Map<String, Object> config, String query, TimeRange timeRange, String creatorUserId) throws InvalidWidgetConfigurationException {
        super(metricRegistry, Type.FIELD_CHART, id, timeRange, description, cacheTime, config, creatorUserId);
        this.searches = searches;

        if (!checkConfig(config)) {
            throw new InvalidWidgetConfigurationException("Missing or invalid widget configuration. Provided config was: " + config.toString());
        }

        if (query == null || query.trim().isEmpty()) {
            this.query = "*";
        } else {
            this.query = query;
        }

        this.field = (String) config.get("field");
        this.statisticalFunction = (String) config.get("valuetype");
        this.renderer = (String) config.get("renderer");
        this.interpolation = (String) config.get("interpolation");
    }

    @Override
    public Map<String, Object> getPersistedConfig() {
        final ImmutableMap.Builder<String, Object> persistedConfig = ImmutableMap.<String, Object>builder()
                .putAll(super.getPersistedConfig())
                .put("query", query)
                .put("field", field)
                .put("valuetype", statisticalFunction)
                .put("renderer", renderer)
                .put("interpolation", interpolation);

        return persistedConfig.build();
    }

    @Override
    protected ComputationResult compute() {
        String filter = null;
        if (!isNullOrEmpty(streamId)) {
            filter = "streams:" + streamId;
        }

        try {
            final HistogramResult histogramResult = searches.fieldHistogram(
                    query,
                    field,
                    Searches.DateHistogramInterval.valueOf(interval.toString().toUpperCase(Locale.ENGLISH)),
                    filter,
                    this.getTimeRange(),
                    "cardinality".equalsIgnoreCase(statisticalFunction));

            return new ComputationResult(histogramResult.getResults(), histogramResult.took().millis(), histogramResult.getHistogramBoundaries());
        } catch (Searches.FieldTypeException e) {
            String msg = "Could not calculate [" + this.getClass().getCanonicalName() + "] widget <" + getId() + ">. Not a numeric field? The field was [" + field + "]";
            LOG.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    private boolean checkConfig(Map<String, Object> config) {
        return config.containsKey("field") && config.containsKey("valuetype")
                && config.containsKey("renderer")
                && config.containsKey("interpolation")
                && config.containsKey("interval");

    }
}