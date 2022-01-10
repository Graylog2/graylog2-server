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

package org.graylog.testing.containermatrix.annotations;

import org.graylog.testing.completebackend.DefaultMavenProjectDirProvider;
import org.graylog.testing.completebackend.DefaultPluginJarsProvider;
import org.graylog.testing.completebackend.GraylogBackendExtension;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.MavenProjectDirProvider;
import org.graylog.testing.completebackend.PluginJarsProvider;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.containermatrix.SearchServer;
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
 * This annotation can be used to create tests for a matrix of ES- and Mongo-instances
 */
@Target({TYPE})
@Retention(RUNTIME)
@Tag("integration")
@ExtendWith(GraylogBackendExtension.class)
@TestInstance(PER_CLASS)
@Testable
public @interface ContainerMatrixTestsConfiguration {
    // combination rule
    Lifecycle serverLifecycle() default Lifecycle.VM;

    // combination rule
    Class<? extends MavenProjectDirProvider> mavenProjectDirProvider() default DefaultMavenProjectDirProvider.class;

    // combination rule
    Class<? extends PluginJarsProvider> pluginJarsProvider() default DefaultPluginJarsProvider.class;

    /**
     * matrix rule
     * If no version is explicitly specified, then {@link SearchServer#DEFAULT_VERSION will be used by the tests}
     */
    SearchServer[] searchVersions() default {SearchServer.OS1, SearchServer.ES6, SearchServer.ES7};

    /**
     * matrix rule
     * If no version is explicitly specified, then {@link MongodbServer#DEFAULT_VERSION will be used by the tests}
     */
    MongodbServer[] mongoVersions() default {MongodbServer.MONGO3, MongodbServer.MONGO4};

    // additional Parameter, gets concatenated for all tests below the above rules
    int[] extraPorts() default {};

    // are run after the initialization of mongoDb, gets concatenated for all tests below the above rules
    String[] mongoDBFixtures() default {};
}
