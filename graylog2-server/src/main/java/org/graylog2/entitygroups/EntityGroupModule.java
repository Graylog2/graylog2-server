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
package org.graylog2.entitygroups;

import org.graylog2.entitygroups.rest.EntityGroupResource;
import org.graylog2.featureflag.FeatureFlags;
import org.graylog2.plugin.PluginModule;
import org.graylog2.shared.utilities.StringUtils;

public class EntityGroupModule extends PluginModule {
    private static final String FEATURE_FLAG = "entity_groups";

    private final FeatureFlags featureFlags;

    public EntityGroupModule(FeatureFlags featureFlags) {
        this.featureFlags = featureFlags;
    }

    @Override
    protected void configure() {
        if (featureFlags.getAll().keySet().stream()
                .map(StringUtils::toLowerCase)
                .anyMatch(s -> s.equals(FEATURE_FLAG)) && featureFlags.isOn(FEATURE_FLAG)) {
            addSystemRestResource(EntityGroupResource.class);
            addAuditEventTypes(EntityGroupAuditEventTypes.class);
        }
    }
}
