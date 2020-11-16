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
package org.graylog2.bindings;

import com.google.inject.multibindings.MapBinder;
import org.graylog2.bindings.providers.DefaultPasswordAlgorithmProvider;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.plugin.security.PasswordAlgorithm;
import org.graylog2.security.hashing.BCryptPasswordAlgorithm;
import org.graylog2.security.hashing.SHA1HashPasswordAlgorithm;
import org.graylog2.users.DefaultPasswordAlgorithm;

public class PasswordAlgorithmBindings extends Graylog2Module {
    @Override
    protected void configure() {
        bindPasswordAlgorithms();
    }

    private void bindPasswordAlgorithms() {
        MapBinder<String, PasswordAlgorithm> passwordAlgorithms = MapBinder.newMapBinder(binder(), String.class, PasswordAlgorithm.class);
        passwordAlgorithms.addBinding("sha-1").to(SHA1HashPasswordAlgorithm.class);
        passwordAlgorithms.addBinding("bcrypt").to(BCryptPasswordAlgorithm.class);

        bind(PasswordAlgorithm.class).annotatedWith(DefaultPasswordAlgorithm.class).toProvider(DefaultPasswordAlgorithmProvider.class);
    }
}
