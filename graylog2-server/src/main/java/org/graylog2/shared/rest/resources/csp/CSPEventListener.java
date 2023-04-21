package org.graylog2.shared.rest.resources.csp;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog.security.events.AuthServiceBackendSavedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CSPEventListener {
    private static final Logger LOG = LoggerFactory.getLogger(CSPEventListener.class);
    private final CSPService cspService;

    @Inject
    public CSPEventListener(EventBus eventBus,
                            CSPService cspService) {
        this.cspService = cspService;
        eventBus.register(this);
    }

    @Subscribe
    public void handleBackendCreated(AuthServiceBackendSavedEvent event) {
        try {
            LOG.info("Updating CSP for authentication service <{}>", event.authServiceId());
            cspService.buildConnectSrc();
        } catch (Exception e) {
            LOG.warn("Failed to update CSP for authentication service <{}>", event.authServiceId());
        }
    }
}
