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
package org.graylog.testing.elasticsearch;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * Checks if a test method is using the {@link SkipDefaultIndexTemplate} annotation and exposes that information
 * with the {@link #shouldSkip()} method.
 */
public class SkipDefaultIndexTemplateWatcher extends TestWatcher {
    private boolean skipIndexTemplateCreation = false;

    @Override
    protected void starting(Description description) {
        final SkipDefaultIndexTemplate skip = description.getAnnotation(SkipDefaultIndexTemplate.class);
        this.skipIndexTemplateCreation = skip != null;
    }

    /**
     * Returns true when the currently executed test method has the {@link SkipDefaultIndexTemplate} annotation.
     *
     * @return true when the current test method has the annotation, false otherwise
     */
    public boolean shouldSkip() {
        return skipIndexTemplateCreation;
    }
}
