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

import org.apache.commons.lang3.NotImplementedException;
import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.testcontainers.containers.Network;

import java.lang.reflect.Constructor;

public class RunningGraylogBackend implements GraylogBackend {
    private final SearchServerInstance searchServerInstance;

    public RunningGraylogBackend() {
        try {
            Class<?> clazz = Class.forName("org.graylog.storage.elasticsearch7.testing.RunningElasticsearchInstanceES7");
            Constructor constructor = clazz.getConstructor();
            this.searchServerInstance = (SearchServerInstance) constructor.newInstance();
        } catch (Exception ex) {
            throw new NotImplementedException("Could not create Search instance.", ex);
        }
    }

    public static GraylogBackend createStarted() {
        return new RunningGraylogBackend();
    }

    @Override
    public String uri() {
        return "http://localhost";
    }

    @Override
    public int apiPort() {
        return 9000;
    }

    @Override
    public SearchServerInstance searchServerInstance() {
        return this.searchServerInstance;
    }

    @Override
    public int mappedPortFor(int originalPort) {
        return originalPort;
    }

    @Override
    public void importMongoDBFixture(String resourcePath, Class<?> testClass) {
        throw new NotImplementedException("Feature needs implementation...");
    }

    @Override
    public void importElasticsearchFixture(String resourcePath, Class<?> testClass) {
        this.searchServerInstance.importFixtureResource(resourcePath, testClass);
    }

    @Override
    public Network network() {
        throw new NotImplementedException("Feature not implemented on Running backends (no container)");
    }

    @Override
    public String getLogs() {
        return "noop -> because the server is running, check the logs in the console ;-)";
    }
}
