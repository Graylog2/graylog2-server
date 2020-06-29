package org.graylog.security.shares;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import org.graylog2.utilities.GRN;

import java.util.Collections;
import java.util.Map;

import static com.google.common.base.MoreObjects.firstNonNull;

@AutoValue
public abstract class EntitySharePrepareRequest {
    @JsonProperty("selected_grantee_roles")
    public abstract ImmutableMap<GRN, GRN> selectedGranteeRoles();

    @JsonCreator
    public static EntitySharePrepareRequest create(@JsonProperty("selected_grantee_roles") Map<GRN, GRN> selectedGranteeRoles) {
        return new AutoValue_EntitySharePrepareRequest(ImmutableMap.copyOf(firstNonNull(selectedGranteeRoles, Collections.emptyMap())));
    }
}
