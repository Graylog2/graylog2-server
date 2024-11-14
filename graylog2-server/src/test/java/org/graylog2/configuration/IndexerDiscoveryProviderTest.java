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
package org.graylog2.configuration;

import com.github.joschi.jadconfig.util.Duration;
import org.assertj.core.api.Assertions;
import org.graylog2.bootstrap.preflight.GraylogCertificateProvisioner;
import org.graylog2.bootstrap.preflight.PreflightConfigResult;
import org.graylog2.bootstrap.preflight.PreflightConfigService;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodeStatus;
import org.graylog2.cluster.nodes.NodeService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.mockito.Mockito.when;

class IndexerDiscoveryProviderTest {


    @Test
    void testAutomaticDiscovery() {
        final IndexerDiscoveryProvider provider = new IndexerDiscoveryProvider(
                Collections.emptyList(),
                1,
                Duration.seconds(1),
                preflightConfig(PreflightConfigResult.FINISHED),
                nodes("http://localhost:9200", "http://other:9201"),
                Collections.emptySet()
        );

        Assertions.assertThat(provider.get())
                .hasSize(2)
                .extracting(URI::toString)
                .contains("http://localhost:9200", "http://other:9201");
    }

    @Test
    void testListeners() {
        final IndexerDiscoveryListener listener1 = Mockito.mock(IndexerDiscoveryListener.class);
        final IndexerDiscoveryListener listener2 = Mockito.mock(IndexerDiscoveryListener.class);

        final IndexerDiscoveryProvider provider = new IndexerDiscoveryProvider(
                Collections.emptyList(),
                10,
                Duration.milliseconds(1),
                preflightConfig(PreflightConfigResult.FINISHED),
                nodes(),
                new HashSet<>(List.of(listener1, listener2))
        );

        Assertions.assertThatThrownBy(provider::get)
                .hasMessageContaining("Unable to retrieve Datanode connection");

        Mockito.verify(listener1, Mockito.times(1)).beforeIndexerDiscovery();
        Mockito.verify(listener1, Mockito.times(10)).onDiscoveryRetry();
        Mockito.verify(listener2, Mockito.times(1)).beforeIndexerDiscovery();
        Mockito.verify(listener2, Mockito.times(10)).onDiscoveryRetry();

    }

    @Test
    void testAutomaticDiscoveryOneUnconfigured() {
        final IndexerDiscoveryProvider provider = new IndexerDiscoveryProvider(
                Collections.emptyList(),
                1,
                Duration.seconds(1),
                preflightConfig(PreflightConfigResult.FINISHED),
                nodes("http://localhost:9200", ""), // the second node is not configured yet, has no transport address
                Collections.emptySet()
        );

        Assertions.assertThat(provider.get())
                .hasSize(1)
                .extracting(URI::toString)
                .contains("http://localhost:9200");
    }


    @Test
    void testPreconfiguredIndexers() {
        final IndexerDiscoveryProvider provider = new IndexerDiscoveryProvider(
                List.of(URI.create("http://my-host:9200")),
                1,
                Duration.seconds(1),
                preflightConfig(null),
                nodes(),
                Collections.emptySet()
        );

        Assertions.assertThat(provider.get())
                .hasSize(1)
                .extracting(URI::toString)
                .contains("http://my-host:9200");
    }

    @Test
    void testSkippedConfigWithDefaultIndexer() {
        final IndexerDiscoveryProvider provider = new IndexerDiscoveryProvider(
                Collections.emptyList(),
                1,
                Duration.seconds(1),
                preflightConfig(PreflightConfigResult.SKIPPED),
                nodes(),
                Collections.emptySet()
        );

        Assertions.assertThat(provider.get())
                .hasSize(1)
                .extracting(URI::toString)
                .contains("http://127.0.0.1:9200");
    }

    @Test
    void testFailedAutodiscovery() {
        final IndexerDiscoveryProvider provider = new IndexerDiscoveryProvider(
                Collections.emptyList(), // no configured indexers
                1,
                Duration.seconds(1),
                preflightConfig(PreflightConfigResult.FINISHED), // preflight correctly finished
                nodes(), // but still no nodes discovered
                Collections.emptySet()
        );

        Assertions.assertThatThrownBy(provider::get)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Unable to retrieve Datanode connection");
    }

    @Test
    void testProvisioningWillBeTriggered() {
        final GraylogCertificateProvisioner provisioner = Mockito.mock(GraylogCertificateProvisioner.class);
        final org.graylog2.configuration.IndexerDiscoveryListener indexerDiscoveryListener = new IndexerDiscoveryListener() {

            @Override
            public void beforeIndexerDiscovery() {

            }

            @Override
            public void onDiscoveryRetry() {
                provisioner.runProvisioning();
            }
        };

        final IndexerDiscoveryProvider provider = new IndexerDiscoveryProvider(
                Collections.emptyList(),
                10,
                Duration.milliseconds(1),
                preflightConfig(PreflightConfigResult.FINISHED),
                nodes(),
                Collections.singleton(indexerDiscoveryListener)
        );

        Assertions.assertThatThrownBy(provider::get)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Unable to retrieve Datanode connection");

        Mockito.verify(provisioner, Mockito.times(10)).runProvisioning();

    }

    private NodeService<DataNodeDto> nodes(String... transportAddress) {

        final NodeService<DataNodeDto> service = Mockito.mock(NodeService.class);
        final Map<String, DataNodeDto> map = Arrays.stream(transportAddress)
                .map(address -> DataNodeDto.Builder.builder()
                        .setId(UUID.randomUUID().toString())
                        .setLeader(false)
                        .setTransportAddress(address)
                        .setHostname("localhost")
                        .setDataNodeStatus(DataNodeStatus.AVAILABLE)
                        .build())
                .collect(Collectors.toMap(
                        DataNodeDto::getId,
                        obj -> obj
                ));

        when(service.allActive()).thenReturn(map);
        return service;
    }

    private PreflightConfigService preflightConfig(@Nullable PreflightConfigResult result) {
        final DummyPreflightConfigService service = new DummyPreflightConfigService();
        service.setConfigResult(result);
        return service;
    }
}
