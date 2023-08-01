package org.graylog.security.events;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.UserManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class AuthServiceDeactivatedEventListener {
    private static final Logger LOG = LoggerFactory.getLogger(AuthServiceDeactivatedEventListener.class);

    private final UserManagementService userManagementService;

    @Inject
    public AuthServiceDeactivatedEventListener(EventBus eventBus,
                                               UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
        eventBus.register(this);
    }

    @Subscribe
    public void handleActiveBackendChanged(ActiveAuthServiceBackendChangedEvent event) {
        event.previouslyActiveBackend().ifPresent(this::disableUsers);
    }

    private void disableUsers(String deactivatedBackend) {
        LOG.debug("Disabling users for authentication service <{}>", deactivatedBackend);
        final List<User> users = userManagementService.loadAllForAuthServiceBackend(deactivatedBackend);
        users.stream().filter(user -> User.AccountStatus.ENABLED.equals(user.getAccountStatus())).forEach(user -> {
            try {
                userManagementService.setUserStatus(user, User.AccountStatus.DISABLED);
            } catch (ValidationException e) {
                LOG.warn("Error disabling user {}", user.getName(), e);
            }
        });
    }

}
