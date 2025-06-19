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
import org.graylog2.bootstrap.preflight.PreflightConfigResult;
import org.graylog2.bootstrap.preflight.PreflightConfigService;
import org.graylog2.bootstrap.preflight.PreflightConstants;

import java.util.concurrent.atomic.AtomicReference;

public class DummyPreflightConfigService implements PreflightConfigService {

    private final AtomicReference<PreflightConfigResult> result = new AtomicReference<>();
    private final String initialPassword = RandomStringUtils.randomAlphabetic(PreflightConstants.INITIAL_PASSWORD_LENGTH);

    @Override
    public ConfigResultState setConfigResult(PreflightConfigResult result) {
        final PreflightConfigResult oldValue = this.result.getAndSet(result);
        return oldValue == null ? ConfigResultState.CREATED : ConfigResultState.UPDATED;
    }

    @Override
    public PreflightConfigResult getPreflightConfigResult() {
        return result.get();
    }

    @Override
    public String getPreflightPassword() {
        return initialPassword;
    }
}
