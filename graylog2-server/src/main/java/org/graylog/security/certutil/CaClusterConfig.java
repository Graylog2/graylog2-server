package org.graylog.security.certutil;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.bootstrap.preflight.web.resources.model.CAType;

public record CaClusterConfig(@JsonProperty String id,
                              @JsonProperty CAType type,
                              @JsonProperty String keystore) {
}
