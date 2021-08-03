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
package org.graylog2.indexer.cluster.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum NodeRole {

    COORDINATING_ONLY('-', false),
    DATA('d', true),
    DATA_COLD('c', true),
    DATA_CONTENT('s', true),
    DATA_HOT('h', true),
    DATA_WARM('w', true),
    FROZEN('f', true),
    INGEST('i', false),
    MACHINE_LEARNING('l', false),
    MASTER_ELIGIBLE('m', false),
    REMOTE_CLUSTER_CLIENT('r', false),
    TRANSFORM('t', false),
    VOTING_ONLY('v', false);

    private static final Logger log = LoggerFactory.getLogger(NodeRole.class);

    private final int symbol;
    private final boolean holdsData;
    private static final Map<Integer, NodeRole> symbolToRole = Stream.of(NodeRole.values())
            .collect(Collectors.toMap(NodeRole::getSymbol, r -> r));

    NodeRole(int symbol, boolean holdsData) {
        this.symbol = symbol;
        this.holdsData = holdsData;
    }

    public int getSymbol() {
        return symbol;
    }

    public boolean holdsData() {
        return holdsData;
    }

    public static EnumSet<NodeRole> parseSymbolString(String symbols) {
        final EnumSet<NodeRole> roles = symbols.chars().boxed()
                .map(symbol -> {
                    final NodeRole role = symbolToRole.get(symbol);
                    if (role == null) {
                        log.warn("Unknown ES node role <{}>.", (char) symbol.intValue());
                    }
                    return role;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(NodeRole.class)));
        log.debug("Parsed node roles <{}> out of symbol string <{}>.", roles, symbols);

        return roles;
    }
}
