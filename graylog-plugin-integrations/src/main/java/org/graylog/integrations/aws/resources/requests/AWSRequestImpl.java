package org.graylog.integrations.aws.resources.requests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

/**
 * A common implementation on AWSRequest, which can be used for any AWS request that just needs region and credentials.
 */
@AutoValue
@WithBeanGetter
@JsonDeserialize(builder = AWSRequestImpl.Builder.class)
public abstract class AWSRequestImpl implements AWSRequest {

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public static abstract class Builder implements AWSRequest.Builder<Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_AWSRequestImpl.Builder();
        }

        public abstract AWSRequestImpl build();
    }
}