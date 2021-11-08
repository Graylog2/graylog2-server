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
package org.graylog.plugins.views.search;

import org.graylog.plugins.views.search.db.SearchDbService;
import org.graylog.plugins.views.search.errors.PermissionException;
import org.graylog.plugins.views.search.permissions.SearchPermissions;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewService;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SearchDomain {
    private final SearchDbService dbService;
    private final ViewService viewService;

    @Inject
    public SearchDomain(SearchDbService dbService, ViewService viewService) {
        this.dbService = dbService;
        this.viewService = viewService;
    }

    public Optional<Search> getForUser(String id, SearchUser searchUser) {
        final Optional<Search> search = dbService.get(id);

        search.ifPresent(s -> checkPermission(searchUser.username(), searchUser, s));

        return search;
    }

    private void checkPermission(String userName, SearchUser searchUser, Search search) {
        if (!hasReadPermissionFor(searchUser, searchUser::canReadView, search))
            throw new PermissionException("User " + userName + " does not have permission to load search " + search.id());
    }

    public List<Search> getAllForUser(SearchPermissions searchPermissions, Predicate<ViewDTO> viewReadPermission) {
        return dbService.streamAll()
                .filter(s -> hasReadPermissionFor(searchPermissions, viewReadPermission, s))
                .collect(Collectors.toList());
    }

    private boolean hasReadPermissionFor(SearchPermissions searchPermissions, Predicate<ViewDTO> viewReadPermission, Search search) {
        if (searchPermissions.owns(search)) {
            return true;
        }
        // Allowed if permissions exist for a referencing view
        final Collection<ViewDTO> views = viewService.forSearch(search.id());
        if (views.isEmpty())
            return false;

        return views.stream().anyMatch(viewReadPermission);
    }
}
