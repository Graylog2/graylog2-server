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
package org.graylog.integrations.aws.resources.requests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

/**
 * This request is used to save a new Kinesis AWS input. Each type of AWS input will use it's own request
 * object due to typically very unique required fields for each.
 */
@AutoValue
@WithBeanGetter
@JsonDeserialize(builder = AWSInputCreateRequest.Builder.class)
public abstract class AWSInputCreateRequest implements AWSRequest {

    private static final String NAME = "name";
    private static final String AWS_MESSAGE_TYPE = "aws_input_type";
    private static final String STREAM_NAME = "stream_name";
    private static final String BATCH_SIZE = "batch_size";
    private static final String GLOBAL = "global";
    private static final String THROTTLING_ALLOWED = "enable_throttling";
    private static final String ADD_FLOW_LOG_PREFIX = "add_flow_log_prefix";

    @JsonProperty(NAME)
    public abstract String name();

    @JsonProperty(AWS_MESSAGE_TYPE)
    public abstract String awsMessageType();

    @JsonProperty(STREAM_NAME)
    public abstract String streamName();

    @JsonProperty(BATCH_SIZE)
    public abstract int batchSize();

    @JsonProperty(GLOBAL)
    public abstract boolean global();

    @JsonProperty(THROTTLING_ALLOWED)
    public abstract boolean throttlingAllowed();

    @JsonProperty(ADD_FLOW_LOG_PREFIX)
    public abstract boolean addFlowLogPrefix();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public static abstract class Builder implements AWSRequest.Builder<Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_AWSInputCreateRequest.Builder();
        }

        @JsonProperty(NAME)
        public abstract Builder name(String name);

        @JsonProperty(AWS_MESSAGE_TYPE)
        public abstract Builder awsMessageType(String awsMessageType);

        @JsonProperty(STREAM_NAME)
        public abstract Builder streamName(String streamName);

        @JsonProperty(BATCH_SIZE)
        public abstract Builder batchSize(int batchSize);

        @JsonProperty(GLOBAL)
        public abstract Builder global(boolean global);

        @JsonProperty(THROTTLING_ALLOWED)
        public abstract Builder throttlingAllowed(boolean throttlingAllowed);

        @JsonProperty(ADD_FLOW_LOG_PREFIX)
        public abstract Builder addFlowLogPrefix(boolean addFlowLogPrefix);

        public abstract AWSInputCreateRequest build();
    }
}