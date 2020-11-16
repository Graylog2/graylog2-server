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
package org.graylog.security;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;

// TODO this only works for method injection.
// In theory, by using proxies, it should also work for constructors, etc.
// See: https://stackoverflow.com/a/38060472
public class UserContextBinder extends AbstractBinder {
    @Override
    protected void configure() {
        bindFactory(UserContextFactory.class)
                .to(UserContext.class)
                .in(RequestScoped.class);
    }
}
