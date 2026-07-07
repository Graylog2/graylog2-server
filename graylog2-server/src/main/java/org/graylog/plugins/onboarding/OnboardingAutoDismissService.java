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
