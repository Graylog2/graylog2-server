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
package org.graylog2.datatiering;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.Configuration;
import org.graylog2.featureflag.FeatureFlags;

@Singleton
public class DataTieringChecker {
    private final FeatureFlags featureFlags;
    private final Configuration configuration;

    @Inject
    public DataTieringChecker(FeatureFlags featureFlags, Configuration configuration) {
        this.featureFlags = featureFlags;
        this.configuration = configuration;
    }
    public static final String DATA_TIERING_CLOUD_FEATURE = "data_tiering_cloud";


    public boolean isEnabled() {
        boolean isCloud = configuration.isCloud();
        return !isCloud || featureFlags.isOn(DATA_TIERING_CLOUD_FEATURE);
    }
}
