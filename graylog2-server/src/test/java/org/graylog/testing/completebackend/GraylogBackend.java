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

import io.restassured.RestAssured;
import io.restassured.config.FailureConfig;
import io.restassured.config.RestAssuredConfig;
import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.testcontainers.containers.Network;

import java.util.Optional;

public interface GraylogBackend {
    String uri();

    int apiPort();

    SearchServerInstance searchServerInstance();

    int mappedPortFor(int originalPort);

    void importMongoDBFixture(String resourcePath, Class<?> testClass);

    void importElasticsearchFixture(String resourcePath, Class<?> testClass);

    Network network();

    String getLogs();

    Optional<MailServerInstance> getEmailServerInstance();

    default RestAssuredConfig withGraylogBackendFailureConfig() {
        return RestAssured.config().failureConfig(FailureConfig.failureConfig().with().failureListeners(
                (reqSpec, respSpec, resp) -> {
                    if (resp.statusCode() >= 500) {
                        System.out.println("------------------------ Output from graylog docker container start ------------------------");
                        System.out.println(this.getLogs());
                        System.out.println("------------------------ Output from graylog docker container ends  ------------------------");
                    }
                })
        );
    }
}
