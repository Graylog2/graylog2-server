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
package org.graylog.storage.elasticsearch6.blocks;

import com.fasterxml.jackson.databind.JsonNode;
import io.searchbox.client.JestResult;
import org.graylog2.indexer.indices.blocks.IndicesBlockStatus;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class JestBlockSettingsParser {

    static final Collection<String> POSSIBLE_BLOCKS = Arrays.asList("read_only", "read_only_allow_delete", "read", "write", "metadata");
    static final String BLOCK_SETTINGS_PREFIX = "index.blocks.";

    public static IndicesBlockStatus parseBlockSettings(final JestResult jestResult, final List<String> indices) {
        final IndicesBlockStatus indicesBlockStatus = new IndicesBlockStatus();
        if (indices == null || indices.isEmpty() || jestResult == null) {
            return indicesBlockStatus;
        }
        final JsonNode jsonObject = jestResult.getJsonObject();
        if (jsonObject != null && !jsonObject.isMissingNode()) {
            for (String indexName : indices) {
                Collection<String> blocks = new LinkedList<>();
                for (String possibleBlock : POSSIBLE_BLOCKS) {
                    final JsonNode blockElem = jsonObject.path(indexName).path("settings").path("index").path("blocks").path(possibleBlock);
                    if (!blockElem.isMissingNode()) {
                        if ((blockElem.isBoolean() && blockElem.asBoolean()) ||
                                (blockElem.isTextual() && blockElem.asText().contains("true"))
                        ) {
                            blocks.add(BLOCK_SETTINGS_PREFIX + possibleBlock);
                        }
                    }
                }
                if (!blocks.isEmpty()) {
                    indicesBlockStatus.addIndexBlocks(indexName, blocks);
                }
            }
        }
        return indicesBlockStatus;
    }
}
