package org.graylog.security;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.autovalue.WithBeanGetter;

import java.util.Set;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class GrantChangedEvent {
    private final static String FIELD_GRANT_IDS = "grant_ids";

    @JsonProperty(FIELD_GRANT_IDS)
    public abstract Set<String> grantIds();

    public static GrantChangedEvent create(Set<String> grantIds) {
        return new AutoValue_GrantChangedEvent(grantIds);
    }

    public static GrantChangedEvent create(String grantId) {
        return new AutoValue_GrantChangedEvent(ImmutableSet.of(grantId));
    }
}
