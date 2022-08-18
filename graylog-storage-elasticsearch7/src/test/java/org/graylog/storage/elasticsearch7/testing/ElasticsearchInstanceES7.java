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
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.elasticsearch.Client;
import org.graylog.testing.elasticsearch.FixtureImporter;
import org.graylog.testing.elasticsearch.TestableSearchServerInstance;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.graylog2.storage.SearchVersion;
import org.graylog2.system.shutdown.GracefulShutdownService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;

import java.net.URI;

public class ElasticsearchInstanceES7 extends TestableSearchServerInstance {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchInstanceES7.class);
    private static final String ES_VERSION = "7.10.2";
    private static final String DEFAULT_IMAGE_OSS = "docker.elastic.co/elasticsearch/elasticsearch-oss";

    private final RestHighLevelClient restHighLevelClient;
    private final ElasticsearchClient elasticsearchClient;
    private final Client client;
    private final FixtureImporter fixtureImporter;

    protected ElasticsearchInstanceES7(String image, SearchVersion version, Network network) {
        super(image, version, network);
        this.restHighLevelClient = buildRestClient();
        this.elasticsearchClient = new ElasticsearchClient(this.restHighLevelClient, false, new ObjectMapperProvider().get());
        this.client = new ClientES7(this.elasticsearchClient);
        this.fixtureImporter = new FixtureImporterES7(this.elasticsearchClient);
    }

    @Override
    public SearchServer searchServer() {
        return SearchServer.ES7;
    }

    private RestHighLevelClient buildRestClient() {
        return new RestHighLevelClientProvider(
                new GracefulShutdownService(),
                ImmutableList.of(URI.create("http://" + this.getHttpHostAddress())),
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
        return create(SearchVersion.elasticsearch(ES_VERSION), network);
    }

    public static ElasticsearchInstanceES7 create(SearchVersion searchVersion, Network network) {
        final String image = imageNameFrom(searchVersion.version());

        LOG.debug("Creating instance {}", image);

        return new ElasticsearchInstanceES7(image, searchVersion, network);
    }


    protected static String imageNameFrom(Version version) {
        return DEFAULT_IMAGE_OSS + ":" + version.toString();
    }

    @Override
    public Client client() {
        return this.client;
    }

    @Override
    public FixtureImporter fixtureImporter() {
        return this.fixtureImporter;
    }

    public ElasticsearchClient elasticsearchClient() {
        return this.elasticsearchClient;
    }

    public RestHighLevelClient restHighLevelClient() {
        return this.restHighLevelClient;
    }
}
