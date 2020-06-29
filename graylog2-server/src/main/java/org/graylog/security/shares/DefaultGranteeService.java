package org.graylog.security.shares;

import com.google.common.collect.ImmutableSet;
import org.graylog.security.shares.EntitySharePrepareResponse.AvailableGrantee;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.UserService;
import org.graylog2.utilities.GRNRegistry;

import javax.inject.Inject;

public class DefaultGranteeService implements GranteeService {
    private final UserService userService;
    private final GRNRegistry grnRegistry;

    @Inject
    public DefaultGranteeService(UserService userService, GRNRegistry grnRegistry) {
        this.userService = userService;
        this.grnRegistry = grnRegistry;
    }

    @Override
    public ImmutableSet<AvailableGrantee> getAvailableGrantees(User sharingUser) {
        return ImmutableSet.<AvailableGrantee>builder()
                .addAll(getAvailableUserGrantees(sharingUser))
                .add(getGlobalGrantee())
                .build();
    }

    private ImmutableSet<AvailableGrantee> getAvailableUserGrantees(User sharingUser) {
        // TODO: We can only expose users that are in the same teams as the sharing user by default. There should
        //       also be a global config setting to allow exposing all existing users in the system.
        return userService.loadAll().stream()
                .map(user -> AvailableGrantee.create(
                        grnRegistry.newGRN("user", user.getName()).toString(),
                        "user",
                        user.getFullName()
                ))
                .collect(ImmutableSet.toImmutableSet());
    }

    private AvailableGrantee getGlobalGrantee() {
        return AvailableGrantee.create(
                GRNRegistry.GLOBAL_USER_GRN,
                "global",
                "Everyone"
        );
    }

}
