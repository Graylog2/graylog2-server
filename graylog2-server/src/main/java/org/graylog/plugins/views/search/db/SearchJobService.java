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
package org.graylog.plugins.views.search.db;

import jakarta.ws.rs.ForbiddenException;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.SearchJobDTO;
import org.graylog2.shared.utilities.StringUtils;

import java.util.Optional;

public interface SearchJobService {

    SearchJob create(Search search, String owner, Integer cancelAfterSeconds);

    Optional<SearchJobDTO> load(String id, SearchUser searchUser) throws ForbiddenException;

    default boolean cancel(final String id, final SearchUser searchUser) throws ForbiddenException {
        final SearchJob searchJob = getFromCache(id);
        if (searchJob == null) {
            return false;
        } else if (hasPermissionToAccessJob(searchUser, searchJob.getOwner())) {
            searchJob.cancel();
            return true;
        } else {
            throw new ForbiddenException(StringUtils.f("User %s cannot load search job %s that belongs to different user!", searchUser.username(), id));
        }
    }

    SearchJob getFromCache(final String id);

    default boolean hasPermissionToAccessJob(final SearchUser searchUser, final String jobOwner) {
        return jobOwner.equals(searchUser.username()) || searchUser.isAdmin();
    }
}
