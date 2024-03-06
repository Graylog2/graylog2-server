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
package org.graylog.plugins.pipelineprocessor.rulebuilder;

import com.fasterxml.jackson.annotation.JsonValue;
import org.graylog2.plugin.Message;

public enum RuleBuilderFunctionGroup {

    MESSAGE("Message Functions", 0),
    STRING("String Functions", 10),
    NUMBER("Number Functions", 20),
    BOOLEAN("Boolean Functions", 22),
    CONVERSION("Conversion Functions", 25),
    DATE("Date Functions", 30),
    PATTERN("Pattern Matching Functions", 35),
    LOOKUP("Lookup Table Functions", 37),
    ENCODING("Encoding/Decoding Functions", 38),
    HASH("Hash Functions", 39),
    SYSLOG("Syslog Functions", 40),
    EXTRACTORS("Extractor Functions", 50),
    ARRAY("Array Functions", 60),
    WATCHLIST("Watchlist Functions", 70),
    ASSET("Asset Functions", 80),
    THREATINTEL("Threat Intelligence Functions", 90),
    OTHER("Other", 999);

    private String name;
    private int sort;

    RuleBuilderFunctionGroup(String name, int sort) {
        this.name = name;
        this.sort = sort;
    }

    public static <T> RuleBuilderFunctionGroup map(Class<? extends T> primaryParam) {
        if (primaryParam == String.class) {
            return STRING;
        } else if (primaryParam == Message.class) {
            return MESSAGE;
        }
        return OTHER;
    }

    @JsonValue
    public String getName() {
        return name;
    }
}
