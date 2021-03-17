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
package org.graylog.storage.elasticsearch7.testing;

import com.github.joschi.jadconfig.util.Duration;
import com.github.zafarkhaja.semver.Version;
import com.google.common.collect.ImmutableList;
import org.graylog.shaded.elasticsearch7.org.apache.http.impl.client.BasicCredentialsProvider;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RestHighLevelClient;
import org.graylog.storage.elasticsearch7.ElasticsearchClient;
import org.graylog.storage.elasticsearch7.RestHighLevelClientProvider;
import org.graylog.testing.elasticsearch.Client;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.graylog.testing.elasticsearch.FixtureImporter;
import org.graylog2.system.shutdown.GracefulShutdownService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;

import java.net.URI;

public class ElasticsearchInstanceES7 extends ElasticsearchInstance {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchInstanceES7.class);
    private static final String DEFAULT_VERSION = "7.8.0";

    private final RestHighLevelClient restHighLevelClient;
    private final ElasticsearchClient elasticsearchClient;
    private final Client client;
    private final FixtureImporter fixtureImporter;

    protected ElasticsearchInstanceES7(String image, Version version, Network network) {
        super(image, version, network);
        this.restHighLevelClient = buildRestClient();
        this.elasticsearchClient = new ElasticsearchClient(this.restHighLevelClient, false);
        this.client = new ClientES7(this.elasticsearchClient);
        this.fixtureImporter = new FixtureImporterES7(this.elasticsearchClient);
    }

    private RestHighLevelClient buildRestClient() {
        return new RestHighLevelClientProvider(
                new GracefulShutdownService(),
                ImmutableList.of(URI.create("http://" + this.container.getHttpHostAddress())),
                Duration.seconds(60),
                Duration.seconds(60),
                Duration.seconds(60),
                1,
                1,
                1,
                false,
                null,
                Duration.seconds(60),
                "http",
                false,
                false,
                new BasicCredentialsProvider())
                .get();
    }

    public static ElasticsearchInstanceES7 create() {
        return create(Network.newNetwork());
    }

    public static ElasticsearchInstanceES7 create(Network network) {
        return create(DEFAULT_VERSION, network);
    }

    public static ElasticsearchInstanceES7 create(String versionString, Network network) {
        final Version version = Version.valueOf(versionString);
        final String image = imageNameFrom(version);

        LOG.debug("Creating instance {}", image);

        return new ElasticsearchInstanceES7(image, version, network);
    }

    @Override
    protected Client client() {
        return this.client;
    }

    @Override
    protected FixtureImporter fixtureImporter() {
        return this.fixtureImporter;
    }

    public ElasticsearchClient elasticsearchClient() {
        return this.elasticsearchClient;
    }

    public RestHighLevelClient restHighLevelClient() {
        return this.restHighLevelClient;
    }
}
