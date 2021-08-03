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

    COLD('c'),
    DATA('d'),
    FROZEN('f'),
    HOT('h'),
    INGEST('i'),
    MACHINE_LEARNING('l'),
    MASTER_ELIGIBLE('m'),
    REMOTE_CLUSTER_CLIENT('r'),
    CONTENT('s'),
    TRANSFORM('t'),
    VOTING_ONLY('v'),
    WARM('w'),
    COORDINATING_ONLY('-');

    private static final Logger log = LoggerFactory.getLogger(NodeRole.class);

    private final int symbol;

    private static final Map<Integer, NodeRole> symbolToRole = Stream.of(NodeRole.values())
            .collect(Collectors.toMap(NodeRole::getSymbol, r -> r));

    NodeRole(int symbol) {
        this.symbol = symbol;
    }

    public int getSymbol() {
        return symbol;
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
