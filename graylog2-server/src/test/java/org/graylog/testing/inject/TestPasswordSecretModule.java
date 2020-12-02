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
package org.graylog.testing.inject;

import com.google.inject.name.Names;
import org.graylog2.plugin.inject.Graylog2Module;

public class TestPasswordSecretModule extends Graylog2Module {
    public static final String TEST_PASSWORD_SECRET = "f9a79178-c949-446d-b0ae-c6d5d9a40ba8";

    @Override
    protected void configure() {
        bind(String.class).annotatedWith(Names.named("password_secret")).toInstance(TEST_PASSWORD_SECRET);
    }
}
