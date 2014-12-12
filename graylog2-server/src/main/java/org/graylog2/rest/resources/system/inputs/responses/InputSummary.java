package org.graylog2.rest.resources.system.inputs.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Created by dennis on 12/12/14.
 */
@JsonAutoDetect
@AutoValue
public abstract class InputSummary {
    @JsonProperty
    public abstract String title();
    @JsonProperty
    public abstract String persistId();
    @JsonProperty
    public abstract Boolean global();
    @JsonProperty
    public abstract String name();
    @JsonProperty
    @Nullable
    public abstract String contentPack();
    @JsonProperty
    public abstract String inputId();
    @JsonProperty
    public abstract DateTime createdAt();
    @JsonProperty
    public abstract String type();
    @JsonProperty
    public abstract String creatorUserId();
    @JsonProperty
    public abstract Map<String, Object> attributes();
    @JsonProperty
    public abstract Map<String, String> staticFields();

    public static InputSummary create(String title,
                                      String persistId,
                                      Boolean global,
                                      String name,
                                      @Nullable String contentPack,
                                      String inputId,
                                      DateTime createdAt,
                                      String type,
                                      String creatorUserId,
                                      Map<String, Object> attributes,
                                      Map<String, String> staticFields) {
        return new AutoValue_InputSummary(title, persistId, global, name, contentPack, inputId, createdAt, type, creatorUserId, attributes, staticFields);
    }
}
