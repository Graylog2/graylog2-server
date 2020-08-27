package org.graylog.security;

import com.google.common.collect.ImmutableSet;
import org.graylog2.audit.PluginAuditEventTypes;

import java.util.Set;

public class SecurityAuditEventTypes implements PluginAuditEventTypes {
    public static final String NAMESPACE = "security:";

    public static final String UPDATE_SHARES = NAMESPACE + "shares:update";

    private static final ImmutableSet<String> EVENT_TYPES = ImmutableSet.of(
            UPDATE_SHARES
    );

    @Override
    public Set<String> auditEventTypes() {
        return EVENT_TYPES;
    }
}

