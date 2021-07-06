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
import com.github.zafarkhaja.semver.Version;
import com.google.common.collect.ImmutableList;
import io.searchbox.client.JestClient;
import org.graylog.storage.elasticsearch6.jest.JestClientProvider;
import org.graylog.testing.elasticsearch.Client;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.graylog.testing.elasticsearch.FixtureImporter;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.net.URI;

public class ElasticsearchInstanceES6 extends ElasticsearchInstance {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchInstance.class);
    private static final String ES_VERSION = "6.8.4";

    private final Client client;
    private final JestClient jestClient;
    private final FixtureImporter fixtureImporter;

    public ElasticsearchInstanceES6(String image, Version version, Network network) {
        super(image, version, network);
        this.jestClient = jestClientFrom(this.container);
        this.client = new ClientES6(jestClient);
        this.fixtureImporter = new FixtureImporterES6(jestClient);
    }

    @Override
    protected Client client() {
        return this.client;
    }

    @Override
    protected FixtureImporter fixtureImporter() {
        return this.fixtureImporter;
    }

    public JestClient jestClient() {
        return this.jestClient;
    }

    public static ElasticsearchInstance create() {
        return create(Network.newNetwork());
    }

    public static ElasticsearchInstance create(Network network) {
        return create(ES_VERSION, network);
    }

    public static ElasticsearchInstance create(String versionString, Network network) {
        final Version version = Version.valueOf(versionString);
        final String image = imageNameFrom(version);

        LOG.debug("Creating instance {}", image);

        return new ElasticsearchInstanceES6(image, version, network);
    }

    private JestClient jestClientFrom(ElasticsearchContainer container) {
        return new JestClientProvider(
                ImmutableList.of(URI.create("http://" + container.getHttpHostAddress())),
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
}
