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
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This annotation can be used to extend JUnit 5 test classes or methods with {@link GraylogBackendExtension},
 * which manages instantiating the backend and allows access to necessary settings like the URI necessary
 * to reach the API.
 */
@Target({TYPE, METHOD})
@ExtendWith(GraylogBackendExtension.class)
@Retention(RUNTIME)
@Tag("integration")
public @interface ApiIntegrationTest {
    Lifecycle serverLifecycle() default Lifecycle.METHOD;

    int[] extraPorts() default {};

    Class<? extends ElasticsearchInstanceFactory> elasticsearchFactory();

    Class<? extends PluginJarsProvider> pluginJarsProvider() default DefaultPluginJarsProvider.class;

    Class<? extends MavenProjectDirProvider> mavenProjectDirProvider() default DefaultMavenProjectDirProvider.class;
}
