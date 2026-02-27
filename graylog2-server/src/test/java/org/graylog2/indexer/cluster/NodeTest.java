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
package org.graylog2.indexer.cluster;

import com.github.zafarkhaja.semver.ParseException;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.storage.SearchVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class NodeTest {

    @Mock
    private NodeAdapter nodeAdapter;

    private Node node;

    @BeforeEach
    public void setUp() throws Exception {
        this.node = new Node(nodeAdapter);
    }

    @Test
    public void returnsEmptyOptionalIfAdapterReturnsNoVersion() {
        when(nodeAdapter.version()).thenReturn(Optional.empty());

        final Optional<SearchVersion> elasticsearchVersion = node.getVersion();

        assertThat(elasticsearchVersion).isEmpty();
    }

    @Test
    public void retrievingVersionSucceedsIfElasticsearchVersionIsValid() throws Exception {
        when(nodeAdapter.version()).thenReturn(Optional.of(SearchVersion.elasticsearch("5.4.0")));

        final Optional<SearchVersion> elasticsearchVersion = node.getVersion();

        assertThat(elasticsearchVersion).contains(SearchVersion.elasticsearch(5, 4, 0));
    }

    @Disabled("TODO: fix this test or remove?")
    @Test
    public void retrievingVersionFailsIfElasticsearchVersionIsInvalid() throws Exception {
        //when(nodeAdapter.version()).thenReturn(Optional.of("Foobar"));

        assertThatThrownBy(() -> node.getVersion())
                .isInstanceOf(ElasticsearchException.class)
                .hasMessageStartingWith("Unable to parse Elasticsearch version: Foobar")
                .hasCauseInstanceOf(ParseException.class);
    }
}
