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

import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.security.UserSessionTerminationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * Simply calls {@link UserSessionTerminationService#runGlobalSessionTermination()} once on startup.
 * <p>
 * This is a periodical, only to make sure that it's only run on the primary node.
 */
public class UserSessionTerminationPeriodical extends Periodical {
    private static final Logger log = LoggerFactory.getLogger(UserSessionTerminationPeriodical.class);

    private final UserSessionTerminationService sessionTerminationService;

    @Inject
    public UserSessionTerminationPeriodical(UserSessionTerminationService sessionTerminationService) {
        this.sessionTerminationService = sessionTerminationService;
    }

    @Override
    public void doRun() {
        sessionTerminationService.runGlobalSessionTermination();
    }

    @Override
    public boolean runsForever() {
        return true;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return false;
    }

    @Override
    public boolean leaderOnly() {
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
        return 0;
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}
