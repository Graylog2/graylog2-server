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
package org.graylog.storage.opensearch3.blocks;

import org.graylog2.indexer.indices.blocks.IndicesBlockStatus;
import org.junit.jupiter.api.Test;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.indices.GetIndicesSettingsResponse;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.opensearch.indices.IndexState;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BlockSettingsParserTest {

    @Test
    public void noBlockedIndicesIdentifiedIfEmptyResponseParsed() {
        GetIndicesSettingsResponse emptyResponse = GetIndicesSettingsResponse.of(b -> b);
        final IndicesBlockStatus indicesBlockStatus = BlockSettingsParser.parseBlockSettings(emptyResponse, Optional.empty());
        assertNotNull(indicesBlockStatus);
        assertEquals(0, indicesBlockStatus.countBlockedIndices());
    }

    @Test
    public void noBlockedIndicesIdentifiedIfEmptySettingsPresent() {
        GetIndicesSettingsResponse emptySettingsResponse = GetIndicesSettingsResponse.of(b -> b
                .result(Map.of("index_0", IndexState.builder().settings(IndexSettings.of(s -> s)).build()))
        );
        final IndicesBlockStatus indicesBlockStatus = BlockSettingsParser.parseBlockSettings(emptySettingsResponse, Optional.empty());
        assertNotNull(indicesBlockStatus);
        assertEquals(0, indicesBlockStatus.countBlockedIndices());
    }

    @Test
    public void parserProperlyResponseWithMultipleIndicesWithDifferentBlockSettings() {
        Map<String, IndexState> settingsBuilder = Map.of(
                "index_with_no_block_settings", IndexState.of(b -> b.settings(IndexSettings.of(s -> s.customSettings(Map.of("lala", JsonData.of(42)))))),
                "index_with_false_block_setting", IndexState.of(b -> b.settings(IndexSettings.of(s -> s.blocksReadOnly(false)))),
                "index_with_true_block_setting", IndexState.of(b -> b.settings(IndexSettings.of(s -> s.blocksReadOnly(true)))),
                "index_with_multiple_true_block_settings", IndexState.of(b -> b.settings(IndexSettings.of(s -> s
                        .blocksReadOnly(true)
                        .blocksReadOnlyAllowDelete(true)
                ))),
                "index_with_mixed_block_settings", IndexState.of(b -> b.settings(IndexSettings.of(s -> s
                        .blocksReadOnly(false)
                        .blocksReadOnlyAllowDelete(true)))));
        GetIndicesSettingsResponse settingsResponse = GetIndicesSettingsResponse.of(b -> b
                .result(settingsBuilder)
        );
        final IndicesBlockStatus indicesBlockStatus = BlockSettingsParser.parseBlockSettings(settingsResponse, Optional.empty());
        assertNotNull(indicesBlockStatus);
        assertEquals(3, indicesBlockStatus.countBlockedIndices());
        final Set<String> blockedIndices = indicesBlockStatus.getBlockedIndices();

        assertFalse(blockedIndices.contains("index_with_no_block_settings"));
        assertFalse(blockedIndices.contains("index_with_false_block_setting"));

        assertTrue(blockedIndices.contains("index_with_true_block_setting"));
        Collection<String> indexBlocks = indicesBlockStatus.getIndexBlocks("index_with_true_block_setting");
        assertEquals(1, indexBlocks.size());
        assertTrue(indexBlocks.contains("index.blocks.read_only"));

        assertTrue(blockedIndices.contains("index_with_multiple_true_block_settings"));
        indexBlocks = indicesBlockStatus.getIndexBlocks("index_with_multiple_true_block_settings");
        assertEquals(2, indexBlocks.size());
        assertTrue(indexBlocks.contains("index.blocks.read_only"));
        assertTrue(indexBlocks.contains("index.blocks.read_only_allow_delete"));

        assertTrue(blockedIndices.contains("index_with_mixed_block_settings"));
        indexBlocks = indicesBlockStatus.getIndexBlocks("index_with_mixed_block_settings");
        assertEquals(1, indexBlocks.size());
        assertFalse(indexBlocks.contains("index.blocks.read_only"));
        assertTrue(indexBlocks.contains("index.blocks.read_only_allow_delete"));


    }


}
