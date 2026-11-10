/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.onboarding.audit;

import org.graylog2.audit.PluginAuditEventTypes;

import java.util.Set;

public class OnboardingAuditEventTypes implements PluginAuditEventTypes {
    private static final String NAMESPACE = "onboarding";

    public static final String ONBOARDING_DISMISSED = NAMESPACE + ":status:dismissed";

    @Override
    public Set<String> auditEventTypes() {
        return Set.of(ONBOARDING_DISMISSED);
    }
}
