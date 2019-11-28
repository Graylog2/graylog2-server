package org.graylog2.rest.models.system.urlwhitelist;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.validation.constraints.NotEmpty;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
@Consumes(MediaType.APPLICATION_JSON)
public abstract class WhitelistCheckRequest {
    @NotEmpty
    @JsonProperty("url")
    public abstract String url();

    @JsonCreator
    public static WhitelistCheckRequest create(@JsonProperty("url") @NotEmpty String url) {
        return new AutoValue_WhitelistCheckRequest(url);
    }
}
