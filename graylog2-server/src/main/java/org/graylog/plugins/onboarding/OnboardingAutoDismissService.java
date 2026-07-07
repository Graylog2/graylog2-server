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
package org.graylog.plugins.onboarding;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.rest.models.system.inputs.responses.InputCreated;

/**
 * This class auto-dismisses the onboarding state if an input has been successfully created and the onboarding
 * state is not already in DISMISSED or FINISHED state.
 */
@Singleton
public class OnboardingAutoDismissService {
    private final ClusterConfigService clusterConfigService;
    private boolean alreadyChecked;

    @Inject
    public OnboardingAutoDismissService(final ClusterConfigService clusterConfigService,
                                        final EventBus eventBus) {
        this.clusterConfigService = clusterConfigService;
        eventBus.register(this);
    }

    boolean needsDismissal(OnboardingStatus status) {
        return !(OnboardingStatus.DISMISSED.equals(status) || OnboardingStatus.FINISHED.equals(status));
    }

    @Subscribe
    @SuppressWarnings("unused")
    synchronized public void handleInputCreate(final InputCreated event) {
        if(!alreadyChecked) {
            final OnboardingState status = clusterConfigService.get(OnboardingState.class);
            if(status == null || needsDismissal(status.status())) {
                clusterConfigService.write(new OnboardingState(OnboardingStatus.DISMISSED));
            }
            alreadyChecked = true;
        }
    }
}
