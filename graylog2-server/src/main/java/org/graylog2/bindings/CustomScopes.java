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

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Scopes;

public class CustomScopes {
    /**
     * A scope for singletons which are meant to be lazily initialized. For instance, guice will initialize all
     * singletons it has bindings for, when entering {@link com.google.inject.Stage#PRODUCTION}. This scope will still
     * enable singleton semantics, but the singleton will only instantiated when it's really needed.
     */
    public static final Scope LAZY_SINGLETON = new Scope() {
        @Override
        public <T> Provider<T> scope(Key<T> key, Provider<T> unscoped) {
            return Scopes.SINGLETON.scope(key, unscoped);
        }

        @Override public String toString() {
            return "CustomScopes.LAZY_SINGLETON";
        }
    };
}
