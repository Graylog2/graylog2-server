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
package org.graylog2.junit.extensions;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.filter.CompositeFilter;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Iterator;
import java.util.Optional;

public class IgnoreDeceptiveExceptionExtension implements BeforeEachCallback, AfterEachCallback {
    private Optional<IgnoreDeceptiveExceptionsByRegexFilter> getFilter() {
        final LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        final ConsoleAppender appender = loggerContext.getConfiguration().getAppender("STDOUT");
        final Filter rootFilter = appender.getFilter();

        if (rootFilter instanceof IgnoreDeceptiveExceptionsByRegexFilter) {
            final IgnoreDeceptiveExceptionsByRegexFilter filter = (IgnoreDeceptiveExceptionsByRegexFilter)rootFilter;
            return Optional.of(filter);
        }
        return Optional.empty();
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if(context.getTestMethod().isPresent()
                && context.getTestMethod().get().isAnnotationPresent(IgnoreDeceptiveExceptionsAnnotation.class)) {
            final String className = context.getTestMethod().get().getAnnotation(IgnoreDeceptiveExceptionsAnnotation.class).clazz().getName();
            Configurator.setLevel(className, Level.OFF);
        } else if(context.getTestMethod().isPresent()
                && context.getTestMethod().get().isAnnotationPresent(IgnoreDeceptiveExceptionsByRegexAnnotation.class)) {
            final String regex = context.getTestMethod().get().getAnnotation(IgnoreDeceptiveExceptionsByRegexAnnotation.class).regex();
            this.getFilter().ifPresent(filter -> filter.setRegex(regex));
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if(context.getTestMethod().isPresent()
                && context.getTestMethod().get().isAnnotationPresent(IgnoreDeceptiveExceptionsAnnotation.class)) {
            final String className = context.getTestMethod().get().getAnnotation(IgnoreDeceptiveExceptionsAnnotation.class).clazz().getName();
            Configurator.setLevel(className, Level.INFO);
        } else if(context.getTestMethod().isPresent()
                && context.getTestMethod().get().isAnnotationPresent(IgnoreDeceptiveExceptionsByRegexAnnotation.class)) {
            this.getFilter().ifPresent(filter -> filter.unsetRegex());
        }
    }
}
