package org.graylog.plugins.views.search.permissions;

import com.google.common.base.Objects;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.rest.ViewsRestPermissions;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewLike;
import org.graylog.plugins.views.search.views.ViewSummaryDTO;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.security.RestPermissions;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class SearchUser implements SearchPermissions, StreamPermissions, ViewPermissions {
    private final User currentUser;
    private final Predicate<String> isPermitted;
    private final BiPredicate<String, String> isPermittedEntity;

    public SearchUser(User currentUser, Predicate<String> isPermitted, BiPredicate<String, String> isPermittedEntity) {
        this.currentUser = currentUser;
        this.isPermitted = isPermitted;
        this.isPermittedEntity = isPermittedEntity;
    }

    public String username() {
        return this.currentUser.getName();
    }

    public boolean hasViewReadPermission(ViewLike view) {
        final String viewId = view.id();
        return isPermitted(ViewsRestPermissions.VIEW_READ, viewId)
                || (view.type().equals(ViewDTO.Type.DASHBOARD) && isPermitted(RestPermissions.DASHBOARDS_READ, viewId));
    }

    public boolean hasStreamReadPermission(String streamId) {
        return isPermitted(RestPermissions.STREAMS_READ, streamId);
    }

    private boolean isPermitted(String permission) {
        return this.isPermitted.test(permission);
    }

    private boolean isPermitted(String permission, String entityId) {
        return this.isPermittedEntity.test(permission, entityId);
    }

    public boolean owns(Search search) {
        return search.owner().map(o -> o.equals(username())).orElse(true);
    }

    public boolean isAdmin() {
        return this.currentUser.isLocalAdmin() || isPermitted("*");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final SearchUser that = (SearchUser) o;
        return Objects.equal(currentUser, that.currentUser);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(currentUser);
    }
}
