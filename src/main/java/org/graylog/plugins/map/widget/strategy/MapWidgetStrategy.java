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
package org.graylog.plugins.map.widget.strategy;

import com.google.common.collect.Maps;
import com.google.inject.assistedinject.Assisted;
import org.graylog2.dashboards.widgets.InvalidWidgetConfigurationException;
import org.graylog2.indexer.results.TermsResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.plugin.dashboards.widgets.ComputationResult;
import org.graylog2.plugin.dashboards.widgets.WidgetStrategy;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.inject.Inject;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

public class MapWidgetStrategy implements WidgetStrategy {

    public interface Factory extends WidgetStrategy.Factory<MapWidgetStrategy> {
        @Override
        MapWidgetStrategy create(Map<String, Object> config, TimeRange timeRange, String widgetId);
    }

    public final int MAX_RESULTS = 250;
    private final Searches searches;
    private final MapWidgetStrategyConfiguration configuration;

    @Inject
    public MapWidgetStrategy(Searches searches, @Assisted Map<String, Object> config, @Assisted TimeRange timeRange, @Assisted String widgetId) throws InvalidWidgetConfigurationException {
        this.searches = searches;
        this.configuration = MapWidgetStrategyConfiguration.create(config, timeRange);
    }

    @Override
    public ComputationResult compute() {
        String filter = null;
        String streamId = this.configuration.streamId();
        if (!isNullOrEmpty(streamId)) {
            filter = "streams:" + streamId;
        }

        final TermsResult terms = searches.terms(this.configuration.field(), MAX_RESULTS, this.configuration.query(), filter, this.configuration.timeRange());

        Map<String, Object> result = Maps.newHashMap();
        result.put("terms", terms.getTerms());
        result.put("total", terms.getTotal());
        result.put("other", terms.getOther());
        result.put("missing", terms.getMissing());

        return new ComputationResult(result, terms.tookMs());
    }
}
