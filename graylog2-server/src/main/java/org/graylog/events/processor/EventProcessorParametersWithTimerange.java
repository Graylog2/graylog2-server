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
