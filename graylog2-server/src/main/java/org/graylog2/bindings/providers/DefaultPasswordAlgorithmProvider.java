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
package org.graylog2.bindings.providers;

import org.graylog2.plugin.security.PasswordAlgorithm;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.util.Map;

public class DefaultPasswordAlgorithmProvider implements Provider<PasswordAlgorithm> {
    private final PasswordAlgorithm defaultPasswordAlgorithm;

    @Inject
    public DefaultPasswordAlgorithmProvider(@Named("user_password_default_algorithm") String defaultPasswordAlgorithmName,
                                            Map<String, PasswordAlgorithm> passwordAlgorithms) {
        if (passwordAlgorithms.containsKey(defaultPasswordAlgorithmName)) {
            this.defaultPasswordAlgorithm = passwordAlgorithms.get(defaultPasswordAlgorithmName);
        } else {
            throw new IllegalArgumentException("Invalid default password hashing specified in config. Found: "
                    + defaultPasswordAlgorithmName + ". Valid options: " + passwordAlgorithms.keySet());
        }
    }

    @Override
    public PasswordAlgorithm get() {
        return defaultPasswordAlgorithm;
    }
}
