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
package org.graylog2.periodical;

import com.google.common.collect.ImmutableMap;
import jakarta.inject.Inject;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.security.AccessTokenImpl;
import org.graylog2.security.AccessTokenService;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.List;

import static org.graylog2.audit.AuditEventTypes.USER_ACCESS_TOKEN_DELETE;

public class ExpiredTokenCleaner extends Periodical {
    public static final Logger LOG = LoggerFactory.getLogger(ExpiredTokenCleaner.class);

    private final AccessTokenService tokenService;
    private final AuditEventSender auditEventSender;
    private final NodeId nodeId;

    @Inject
    public ExpiredTokenCleaner(AccessTokenService tokenService, AuditEventSender auditEventSender, NodeId nodeId) {
        this.tokenService = tokenService;
        this.auditEventSender = auditEventSender;
        this.nodeId = nodeId;
    }

    @Override
    public void doRun() {
        LOG.debug("Start cleaning up expired tokens.");
        final DateTime expireBefore = Tools.nowUTC();
        final List<AccessTokenService.ExpiredToken> expiredTokens = this.tokenService.findExpiredTokens(expireBefore);
        if (!expiredTokens.isEmpty()) {
            LOG.info("Found {} tokens expired before {}.", expiredTokens.size(), expireBefore);
        }

        for (AccessTokenService.ExpiredToken token : expiredTokens) {
            ImmutableMap.Builder<String, Object> ctxBuilder = ImmutableMap.builder();
            ctxBuilder.put(AccessTokenImpl.NAME, token.tokenName()).put("userId", token.userId());
            try {
                this.tokenService.deleteById(token.id());
                LOG.info("Successfully removed expired token \"{}\" (id: {}) for user <{}>.", token.tokenName(), token.id(), token.userId());
                this.auditEventSender.success(AuditActor.system(nodeId), USER_ACCESS_TOKEN_DELETE, ctxBuilder.build());
            } catch (Exception e) {
                LOG.warn("Failed to remove expired token \"{}\" (id: {}) for user <{}>", token.tokenName(), token.id(), token.userId(), e);
                ctxBuilder.put("Failure", e.getMessage());
                this.auditEventSender.failure(AuditActor.system(nodeId), USER_ACCESS_TOKEN_DELETE, ctxBuilder.build());
            }
        }
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return false;
    }

    @Override
    public boolean leaderOnly() {
        //Don't run this periodical concurrently on multiple nodes:
        return true;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return false;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        //Run every 5 minutes:
        return 5 * 60;
    }

    @Override
    @Nonnull
    protected Logger getLogger() {
        return LOG;
    }
}
