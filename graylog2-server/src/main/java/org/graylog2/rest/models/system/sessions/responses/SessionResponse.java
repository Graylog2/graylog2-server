package org.graylog2.rest.models.system.sessions.responses;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;

public interface SessionResponse {
    Date validUntil();

    @JsonIgnore
    String getAuthenticationToken();
}
