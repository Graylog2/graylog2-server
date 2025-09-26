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
package org.graylog2.security.sessions;

import com.google.common.collect.Sets;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.slf4j.Logger;

import java.util.Date;
import java.util.Optional;
import java.util.Set;

import static org.graylog2.shared.utilities.StringUtils.f;
import static org.slf4j.LoggerFactory.getLogger;

public class SessionConverter {
    private static final Logger LOG = getLogger(SessionConverter.class);

    public static final Set<String> KNOWN_SESSION_KEYS = Set.of(
            DefaultSubjectContext.PRINCIPALS_SESSION_KEY,
            DefaultSubjectContext.AUTHENTICATED_SESSION_KEY,
            SessionDTO.USERNAME_SESSION_KEY,
            SessionDTO.AUTH_CONTEXT_SESSION_KEY
    );

    public static SessionDTO simpleSessionToSessionDTO(SimpleSession simpleSession) {
        final var unknownKeys = unknownSessionKeys(simpleSession);
        if (!unknownKeys.isEmpty()) {
            LOG.warn("Session contains unknown attribute keys: {}. Known keys are: {}",
                    unknownKeys, KNOWN_SESSION_KEYS);
        }

        final var principalInfo = extractPrincipalInfoFromSimpleSession(simpleSession);

        return SessionDTO.builder()
                .expired(simpleSession.isExpired())
                .sessionId(simpleSession.getId().toString())
                .host(simpleSession.getHost())
                .timeout(simpleSession.getTimeout())
                .startTimestamp(simpleSession.getStartTimestamp().toInstant())
                .lastAccessTime(simpleSession.getLastAccessTime().toInstant())
                .userId(principalInfo.map(PrincipalInfo::userId).orElse(null))
                .authenticationRealm(principalInfo.map(PrincipalInfo::realm).orElse(null))
                .authenticated((Boolean) simpleSession.getAttribute(DefaultSubjectContext.AUTHENTICATED_SESSION_KEY))
                .userName((String) simpleSession.getAttribute(SessionDTO.USERNAME_SESSION_KEY))
                .authContext((SessionAuthContext) simpleSession.getAttribute(SessionDTO.AUTH_CONTEXT_SESSION_KEY))
                .build();
    }

    private static Set<Object> unknownSessionKeys(SimpleSession simpleSession) {
        return Sets.difference(Set.copyOf(simpleSession.getAttributeKeys()), KNOWN_SESSION_KEYS);
    }

    public static SimpleSession sessionDTOToSimpleSession(SessionDTO sessionDTO) {
        final var simpleSession = new SimpleSession();
        simpleSession.setId(sessionDTO.sessionId());
        simpleSession.setHost(sessionDTO.host());
        simpleSession.setTimeout(sessionDTO.timeout());
        simpleSession.setStartTimestamp(Date.from(sessionDTO.startTimestamp()));
        simpleSession.setLastAccessTime(Date.from(sessionDTO.lastAccessTime()));
        simpleSession.setExpired(sessionDTO.expired());

        if (sessionDTO.userId().isPresent() && sessionDTO.authenticationRealm().isPresent()) {
            simpleSession.setAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY, new SimplePrincipalCollection(
                    sessionDTO.userId().get(), sessionDTO.authenticationRealm().get()));
        }
        sessionDTO.userName().ifPresent(userName ->
                simpleSession.setAttribute(SessionDTO.USERNAME_SESSION_KEY, userName));
        sessionDTO.authenticated().ifPresent(authenticated ->
                simpleSession.setAttribute(DefaultSubjectContext.AUTHENTICATED_SESSION_KEY, authenticated));
        sessionDTO.authContext().ifPresent(authContext ->
                simpleSession.setAttribute(SessionDTO.AUTH_CONTEXT_SESSION_KEY, authContext));

        return simpleSession;
    }

    private record PrincipalInfo(String userId, String realm) {}

    private static Optional<PrincipalInfo> extractPrincipalInfoFromSimpleSession(SimpleSession simpleSession) {
        final var principals = simpleSession.getAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY);
        if (principals == null) {
            return Optional.empty();
        }
        if (principals instanceof PrincipalCollection principalCollection) {
            if (principalCollection.isEmpty()) {
                return Optional.empty();
            }
            if (principalCollection.asList().size() > 1) {
                LOG.error("Expected exactly one principal in session, but got {}. Taking the first one as user ID and " +
                        "ignoring the others.", principalCollection.asList().size());
            }
            final var realm = principalCollection.getRealmNames().stream().findFirst().orElseThrow(() ->
                    new IllegalStateException("Principal in session has no associated realm."));
            final var principal = principalCollection.fromRealm(realm).iterator().next();
            if (principal instanceof String userId) {
                return Optional.of(new PrincipalInfo(userId, realm));
            } else {
                LOG.error("Expected principal in session to be of type String, but got \"{}\".",
                        principalCollection.getPrimaryPrincipal().getClass().getSimpleName());
                return Optional.empty();
            }
        } else {
            throw new IllegalArgumentException(f("Unexpected type of principals in session." +
                    "Expected \"PrincipalCollection\" but got \"%s\".", principals.getClass().getSimpleName()));
        }
    }
}
