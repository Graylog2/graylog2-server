package org.graylog.plugins.views.search.views.sharing;

import org.graylog2.plugin.database.users.User;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

public class AllUsersOfInstanceStrategy implements SharingStrategy<AllUsersOfInstance> {
    @Override
    public boolean isAllowedToSee(@Nullable User user, @NotNull AllUsersOfInstance viewSharing) {
        return user != null;
    }
}
