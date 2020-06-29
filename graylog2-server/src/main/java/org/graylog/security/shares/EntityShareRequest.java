package org.graylog.security.shares;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog2.utilities.GRN;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.MoreObjects.firstNonNull;

@AutoValue
@JsonAutoDetect
public abstract class EntityShareRequest {
    @JsonProperty("grantee_roles")
    public abstract ImmutableMap<GRN, GRN> granteeRoles();

    public Set<GRN> grantees() {
        return granteeRoles().keySet();
    }

    public Set<GRN> roles() {
        return ImmutableSet.copyOf(granteeRoles().values());
    }

    @JsonCreator
    public static EntityShareRequest create(@JsonProperty("grantee_roles") Map<GRN, GRN> granteeRoles) {
        return new AutoValue_EntityShareRequest(ImmutableMap.copyOf(firstNonNull(granteeRoles, Collections.emptyMap())));
    }
}
