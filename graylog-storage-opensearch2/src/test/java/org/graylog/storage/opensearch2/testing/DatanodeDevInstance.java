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

import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.datanode.DatanodeDevContainerInstanceProvider;
import org.graylog2.storage.SearchVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.util.List;

public class DatanodeDevInstance extends OpenSearchInstance {
    private static final Logger LOG = LoggerFactory.getLogger(DatanodeDevInstance.class);
    public static final SearchServer DATANODE_VERSION = SearchServer.DATANODE_DEV;
    private final String mongoDBUri;
    private final String passwordSecret;
    private final String rootPasswordSha2;

    public DatanodeDevInstance(final SearchVersion version, final Network network, final String mongoDBUri, final String passwordSecret, final String rootPasswordSha2, final String heapSize, final List<String> featureFlags) {
        super(version, network, heapSize, featureFlags);
        this.mongoDBUri = mongoDBUri;
        this.passwordSecret = passwordSecret;
        this.rootPasswordSha2 = rootPasswordSha2;
    }

    @Override
    protected String imageName() {
        return "creating image locally by provider";
    }

    @Override
    public SearchServer searchServer() {
        return DATANODE_VERSION;
    }

    @Override
    public GenericContainer<?> buildContainer(String image, Network network) {
        var builder =  DatanodeDevContainerInstanceProvider.getBuilderFor(this.version()).orElseThrow(() -> new UnsupportedOperationException("Can not build container for Search version " + this.version() + " - not supported."));

        builder.passwordSecret(passwordSecret).rootPasswordSha2(rootPasswordSha2).network(network).mongoDbUri(mongoDBUri).restPort(8999).openSearchPort(9200);

        return builder.build();
/*
        return new GenericContainer<>(DockerImageName.parse(image))
                .withImagePullPolicy(PullPolicy.alwaysPull())
                // Avoids reuse warning on Jenkins (we don't want reuse in our CI environment)
                .withReuse(isNull(System.getenv("BUILD_ID")))
                .withEnv("OPENSEARCH_JAVA_OPTS", getEsJavaOpts())
                .withEnv("GRAYLOG_DATANODE_PASSWORD_SECRET", passwordSecret)
                .withEnv("GRAYLOG_DATANODE_ROOT_PASSWORD_SHA2", rootPasswordSha2)
                .withEnv("GRAYLOG_DATANODE_MONGODB_URI", mongoDBUri)
                .withEnv("GRAYLOG_DATANODE_SINGLE_NODE_ONLY", "true")
                .withEnv("GRAYLOG_DATANODE_INSECURE_STARTUP", "true")
                .withExposedPorts(8999, 9200, 9300)
                .withNetwork(network)
                .withNetworkAliases(NETWORK_ALIAS)
                .waitingFor(
                        Wait.forHttp("/_cluster/health")
                                .forPort(OPENSEARCH_PORT)
                                .forStatusCode(200)
                                .forResponsePredicate(s -> {
                                    LOG.info("Response while waiting: {}", s);
                                    // allow yellow for fixing indices later
                                    return s.contains("\"status\":\"green\"") || s.contains("\"status\":\"yellow\"");
                                })
                                .withStartupTimeout(java.time.Duration.ofSeconds(180))
                ); */
    }
}
