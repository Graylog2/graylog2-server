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

package org.graylog.testing.completebackend;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.annotation.Testable;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

/**
 * Use this to run a test with a configured Graylog backend extension.
 */
@Target({TYPE})
@Retention(RUNTIME)
@Tag("integration")
@ExtendWith(GraylogBackendExtension.class)
@TestInstance(PER_CLASS)
@Testable
public @interface GraylogBackendConfiguration {
    @interface Env {
        /**
         * The upper-case key of the environment variable to set.
         */
        String key();

        /**
         * The value to set the environment variable to.
         */
        String value();
    }

    /**
     * Defines environment variables to set in the Graylog and Data Node containers. Note: can't be used with
     * Lifecycle.VM, as the container is reused between tests.
     */
    Env[] env() default {};

    /**
     * Defines the lifecycle of the Graylog server container.
     */
    Lifecycle serverLifecycle() default Lifecycle.VM;

    Class<? extends GraylogServerProduct> serverProduct() default OpenServerProduct.class;

    Class<? extends GraylogDataNodeProduct> datanodeProduct() default OpenDataNodeProduct.class;

    Class<? extends MavenProjectDirProvider> mavenProjectDirProvider() default DefaultMavenProjectDirProvider.class;

    Class<? extends PluginJarsProvider> pluginJarsProvider() default DefaultPluginJarsProvider.class;

    /**
     * A list of Graylog Feature Flags that should be enabled for this test. Note: can't be used with
     * Lifecycle.VM, as the container is reused between tests.
     */
    String[] enabledFeatureFlags() default {};

    /**
     * Automatically import licenses on server startup. Note: can't be used with Lifecycle.VM.
     */
    boolean importLicenses() default GraylogBackendExtension.IMPORT_LICENSES_DEFAULT;

    Class<? extends PluginJarsProvider> datanodePluginJarsProvider() default NoPluginJarsProvider.class;
}
