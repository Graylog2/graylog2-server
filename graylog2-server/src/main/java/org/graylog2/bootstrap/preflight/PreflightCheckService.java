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
package org.graylog2.bootstrap.preflight;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

public class PreflightCheckService {
    private static final Logger LOG = LoggerFactory.getLogger(PreflightCheckService.class);

    private final Map<String, PreflightCheck> preflightChecks;
    private final boolean skipPreflightChecks;

    @Inject
    public PreflightCheckService(Map<String, PreflightCheck> preflightChecks,
                                 @Named(value = "skip_preflight_checks") boolean skipPreflightChecks) {
        this.preflightChecks = preflightChecks;
        this.skipPreflightChecks = skipPreflightChecks;
    }

    /**
     * Performs preflight checks. In case of a failure, logs the cause of it.
     *
     * @throws PreflightCheckException If a preflight check failed.
     */
    public void runChecks() throws PreflightCheckException {
        if (skipPreflightChecks) {
            LOG.info("Skipping preflight checks");
            return;
        }

        try {
            preflightChecks.values().forEach(PreflightCheck::runCheck);
        } catch (PreflightCheckException e) {
            LOG.error("Preflight check failed with error: {}", e.getLocalizedMessage());
            throw e;
        }
    }
}
