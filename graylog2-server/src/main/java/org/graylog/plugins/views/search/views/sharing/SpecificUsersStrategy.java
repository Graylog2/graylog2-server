package org.graylog.plugins.views.search.views.sharing;

import org.graylog2.plugin.database.users.User;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

public class SpecificUsersStrategy implements SharingStrategy<SpecificUsers> {
    @Override
    public boolean isAllowedToSee(@Nullable User user, @NotNull SpecificUsers viewSharing) {
        if (user == null) {
            return false;
        }
        return user.isLocalAdmin() || viewSharing.users().stream().anyMatch(userName -> user.getName().equals(userName));
    }
}
