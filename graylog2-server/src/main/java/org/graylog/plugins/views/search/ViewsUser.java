package org.graylog.plugins.views.search;

import org.graylog2.plugin.database.users.User;

import java.util.Objects;
import java.util.function.Predicate;

public class ViewsUser {
    private final User dbUser;
    private final Predicate<String> hasStreamReadPermission;
    private final Predicate<String> hasViewReadPermission;
    private final String name;
    private final boolean isAdmin;

    public static ViewsUser fromDbUser(User dbUser, Predicate<String> hasStreamReadPermission, Predicate<String> hasViewReadPermission, Predicate<String> hasPermission) {
        Objects.requireNonNull(dbUser);
        return new ViewsUser(dbUser, hasStreamReadPermission, hasViewReadPermission, hasPermission);
    }

    public ViewsUser(User dbUser, Predicate<String> hasStreamReadPermission, Predicate<String> hasViewReadPermission, Predicate<String> hasPermission) {
        Objects.requireNonNull(dbUser);
        this.dbUser = dbUser;
        this.name = dbUser.getName();
        this.isAdmin = dbUser.isLocalAdmin() || hasPermission.test("*");
        this.hasStreamReadPermission = hasStreamReadPermission;
        this.hasViewReadPermission = hasViewReadPermission;
    }

    public String getName() {
        return name;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public boolean isOwnerOf(Search search) {
        return search.owner()
                .map(owner -> owner.equals(name))
                .orElse(true);
    }

    public boolean hasStreamReadPermission(String streamId) {
        return hasStreamReadPermission.test(streamId);
    }

    public boolean hasViewReadPermission(String viewId) {
        return hasViewReadPermission.test(viewId);
    }

    //this is in here for compatibility with legacy code which depend on that class. should be removed, if possible.
    public User getDbUser() {
        return dbUser;
    }
}
