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

import com.google.common.collect.Maps;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.dashboards.widgets.InvalidWidgetConfigurationException;
import org.graylog2.indexer.results.TermsResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.plugin.dashboards.widgets.ComputationResult;
import org.graylog2.plugin.dashboards.widgets.WidgetStrategy;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nullable;
import java.util.Map;

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

        return new ComputationResult(result, terms.tookMs());
    }

    private boolean checkConfig(Map<String, Object> config) {
        return config.containsKey("field");
    }
}