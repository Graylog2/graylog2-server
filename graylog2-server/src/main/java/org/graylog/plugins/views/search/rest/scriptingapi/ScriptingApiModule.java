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
package org.graylog.plugins.views.search.rest.scriptingapi;

import org.graylog.plugins.views.ViewsModule;
import org.graylog2.featureflag.FeatureFlags;

public class ScriptingApiModule extends ViewsModule {

    public static final String FEATURE_FLAG = "scripting_api_preview";
    private final FeatureFlags featureFlags;

    public ScriptingApiModule(final FeatureFlags featureFlags) {
        this.featureFlags = featureFlags;
    }

    @Override
    protected void configure() {
        if (featureFlags.isOn(FEATURE_FLAG)) {
            addSystemRestResource(ScriptingApiResource.class);
        }
    }
}
