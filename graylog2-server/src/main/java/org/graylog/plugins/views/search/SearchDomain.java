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
import org.graylog.plugins.views.search.views.ViewResolver;
import org.graylog.plugins.views.search.views.ViewService;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SearchDomain {
    private final SearchDbService dbService;
    private final SearchExecutionGuard executionGuard;
    private final ViewService viewService;
    private final Map<String, ViewResolver> viewResolvers;

    @Inject
    public SearchDomain(SearchDbService dbService,
                        SearchExecutionGuard executionGuard,
                        ViewService viewService, Map<String, ViewResolver> viewResolvers) {
        this.dbService = dbService;
        this.executionGuard = executionGuard;
        this.viewService = viewService;
        this.viewResolvers = viewResolvers;
    }

    public Optional<Search> getForUser(String id, SearchUser searchUser) {
        final Optional<Search> search = dbService.get(id);

        search.ifPresent(s -> checkPermission(searchUser, s));

        return search;
    }

    private void checkPermission(SearchUser searchUser, Search search) {
        if (!hasReadPermissionFor(searchUser, searchUser::canReadView, search))
            throw new PermissionException("User " + searchUser.username() + " does not have permission to load search " + search.id());
    }

    public List<Search> getAllForUser(SearchPermissions searchPermissions, Predicate<ViewDTO> viewReadPermission) {
        return dbService.streamAll()
                .filter(s -> hasReadPermissionFor(searchPermissions, viewReadPermission, s))
                .collect(Collectors.toList());
    }

    public Search saveForUser(Search search, SearchUser searchUser) {
        this.executionGuard.check(search, searchUser::canReadStream);

        final Optional<Search> previous = Optional.ofNullable(search.id()).flatMap(dbService::get);
        if (!searchUser.isAdmin() && !previous.map(searchUser::owns).orElse(true)) {
            throw new PermissionException("Unable to update search with id <" + search.id() + ">, already exists and user is not permitted to overwrite it.");
        }

        return dbService.save(search.withOwner(searchUser.username()));
    }

    private boolean hasReadPermissionFor(SearchPermissions searchPermissions, Predicate<ViewDTO> viewReadPermission, Search search) {
        if (searchPermissions.owns(search)) {
            return true;
        }

        // Allowed if permissions exist for a referencing view
        final Set<ViewDTO> views = new HashSet<>();
        views.addAll(viewService.forSearch(search.id()));
        views.addAll(viewResolvers.values().stream()
                .flatMap(viewResolver -> viewResolver.getBySearchId(search.id()).stream()).collect(Collectors.toSet()));
        if (views.isEmpty())
            return false;

        return views.stream().anyMatch(viewReadPermission);
    }
}
