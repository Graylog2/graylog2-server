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
package org.graylog2.migrations;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.graylog.plugins.onboarding.OnboardingState;
import org.graylog.plugins.onboarding.OnboardingStatus;
import org.graylog2.featureflag.FeatureFlags;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;

public class V20260624153204_InitializeOnboardingState extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20260624153204_InitializeOnboardingState.class);

    private final boolean isFreshInstallation;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public V20260624153204_InitializeOnboardingState(@Named("isFreshInstallation") boolean isFreshInstallation,
                                                     ClusterConfigService clusterConfigService) {
        this.isFreshInstallation = isFreshInstallation;
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2026-06-24T15:32:04Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            return;
        }

        final OnboardingState status = clusterConfigService.get(OnboardingState.class);
        if (status == null) {
            if (isFreshInstallation) {
                LOG.info("Fresh Graylog installation detected. Creating initial SETUP onboarding state...");
                clusterConfigService.write(new OnboardingState(OnboardingStatus.SETUP));
            } else {
                LOG.info("Adding a FINISHED onboarding state for an already running Graylog installation...");
                clusterConfigService.write(new OnboardingState(OnboardingStatus.FINISHED));
            }
        } else {
            LOG.warn("An OnboardingState exists - which should not be the case at this point. Not writing a state but stopping and finishing the migration anyway.");
        }

        clusterConfigService.write(new MigrationCompleted());
    }

    record MigrationCompleted() {}
}
