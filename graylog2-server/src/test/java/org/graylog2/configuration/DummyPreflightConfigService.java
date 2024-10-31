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
package org.graylog2.configuration;

import org.apache.commons.lang3.RandomStringUtils;
import org.graylog2.bootstrap.preflight.PreflightConfig;
import org.graylog2.bootstrap.preflight.PreflightConfigResult;
import org.graylog2.bootstrap.preflight.PreflightConfigService;
import org.graylog2.bootstrap.preflight.PreflightConstants;

public class DummyPreflightConfigService implements PreflightConfigService {

    private PreflightConfigResult result;
    private final String initialPassword = RandomStringUtils.randomAlphabetic(PreflightConstants.INITIAL_PASSWORD_LENGTH);

    @Override
    public PreflightConfig setConfigResult(PreflightConfigResult result) {
        this.result = result;
        return new PreflightConfig(result);
    }

    @Override
    public PreflightConfigResult getPreflightConfigResult() {
        return result;
    }

    @Override
    public String getPreflightPassword() {
        return initialPassword;
    }
}
