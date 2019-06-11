package org.graylog.plugins.enterprise.search.views.sharing;

import org.graylog2.plugin.database.users.User;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.Map;

public class IsViewSharedForUser {
    private final Map<String, SharingStrategy> sharingStrategies;

    @Inject
    public IsViewSharedForUser(Map<String, SharingStrategy> sharingStrategies) {
        this.sharingStrategies = sharingStrategies;
    }

    public boolean isAllowedToSee(@Nullable User user, @NotNull ViewSharing viewSharing) {
        final SharingStrategy sharingStrategy = this.sharingStrategies.getOrDefault(viewSharing.type(), (u, v) -> false);
        return sharingStrategy.isAllowedToSee(user, viewSharing);
    }
}