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
package org.graylog.storage.opensearch2.testing;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

public class OpenSearchContainer extends GenericContainer<OpenSearchContainer> {
    public OpenSearchContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        this.logger().info("Starting an OpenSearch container using [{}]", dockerImageName);
        this.withNetworkAliases("opensearch-" + Base58.randomString(6));
        this.withEnv("discovery.type", "single-node");
        this.addExposedPorts(9200, 9300);
        this.setWaitStrategy((new HttpWaitStrategy()).forPort(9200).forStatusCodeMatching((response) -> response == 200 || response == 401).withStartupTimeout(Duration.ofMinutes(2L)));
    }
}
