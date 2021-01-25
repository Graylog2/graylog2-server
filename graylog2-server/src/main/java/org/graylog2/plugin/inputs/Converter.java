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
package org.graylog2.plugin.inputs;

import java.util.Map;

public abstract class Converter {

    public enum Type {
        NUMERIC,
        DATE,
        HASH,
        SPLIT_AND_COUNT,
        SYSLOG_PRI_LEVEL,
        SYSLOG_PRI_FACILITY,
        TOKENIZER,
        IP_ANONYMIZER,
        CSV,
        LOWERCASE,
        UPPERCASE,
        FLEXDATE,
        LOOKUP_TABLE
    }

    private final Type type;
    private final Map<String, Object> config;

    public Converter(Type type, Map<String, Object> config) {
        this.type = type;
        this.config = config;
    }

    public Type getType() {
        return type;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public abstract Object convert(String value);
    public abstract boolean buildsMultipleFields();

}
