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
package org.graylog2.security;

import org.graylog2.plugin.security.PasswordAlgorithm;
import org.graylog2.users.DefaultPasswordAlgorithm;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Map;

public class PasswordAlgorithmFactory {
    private final Map<String, PasswordAlgorithm> passwordAlgorithms;
    private final PasswordAlgorithm defaultPasswordAlgorithm;

    @Inject
    public PasswordAlgorithmFactory(Map<String, PasswordAlgorithm> passwordAlgorithms,
                                    @DefaultPasswordAlgorithm PasswordAlgorithm defaultPasswordAlgorithm) {
        this.passwordAlgorithms = passwordAlgorithms;
        this.defaultPasswordAlgorithm = defaultPasswordAlgorithm;
    }

    @Nullable
    public PasswordAlgorithm forPassword(String hashedPassword) {
        for (PasswordAlgorithm passwordAlgorithm : passwordAlgorithms.values()) {
            if (passwordAlgorithm.supports(hashedPassword))
                return passwordAlgorithm;
        }

        return null;
    }

    @Nullable
    public PasswordAlgorithm forName(String name) {
        return this.passwordAlgorithms.get(name);
    }

    public PasswordAlgorithm defaultPasswordAlgorithm() {
        return defaultPasswordAlgorithm;
    }
}
