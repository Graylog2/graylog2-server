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
package org.graylog.storage.elasticsearch6.testing;

import com.github.joschi.jadconfig.util.Duration;
import com.google.common.collect.ImmutableList;
import io.searchbox.client.JestClient;
import org.graylog.storage.elasticsearch6.jest.JestClientProvider;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.elasticsearch.Client;
import org.graylog.testing.elasticsearch.FixtureImporter;
import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.graylog.testing.elasticsearch.TestableSearchServerInstance;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.storage.SearchVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;

import java.net.URI;

public class ElasticsearchInstanceES6 extends TestableSearchServerInstance {
    private static final Logger LOG = LoggerFactory.getLogger(SearchServerInstance.class);
    private static final String DEFAULT_IMAGE_OSS = "docker.elastic.co/elasticsearch/elasticsearch-oss";

    private final Client client;
    private final JestClient jestClient;
    private final FixtureImporter fixtureImporter;

    public ElasticsearchInstanceES6(String image, SearchVersion version, Network network) {
        super(image, version, network);
        this.jestClient = jestClientFrom();
        this.client = new ClientES6(jestClient);
        this.fixtureImporter = new FixtureImporterES6(jestClient);
    }

    @Override
    public SearchServer searchServer() {
        return SearchServer.ES6;
    }

    @Override
    public Client client() {
        return this.client;
    }

    @Override
    public FixtureImporter fixtureImporter() {
        return this.fixtureImporter;
    }

    public JestClient jestClient() {
        return this.jestClient;
    }

    public static TestableSearchServerInstance create() {
        return create(Network.newNetwork());
    }

    public static TestableSearchServerInstance create(Network network) {
        return create(SearchServer.ES6.getSearchVersion(), network);
    }

    public static TestableSearchServerInstance create(SearchVersion version, Network network) {
        final String image = imageNameFrom(version);

        LOG.debug("Creating instance {}", image);

        return new ElasticsearchInstanceES6(image, version, network);
    }

    protected static String imageNameFrom(SearchVersion version) {
        return DEFAULT_IMAGE_OSS + ":" + version.version().toString();
    }

    private JestClient jestClientFrom() {
        return new JestClientProvider(
                ImmutableList.of(URI.create("http://" + getHttpHostAddress())),
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
                null,
                null,
                new ObjectMapperProvider().get()
        ).get();
    }

    @Override
    public String getHttpHostAddress() {
        return this.container.getHost() + ":" + this.container.getMappedPort(9200);
    }
}
