/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.views.search.permissions;

import com.google.common.base.Objects;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.rest.PermittedStreams;
import org.graylog.plugins.views.search.rest.ViewsRestPermissions;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewLike;
import org.graylog.plugins.views.search.views.ViewResolver;
import org.graylog.plugins.views.search.views.ViewResolverDecoder;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.security.RestPermissions;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class SearchUser implements SearchPermissions, StreamPermissions, ViewPermissions {
    private static final Logger LOG = LoggerFactory.getLogger(SearchUser.class);
    private final User currentUser;
    private final Predicate<String> isPermitted;
    private final BiPredicate<String, String> isPermittedEntity;
    private final UserStreams userStreams;
    private final Map<String, ViewResolver> viewResolvers;

    public SearchUser(User currentUser, Predicate<String> isPermitted, BiPredicate<String, String> isPermittedEntity,
                      PermittedStreams permittedStreams, Map<String, ViewResolver> viewResolvers) {
        this.currentUser = currentUser;
        this.isPermitted = isPermitted;
        this.isPermittedEntity = isPermittedEntity;
        this.userStreams = new UserStreams(this, permittedStreams);
        this.viewResolvers = viewResolvers;
    }

    public Optional<DateTimeZone> timeZone() {
        return Optional.ofNullable(this.currentUser.getTimeZone());
    }

    public String username() {
        return this.currentUser.getName();
    }

    public boolean canReadView(ViewLike view) {
        final String viewId = view.id();

        // If a resolved view id is provided, delegate the permissions check to the resolver.
        final ViewResolverDecoder decoder = new ViewResolverDecoder(viewId);
        if (decoder.isResolverViewId()) {
            final ViewResolver viewResolver = viewResolvers.get(decoder.getResolverName());
            if (viewResolver != null) {
                return viewResolver.canReadView(viewId, isPermitted, isPermittedEntity);
            } else {
                // Resolved view could not be found, so permissions cannot be checked.
                LOG.error("View resolver [{}] could not be found.", decoder.getResolverName());
                return false;
            }
        }

        // Proceed to standard views permission check.
        return isPermitted(ViewsRestPermissions.VIEW_READ, viewId)
                || (view.type().equals(ViewDTO.Type.DASHBOARD) && isPermitted(RestPermissions.DASHBOARDS_READ, viewId));
    }

    @Override
    public boolean canCreateDashboards() {
        return isPermitted(RestPermissions.DASHBOARDS_CREATE);
    }

    @Override
    public boolean canUpdateView(ViewLike view) {
        return view.type().equals(ViewDTO.Type.DASHBOARD)
                ? isPermitted(ViewsRestPermissions.VIEW_EDIT, view.id()) || isPermitted(RestPermissions.DASHBOARDS_EDIT, view.id())
                : isPermitted(ViewsRestPermissions.VIEW_EDIT, view.id());
    }

    @Override
    public boolean canDeleteView(ViewLike view) {
        return isPermitted(ViewsRestPermissions.VIEW_DELETE, view.id());
    }

    public boolean canReadStream(String streamId) {
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

    public UserStreams streams() {
        return this.userStreams;
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
