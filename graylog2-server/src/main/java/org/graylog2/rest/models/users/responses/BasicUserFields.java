package org.graylog2.rest.models.users.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.shared.fields.IdField;

import javax.annotation.Nullable;

public interface BasicUserFields extends IdField {

    @JsonProperty("username")
    String username();

    @JsonProperty("full_name")
    @Nullable
    String fullName();

    @JsonProperty("read_only")
    boolean readOnly();

    @JsonProperty("service_account")
    boolean isServiceAccount();

}
