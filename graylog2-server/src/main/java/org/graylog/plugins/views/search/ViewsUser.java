package org.graylog.plugins.views.search;

import org.graylog2.plugin.database.users.User;

import java.util.Objects;
import java.util.function.Predicate;

public class ViewsUser {
    private final Predicate<String> hasStreamReadPermission;
    private final Predicate<String> hasViewReadPermission;
    private final Predicate<String> hasPermission;
    private final String name;
    private final boolean isAdmin;

    public static ViewsUser fromDbUser(User dbUser, Predicate<String> hasStreamReadPermission, Predicate<String> hasViewReadPermission, Predicate<String> hasPermission) {
        Objects.requireNonNull(dbUser);
        return new ViewsUser(dbUser.getName(), dbUser.isLocalAdmin(), hasStreamReadPermission, hasViewReadPermission, hasPermission);
    }

    public ViewsUser(String name, boolean isLocalAdmin, Predicate<String> hasStreamReadPermission, Predicate<String> hasViewReadPermission, Predicate<String> hasPermission) {
        this.hasStreamReadPermission = hasStreamReadPermission;
        this.hasViewReadPermission = hasViewReadPermission;
        this.hasPermission = hasPermission;
        this.name = name;
        this.isAdmin = isLocalAdmin || hasPermission.test("*");
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
}
