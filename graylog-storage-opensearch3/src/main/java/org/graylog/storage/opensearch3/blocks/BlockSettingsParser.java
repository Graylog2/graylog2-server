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

import org.graylog.storage.opensearch3.IndicesAdapterOS;
import org.graylog2.indexer.indices.blocks.IndicesBlockStatus;
import org.opensearch.client.opensearch.indices.GetIndicesSettingsResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class BlockSettingsParser {

    static final String BLOCK_SETTINGS_PREFIX = "index.blocks.";

    public static IndicesBlockStatus parseBlockSettings(final GetIndicesSettingsResponse settingsResponse, final Optional<List<String>> indices) {
        final IndicesBlockStatus result = new IndicesBlockStatus();
        indices.orElse(settingsResponse.result().keySet().stream().toList()).forEach(index -> {
            final Map<String, Object> settings = IndicesAdapterOS.toIndexSettings(settingsResponse, index);
            if(settings != null) {
                Set<String> blockSettingsSetToTrue = settings.entrySet().stream()
                        .filter(entry -> entry.getKey().startsWith(BLOCK_SETTINGS_PREFIX))
                        .filter(entry -> {
                            Object v = entry.getValue();
                            return switch (v) {
                                case null -> false;
                                case Boolean b -> b;
                                case String val -> Boolean.parseBoolean(val);
                                default -> false;
                            };
                        })
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet());
                if (!blockSettingsSetToTrue.isEmpty()) {
                    result.addIndexBlocks(index, blockSettingsSetToTrue);
                }
            }
        });

        return result;
    }
}
