package org.graylog.integrations.aws.resources.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class KinesisPermissionsResponse {

    private static final String SETUP_POLICY = "setup_policy";
    private static final String AUTO_SETUP_POLICY = "auto_setup_policy";

    @JsonProperty(SETUP_POLICY)
    public abstract String setupPolicy();

    @JsonProperty(AUTO_SETUP_POLICY)
    public abstract String autoSetupPolicy();

    public static KinesisPermissionsResponse create(@JsonProperty(SETUP_POLICY) String setupPolicy,
                                                    @JsonProperty(AUTO_SETUP_POLICY) String autoSetupPolicy) {
        return new AutoValue_KinesisPermissionsResponse(setupPolicy, autoSetupPolicy);
    }
}