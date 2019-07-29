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
package org.graylog.events.processor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;

public interface EventProcessorParametersWithTimerange extends EventProcessorParameters {
    String FIELD_TIMERANGE = "timerange";

    @JsonProperty(FIELD_TIMERANGE)
    TimeRange timerange();

    @JsonIgnore
    EventProcessorParametersWithTimerange withTimerange(DateTime from, DateTime to);

    interface Builder<SELF> extends EventProcessorParameters.Builder<SELF> {
        @JsonProperty(FIELD_TIMERANGE)
        SELF timerange(TimeRange timerange);
    }

    class FallbackParameters implements EventProcessorParametersWithTimerange {
        @Override
        public String type() {
            return "";
        }

        @Override
        public TimeRange timerange() {
            return null;
        }

        @Override
        public EventProcessorParametersWithTimerange withTimerange(DateTime from, DateTime to) {
            return null;
        }
    }
}
