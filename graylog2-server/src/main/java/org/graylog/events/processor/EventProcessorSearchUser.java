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
package org.graylog.events.processor;

import com.google.inject.assistedinject.Assisted;
import jakarta.inject.Inject;
import org.apache.shiro.subject.Subject;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.PermittedStreams;
import org.graylog.plugins.views.search.views.ViewResolver;
import org.graylog2.plugin.database.users.User;

import java.util.Map;

public class EventProcessorSearchUser extends SearchUser {

    @Inject
    public EventProcessorSearchUser(@Assisted User owner,
                                    @Assisted Subject subject,
                                    PermittedStreams permittedStreams,
                                    Map<String, ViewResolver> viewResolvers) {
        super(owner, subject::isPermitted, (perm, id) -> subject.isPermitted(perm + ":" + id), permittedStreams, viewResolvers);
    }

    public interface Factory {
        EventProcessorSearchUser create(User owner, Subject subject);
    }
}
