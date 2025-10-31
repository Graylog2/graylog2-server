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

package org.graylog2.indexer.indices;

import org.graylog.testing.elasticsearch.ElasticsearchBaseTest;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public abstract class IndicesAdapterIT extends ElasticsearchBaseTest {

    private static final String TEST_INDEX = "test_index";
    IndicesAdapter indicesAdapter;

    @Before
    public void setup() {
        client().createIndex(TEST_INDEX);
        client().waitForGreenStatus(TEST_INDEX);
        this.indicesAdapter = searchServer().adapters().indicesAdapter();
    }

    @Test
    public void testMove() {
        String movedIndex = "moved_index";
        client().createIndex(movedIndex);
        client().waitForGreenStatus(movedIndex);
        indicesAdapter.move(TEST_INDEX, movedIndex, result -> {
            assertThat(result.hasFailedItems()).isFalse();
            assertThat(result.tookMs()).isGreaterThan(0);
            assertThat(result.movedDocuments()).isEqualTo(0);
        });
    }

    @Test
    public void testDelete() throws IOException {
        indicesAdapter.delete(TEST_INDEX);
        assertThat(indicesAdapter.exists(TEST_INDEX)).isFalse();
    }

    @Test
    public void testAliasHandling() throws IOException {
        String alias = "test_alias";
        assertThat(indicesAdapter.aliasExists(alias)).isFalse();

        Set<String> index = indicesAdapter.resolveAlias(alias);
        assertThat(index).isEmpty();

        indicesAdapter.cycleAlias(alias, TEST_INDEX);
        assertThat(indicesAdapter.aliasExists(alias)).isTrue();

        index = indicesAdapter.resolveAlias(alias);
        assertThat(index).isNotEmpty();
        assertThat(index).hasSize(1);
        assertThat(index).contains(TEST_INDEX);

        String newIndex = "new_index";

        assertThatThrownBy(() -> indicesAdapter.cycleAlias(alias, newIndex, TEST_INDEX))
                .hasMessageContaining("Couldn't switch alias " + alias + " from index " + TEST_INDEX + " to index " + newIndex);

        client().createIndex(newIndex);
        client().waitForGreenStatus(newIndex);

        indicesAdapter.cycleAlias(alias, newIndex, TEST_INDEX);
        index = indicesAdapter.resolveAlias(alias);
        assertThat(index).isNotEmpty();
        assertThat(index).hasSize(1);
        assertThat(index).contains(newIndex);

        String yetAnotherIndex = "yet_another_index";
        client().createIndex(yetAnotherIndex);
        client().waitForGreenStatus(yetAnotherIndex);

        // only for testing, this is something you would never do in real life
        indicesAdapter.cycleAlias(alias, yetAnotherIndex);
        index = indicesAdapter.resolveAlias(alias);
        assertThat(index).isNotEmpty();
        assertThat(index).hasSize(2);
        assertThat(index).containsExactlyInAnyOrder(newIndex, yetAnotherIndex);

        indicesAdapter.removeAliases(Set.of(newIndex, yetAnotherIndex), alias);
        assertThat(indicesAdapter.aliasExists(alias)).isFalse();
        index = indicesAdapter.resolveAlias(alias);
        assertThat(index).isEmpty();

        assertThatThrownBy(() -> indicesAdapter.cycleAlias(alias, "not_an_index"))
                .hasMessageContaining("Couldn't point alias test_alias to index not_an_index");

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetMapping() {
        Map<String, Object> indexMapping = indicesAdapter.getIndexMapping(TEST_INDEX);
        assertThat(indexMapping).isNotNull();
        assertThat(indexMapping.keySet()).hasSize(2);
        assertThat(indexMapping).containsKey("properties");
        assertThat(indexMapping.get("properties")).isInstanceOf(LinkedHashMap.class);
        assertThat((LinkedHashMap<String, String>) indexMapping.get("properties")).containsKey("gl2_original_timestamp");
    }

}
