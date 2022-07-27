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
package org.graylog2.shared.bindings;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.name.Names;

import javax.inject.Named;

/**
 * This Module binds a {@link Named } boolean that can be used to detect whether a fresh installation
 * of Graylog is happening. <br>
 * Use with {@code @Named("isFreshInstallation") }
 */
public class FreshInstallDetectionModule implements Module {
    public static final String IS_FRESH_INSTALLATION = "isFreshInstallation";
    private final boolean isFreshInstall;

    public FreshInstallDetectionModule(boolean isFreshInstall) {
        this.isFreshInstall = isFreshInstall;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(Boolean.class).annotatedWith(Names.named(IS_FRESH_INSTALLATION)).toInstance(isFreshInstall);
    }
}
