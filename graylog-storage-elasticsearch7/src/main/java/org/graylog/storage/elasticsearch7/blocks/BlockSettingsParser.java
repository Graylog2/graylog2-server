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
package org.graylog.storage.elasticsearch7.blocks;

import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.collect.ImmutableOpenMap;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.settings.Settings;
import org.graylog2.indexer.indices.blocks.IndicesBlockStatus;

import java.util.Set;
import java.util.stream.Collectors;

public class BlockSettingsParser {

    static final String BLOCK_SETTINGS_PREFIX = "index.blocks.";

    public static IndicesBlockStatus parseBlockSettings(final GetSettingsResponse settingsResponse) {
        IndicesBlockStatus result = new IndicesBlockStatus();
        final ImmutableOpenMap<String, Settings> indexToSettingsMap = settingsResponse.getIndexToSettings();
        final String[] indicesInResponse = indexToSettingsMap.keys().toArray(String.class);
        for (String index : indicesInResponse) {
            final Settings blockSettings = indexToSettingsMap.get(index).getByPrefix(BLOCK_SETTINGS_PREFIX);

            if (!blockSettings.isEmpty()) {
                final Set<String> blockSettingsNames = blockSettings.names();
                final Set<String> blockSettingsSetToTrue = blockSettingsNames.stream()
                        .filter(s -> blockSettings.getAsBoolean(s, false))
                        .map(s -> BLOCK_SETTINGS_PREFIX + s)
                        .collect(Collectors.toSet());
                if (!blockSettingsSetToTrue.isEmpty()) {
                    result.addIndexBlocks(index, blockSettingsSetToTrue);
                }
            }
        }

        return result;
    }
}
