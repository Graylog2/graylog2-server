package org.graylog.plugins.enterprise.search.views.sharing;

import org.graylog2.plugin.database.users.User;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

public interface SharingStrategy<T extends ViewSharing> {
    boolean isAllowedToSee(@Nullable User user, @NotNull T viewSharing);
}
