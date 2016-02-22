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
import org.graylog2.indexer.searches.Searches;
import org.graylog2.plugin.dashboards.widgets.ComputationResult;
import org.graylog2.plugin.dashboards.widgets.WidgetStrategy;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

public class StreamSearchResultCountWidgetStrategy extends SearchResultCountWidgetStrategy {
    public interface Factory extends WidgetStrategy.Factory<StreamSearchResultCountWidgetStrategy> {
        @Override
        StreamSearchResultCountWidgetStrategy create(Map<String, Object> config, TimeRange timeRange, String widgetId);
    }

    private final String streamId;

    @AssistedInject
    public StreamSearchResultCountWidgetStrategy(Searches searches, @Assisted Map<String, Object> config, @Assisted TimeRange timeRange, @Assisted String widgetId) {
        super(searches, config, timeRange, widgetId);
        this.streamId = (String) config.get("stream_id");
    }

    @Override
    public ComputationResult compute() {
        String filter = null;
        if (!isNullOrEmpty(streamId)) {
            filter = "streams:" + streamId;
        }
        return computeInternal(filter);
    }
}
