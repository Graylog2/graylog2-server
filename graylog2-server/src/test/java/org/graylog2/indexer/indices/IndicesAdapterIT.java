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

import com.github.joschi.jadconfig.util.Duration;
import org.assertj.core.api.Assertions;
import org.graylog.testing.elasticsearch.ElasticsearchBaseTest;
import org.graylog2.indexer.indices.blocks.IndicesBlockStatus;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public abstract class IndicesAdapterIT extends ElasticsearchBaseTest {

    private static final String TEST_INDEX = "test_index";
    IndicesAdapter indicesAdapter;

    @BeforeEach
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
    void testNonexistentAlias() {
        final Map<String, Set<String>> result = indicesAdapter.aliases("nonexistent_*");
        Assertions.assertThat(result).isEmpty();
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

        Map<String, Set<String>> aliases = indicesAdapter.aliases("*_index");
        assertThat(aliases).hasSize(2);
        assertThat(aliases.get(newIndex)).containsExactly(alias);
        assertThat(aliases.get(TEST_INDEX)).isEmpty();

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

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateMapping() {
        Map<String, Object> indexMapping = indicesAdapter.getIndexMapping(TEST_INDEX);
        LinkedHashMap<String, Object> properties = (LinkedHashMap<String, Object>) indexMapping.get("properties");
        String originalProperty = "gl2_original_timestamp";
        properties.put("copied_" + originalProperty, properties.get(originalProperty));
        indicesAdapter.updateIndexMapping(TEST_INDEX, "messages", indexMapping);
        Map<String, Object> updatedIndexMapping = indicesAdapter.getIndexMapping(TEST_INDEX);
        LinkedHashMap<String, Object> updatedProperties = (LinkedHashMap<String, Object>) updatedIndexMapping.get("properties");
        assertThat(updatedProperties).containsKey("copied_" + originalProperty);
        assertThat(updatedProperties.get("copied_" + originalProperty)).isEqualTo(properties.get(originalProperty));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getIndexSettings() {
        Map<String, Object> settings = indicesAdapter.getStructuredIndexSettings(TEST_INDEX);
        assertThat(settings).isNotNull();
        assertThat(settings).containsKey("index");
        assertThat(settings.get("index")).isInstanceOf(LinkedHashMap.class);
        LinkedHashMap<String, Object> indexSettings = (LinkedHashMap<String, Object>) settings.get("index");
        assertThat(indexSettings.get("version")).isInstanceOf(LinkedHashMap.class);
        assertThat((LinkedHashMap<String, Object>) indexSettings.get("version")).containsKey("created");
    }

    @Test
    public void getIndexUUID() {
        String indexId = indicesAdapter.getIndexId(TEST_INDEX);
        assertThat(indexId).isNotNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateIndex() {
        Map<String, Object> indexMapping = indicesAdapter.getIndexMapping(TEST_INDEX);
        LinkedHashMap<String, Object> properties = (LinkedHashMap<String, Object>) indexMapping.get("properties");
        String originalProperty = "gl2_original_timestamp";
        properties.put("copied_" + originalProperty, properties.get(originalProperty));
        IndexSettings settings = IndexSettings.create(1, 0, Map.of("analyzer", Map.of("default", Map.of("type", "simple"))));
        final String createdIndex = "created_index";
        indicesAdapter.create(createdIndex, settings, indexMapping);
        Map<String, Object> createdMapping = indicesAdapter.getIndexMapping(createdIndex);
        assertThat((LinkedHashMap<String, Object>) createdMapping.get("properties")).containsKey("copied_" + originalProperty);
        Map<String, Object> createdSettings = indicesAdapter.getStructuredIndexSettings(createdIndex);
        LinkedHashMap<String, Object> index = (LinkedHashMap<String, Object>) createdSettings.get("index");
        LinkedHashMap<String, Object> analysis = (LinkedHashMap<String, Object>) index.get("analysis");
        LinkedHashMap<String, Object> analyzer = (LinkedHashMap<String, Object>) analysis.get("analyzer");
        LinkedHashMap<String, Object> defaultType = (LinkedHashMap<String, Object>) analyzer.get("default");
        assertThat(defaultType).containsKey("type");
    }

    @Test
    public void testMetadata() {
        long now = System.currentTimeMillis();
        indicesAdapter.updateIndexMetaData(TEST_INDEX, Map.of("closing_date", now), true);
        Map<String, Object> indexMetaData = indicesAdapter.getIndexMetaData(TEST_INDEX);
        assertThat(indexMetaData).containsKey("closing_date");
        Optional<DateTime> closingDate = indicesAdapter.indexClosingDate(TEST_INDEX);
        assertThat(closingDate).isPresent();
        assertThat(closingDate.get()).isEqualTo(new DateTime(now, DateTimeZone.UTC));
    }

    @Test
    public void testIndexCreationDate() {
        Optional<DateTime> dateTime = indicesAdapter.indexCreationDate(TEST_INDEX);
        assertThat(dateTime).isPresent();
        assertThat(dateTime.get()).isBetween(DateTime.now(DateTimeZone.UTC).minusMinutes(1), DateTime.now(DateTimeZone.UTC).plusMinutes(1));
    }

    @Test
    public void testIndexClosingAndOpening() throws IOException {
        assertThat(indicesAdapter.closedIndices(Set.of(TEST_INDEX))).doesNotContain(TEST_INDEX);
        indicesAdapter.close(TEST_INDEX);
        assertThat(indicesAdapter.isOpen(TEST_INDEX)).isFalse();
        assertThat(indicesAdapter.isClosed(TEST_INDEX)).isTrue();
        assertThat(indicesAdapter.closedIndices(Set.of(TEST_INDEX))).contains(TEST_INDEX);
        indicesAdapter.openIndex(TEST_INDEX);
        assertThat(indicesAdapter.isOpen(TEST_INDEX)).isTrue();
        assertThat(indicesAdapter.isClosed(TEST_INDEX)).isFalse();
        assertThat(indicesAdapter.closedIndices(Set.of(TEST_INDEX))).doesNotContain(TEST_INDEX);
        indicesAdapter.markIndexReopened(TEST_INDEX);
        assertThat(indicesAdapter.aliasExists(TEST_INDEX + Indices.REOPENED_ALIAS_SUFFIX)).isTrue();
    }

    @Test
    public void testIndexExists() throws IOException {
        assertThat(indicesAdapter.exists(TEST_INDEX)).isTrue();
        assertThat(indicesAdapter.exists("idonotexist")).isFalse();
    }

    @Test
    public void testIndexOptimization() {
        indicesAdapter.optimizeIndex(TEST_INDEX, 1, Duration.seconds(60));
    }

    @Test
    public void testIndexBlocks() {
        String secondIndex = "second_index";
        client().createIndex(secondIndex);
        client().waitForGreenStatus(secondIndex);
        List<String> indices = List.of(TEST_INDEX, secondIndex);
        IndicesBlockStatus blocks = indicesAdapter.getIndicesBlocksStatus(indices);
        assertThat(blocks.getBlockedIndices()).isEmpty();
        assertThat(blocks.getIndexBlocks(TEST_INDEX)).isNull();
        indicesAdapter.setReadOnly(secondIndex);
        blocks = indicesAdapter.getIndicesBlocksStatus(indices);
        assertThat(blocks.getBlockedIndices()).hasSize(1);
        assertThat(blocks.getBlockedIndices()).containsOnly(secondIndex);
        assertThat(blocks.getIndexBlocks(secondIndex)).containsExactly("index.blocks.write");
        assertThat(blocks.getIndexBlocks(TEST_INDEX)).isNull();
        assertThat(blocks.getIndexBlocks(secondIndex)).isNotNull();
    }

}
