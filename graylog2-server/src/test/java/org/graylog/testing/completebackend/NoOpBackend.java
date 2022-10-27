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

import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.testcontainers.containers.Network;

import java.util.Optional;

public class NoOpBackend implements GraylogBackend {
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
        return null;
    }

    @Override
    public int mappedPortFor(int originalPort) {
        return 9200;
    }

    @Override
    public void importMongoDBFixture(String resourcePath, Class<?> testClass) {

    }

    @Override
    public void importElasticsearchFixture(String resourcePath, Class<?> testClass) {

    }

    @Override
    public Network network() {
        return null;
    }

    @Override
    public String getLogs() {
        return "no logs in NoOp";
    }

    @Override
    public Optional<MailServerInstance> getEmailServerInstance() {
        return Optional.empty();
    }
}
