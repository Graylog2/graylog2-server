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
package org.graylog.plugins.threatintel.adapters.tor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TorExitNodeListParser {
    public Map<String, List<String>> parse(String list) {
        if (list == null) {
            return Collections.emptyMap();
        }
        final Map<String, List<String>> result = new HashMap<>();
        String exitNodeId = null;
        for (String line : list.split("\n")) {
            if (line.startsWith("ExitNode")) {
                final String elements[] = line.split("\\s+");
                if (elements.length == 2) {
                    exitNodeId = elements[1];
                }
            }
            if (line.startsWith("ExitAddress")) {
                final String elements[] = line.split("\\s+");
                if (elements.length >= 2 && exitNodeId != null) {
                    final String ip = elements[1];
                    if (!result.containsKey(ip)) {
                        result.put(ip, new ArrayList<>());
                    }
                    result.get(ip).add(exitNodeId);
                }
            }
        }
        return result;
    }
}
