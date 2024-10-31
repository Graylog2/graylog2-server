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
package org.graylog2.users;

import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNTypes;
import org.graylog.security.DBGrantService;
import org.graylog.security.GrantDTO;
import org.graylog2.users.events.UserDeletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Listens for {@link UserDeletedEvent}s to remove orphaned grants from the DB. It will go through all grants present
 * in the DB, not just the grants for the user mentioned in the delete event to remove any leftover grants for users
 * which don't exist anymore.
 */
@Singleton
public class GrantsCleanupListener {
    private static final Logger log = LoggerFactory.getLogger(GrantsCleanupListener.class);

    private final DBGrantService grantService;
    private final PaginatedUserService userService;
    private final GRNRegistry grnRegistry;

    @Inject
    public GrantsCleanupListener(EventBus eventBus, DBGrantService grantService, PaginatedUserService userService,
                                 GRNRegistry grnRegistry) {
        this.grantService = grantService;
        this.userService = userService;
        this.grnRegistry = grnRegistry;
        eventBus.register(this);
    }

    @Subscribe
    public void handleUserDeletedEvent(UserDeletedEvent event) {
        final Set<GRN> grantees;
        try (final Stream<GrantDTO> grantStream = grantService.streamAll()) {
            grantees = grantStream
                    .map(GrantDTO::grantee)
                    .filter(grantee -> grantee.grnType().equals(GRNTypes.USER))
                    .collect(Collectors.toSet());
        }

        final Set<GRN> users;
        try (final Stream<UserOverviewDTO> userStream = userService.streamAll()) {
            users = userStream
                    .map(user -> grnRegistry.newGRN(GRNTypes.USER.type(), user.id()))
                    .collect(Collectors.toSet());
        }

        final Sets.SetView<GRN> removedGrantees = Sets.difference(grantees, users);

        if (!removedGrantees.isEmpty()) {
            log.debug("Clearing grants for {} grantees ({}).", removedGrantees.size(), removedGrantees);
            removedGrantees.forEach(grantService::deleteForGrantee);
        }
    }
}
