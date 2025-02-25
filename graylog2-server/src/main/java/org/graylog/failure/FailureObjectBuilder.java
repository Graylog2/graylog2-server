package org.graylog.failure;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

import static org.graylog.failure.Failure.FIELD_FAILED_MESSAGE_ID;
import static org.graylog.failure.Failure.FIELD_FAILED_MESSAGE_TIMESTAMP;
import static org.graylog.failure.Failure.FIELD_FAILURE_CAUSE;
import static org.graylog.failure.Failure.FIELD_FAILURE_DETAILS;
import static org.graylog.failure.Failure.FIELD_FAILURE_TYPE;
import static org.graylog2.plugin.Message.FIELD_MESSAGE;
import static org.graylog2.plugin.Message.FIELD_STREAMS;
import static org.graylog2.plugin.Message.FIELD_TIMESTAMP;
import static org.graylog2.plugin.Tools.buildElasticSearchTimeFormat;
import static org.graylog2.plugin.streams.Stream.FAILURES_STREAM_ID;

public class FailureObjectBuilder {

    final ImmutableMap.Builder<String, Object> esObject;


    public FailureObjectBuilder(Failure inputFailure) {
        esObject = ImmutableMap.<String, Object>builder()
                .put(FIELD_MESSAGE, inputFailure.message())
                .put(FIELD_STREAMS, ImmutableList.of(FAILURES_STREAM_ID))
                .put(FIELD_TIMESTAMP, buildElasticSearchTimeFormat(inputFailure.failureTimestamp()))
                .put(FIELD_FAILURE_TYPE, inputFailure.failureType().toString())
                .put(FIELD_FAILURE_CAUSE, inputFailure.failureCause().label())
                .put(FIELD_FAILURE_DETAILS, inputFailure.failureDetails())

                .put(FIELD_FAILED_MESSAGE_ID, inputFailure.messageId())
                .put(FIELD_FAILED_MESSAGE_TIMESTAMP, buildElasticSearchTimeFormat(inputFailure.messageTimestamp()));
    }

    public FailureObjectBuilder put(String key, Object value) {
        esObject.put(key, value);
        return this;
    }

    public Map<String, Object> build() {
        return esObject.build();
    }

}
