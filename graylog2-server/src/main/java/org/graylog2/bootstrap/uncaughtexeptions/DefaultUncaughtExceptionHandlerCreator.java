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
package org.graylog2.bootstrap.uncaughtexeptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Singleton;

@Singleton
public class DefaultUncaughtExceptionHandlerCreator {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultUncaughtExceptionHandlerCreator.class);

    public DefaultUncaughtExceptionHandlerCreator() {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            if (!(e instanceof ThreadDeath)) {
                defaultHandling(t, e);
                if (e instanceof OutOfMemoryError) {
                    outOfMemoryHandling(t);
                }
            }
        });
    }

    private void outOfMemoryHandling(final Thread t) {
        LOG.error("OutOfMemoryError encountered in thread " + t.getName() + ", Graylog instance will be shut down.");
        final Runtime runtime = Runtime.getRuntime();
        LOG.info("Free JVM memory : " + runtime.freeMemory());
        LOG.info("Total JVM memory : " + runtime.totalMemory());
        LOG.info("Max JVM memory : " + runtime.maxMemory());

        System.exit(1);
    }

    private void defaultHandling(final Thread t, final Throwable e) {
        //see ThreadGrooup.uncaughtException -> we don't want to remove the well-known "printStackTrace" uncaught exception handling
        System.err.print("Exception in thread \""
                + t.getName() + "\" ");
        e.printStackTrace(System.err);
    }
}
