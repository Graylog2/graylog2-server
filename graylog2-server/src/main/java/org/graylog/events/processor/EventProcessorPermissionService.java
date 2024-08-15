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

import jakarta.inject.Inject;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.graylog.grn.GRNTypes;
import org.graylog.security.entities.EntityOwnershipService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.security.AccessToken;
import org.graylog2.security.AccessTokenService;
import org.graylog2.shared.security.AccessTokenAuthToken;
import org.graylog2.shared.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

public class EventProcessorPermissionService {

    private final EntityOwnershipService entityOwnershipService;
    private final UserService userService;
    private final EventProcessorSearchUser.Factory searchUserFactory;
    private final AccessTokenService accessTokenService;

    @Inject
    public EventProcessorPermissionService(EntityOwnershipService entityOwnershipService,
                                           UserService userService,
                                           EventProcessorSearchUser.Factory searchUserFactory,
                                           AccessTokenService accessTokenService) {
        this.entityOwnershipService = entityOwnershipService;
        this.userService = userService;
        this.searchUserFactory = searchUserFactory;
        this.accessTokenService = accessTokenService;
    }

    public Optional<User> getOwner(String eventDefinitionId) {
        final Optional<String> eventProcessorOwnerId = entityOwnershipService.getOwnerGranteeId(GRNTypes.EVENT_DEFINITION, eventDefinitionId);
        return eventProcessorOwnerId.map(userService::loadById);
    }

    public EventProcessorSearchUser getSearchUser(User user) {
        return searchUserFactory.create(user, newWebSession(user.getName()).subject());
    }

    private WebSession newWebSession(final String userName) {
        final WebSession webSession = new WebSession(
                userName,
                accessTokenService.create(userName, "event-processor" + UUID.randomUUID()),
                new Subject.Builder().host("127.0.0.1").buildSubject()
        );
        webSession.login();
        return webSession;
    }
    public record WebSession(String username, AccessToken accessToken, Subject subject) {
        private static final Logger LOG = LoggerFactory.getLogger(WebSession.class);

        public Session getSession() {
            return subject.getSession();
        }

        public void login() {
            AccessTokenAuthToken accessTokenAuthToken = new AccessTokenAuthToken(
                    accessToken.getToken(),
                    subject.getSession().getHost()
            );
            try {
                subject.login(accessTokenAuthToken);
            } catch (AuthenticationException e) {
                LOG.error("Unable to authenticate internal web connection. Make sure API token authentication is enabled!");
                throw new AuthenticationException(e);
            }
        }
    }
}

