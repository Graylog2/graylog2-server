/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.dashboards.widgets;

import com.codahale.metrics.MetricRegistry;
import org.graylog2.indexer.IndexHelper;
import org.graylog2.indexer.Indexer;
import org.graylog2.indexer.results.HistogramResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.timeranges.TimeRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class FieldChartWidget extends DashboardWidget {

    private static final Logger LOG = LoggerFactory.getLogger(FieldChartWidget.class);

    private final String query;
    private final TimeRange timeRange;
    private final String streamId;
    private final Map<String, Object> config;
    private final Indexer indexer;

    public FieldChartWidget(MetricRegistry metricRegistry, Indexer indexer, String id, String description, int cacheTime, Map<String, Object> config, String query, TimeRange timeRange, String creatorUserId) throws InvalidWidgetConfigurationException {
        super(metricRegistry, Type.FIELD_CHART, id, description, cacheTime, config, creatorUserId);
        this.indexer = indexer;

        if (!checkConfig(config)) {
            throw new InvalidWidgetConfigurationException("Missing or invalid widget configuration. Provided config was: " + config.toString());
        }

        if (query == null || query.trim().isEmpty()) {
            this.query = "*";
        } else {
            this.query = query;
        }

        this.timeRange = timeRange;
        this.config = config;

        if (config.containsKey("stream_id")) {
            this.streamId = (String) config.get("stream_id");
        } else {
            this.streamId = null;
        }
    }

    @Override
    public Map<String, Object> getPersistedConfig() {
        return new HashMap<String, Object>() {{
            put("query", query);
            put("timerange", timeRange.getPersistedConfig());
            put("stream_id", streamId);

            put("field", config.get("field"));
            put("valuetype", config.get("valuetype"));
            put("renderer", config.get("renderer"));
            put("interpolation", config.get("interpolation"));
            put("interval", config.get("interval"));
        }};
    }

    @Override
    protected ComputationResult compute() {
        String filter = null;
        if (streamId != null && !streamId.isEmpty()) {
            filter = "streams:" + streamId;
        }

        try {
            HistogramResult histogramResult = indexer.searches().fieldHistogram(
                    query,
                    (String) config.get("field"),
                    Indexer.DateHistogramInterval.valueOf(((String) config.get("interval")).toUpperCase()),
                    filter,
                    timeRange
            );

            return new ComputationResult(histogramResult.getResults(), histogramResult.took().millis(), histogramResult.getHistogramBoundaries());
        } catch (Searches.FieldTypeException e) {
            String msg = "Could not calculate [" + this.getClass().getCanonicalName() + "] widget <" + getId() + ">. Not a numeric field? The field was [" + config.get("field") + "]";
            LOG.error(msg, e);
            throw new RuntimeException(msg);
        } catch (IndexHelper.InvalidRangeFormatException e) {
            String msg = "Could not calculate [" + this.getClass().getCanonicalName() + "] widget <" + getId() + ">. Invalid time range.";
            LOG.error(msg, e);
            throw new RuntimeException(msg);
        }
    }

    private boolean checkConfig(Map<String, Object> config) {
        return config.containsKey("field") && config.containsKey("valuetype")
                && config.containsKey("renderer")
                && config.containsKey("interpolation")
                && config.containsKey("interval");

    }

}
