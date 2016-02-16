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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.graylog2.indexer.results.TermsResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.plugin.dashboards.widgets.ComputationResult;
import org.graylog2.plugin.dashboards.widgets.WidgetStrategy;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

public class QuickvaluesWidget implements WidgetStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(QuickvaluesWidget.class);

    private final String query;
    @Nullable
    private final String streamId;

    private final String field;
    private final Searches searches;
    private final TimeRange timeRange;

    private final Boolean showPieChart;
    private final Boolean showDataTable;

    public QuickvaluesWidget(Searches searches, Map<String, Object> config, String query, TimeRange timeRange) throws InvalidWidgetConfigurationException {
        this.searches = searches;
        this.timeRange = timeRange;

        if (!checkConfig(config)) {
            throw new InvalidWidgetConfigurationException("Missing or invalid widget configuration. Provided config was: " + config.toString());
        }

        this.query = query;

        this.field = (String) config.get("field");
        this.streamId = (String) config.get("stream_id");

        this.showPieChart = config.get("show_pie_chart") != null && Boolean.parseBoolean(String.valueOf(config.get("show_pie_chart")));
        this.showDataTable = !config.containsKey("show_data_table") || Boolean.parseBoolean(String.valueOf(config.get("show_data_table")));
    }

    public String getQuery() {
        return query;
    }

    public Map<String, Object> getPersistedConfig() {
        final ImmutableMap.Builder<String, Object> persistedConfig = ImmutableMap.<String, Object>builder()
                .putAll(ImmutableMap.of("timerange", this.timeRange.getPersistedConfig()))
                .put("query", query)
                .put("field", field)
                .put("show_pie_chart", showPieChart)
                .put("show_data_table", showDataTable);

        if (!isNullOrEmpty(streamId)) {
            persistedConfig.put("stream_id", streamId);
        }

        return persistedConfig.build();
    }

    @Override
    public ComputationResult compute() {
        String filter = null;
        if (!isNullOrEmpty(streamId)) {
            filter = "streams:" + streamId;
        }

        final TermsResult terms = searches.terms(field, 50, query, filter, this.timeRange);

        Map<String, Object> result = Maps.newHashMap();
        result.put("terms", terms.getTerms());
        result.put("total", terms.getTotal());
        result.put("other", terms.getOther());
        result.put("missing", terms.getMissing());

        return new ComputationResult(result, terms.took().millis());
    }

    private boolean checkConfig(Map<String, Object> config) {
        return config.containsKey("field");
    }
}