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
package org.graylog.integrations.ipfix;

import com.google.auto.value.AutoValue;

import java.util.Locale;

@AutoValue
public abstract class InformationElementDefinition {
    public static InformationElementDefinition create(String dataType, String fieldName, int id) {
        final String cleanDataType = dataType.trim().toUpperCase(Locale.ENGLISH);
        return new AutoValue_InformationElementDefinition(DataType.valueOf(cleanDataType), fieldName, id);
    }

    public abstract DataType dataType();

    public abstract String fieldName();

    public abstract int id();


    public enum DataType {
        UNSIGNED8,
        UNSIGNED16,
        UNSIGNED32,
        UNSIGNED64,
        SIGNED8,
        SIGNED16,
        SIGNED32,
        SIGNED64,
        FLOAT32,
        FLOAT64,
        MACADDRESS,
        IPV4ADDRESS,
        IPV6ADDRESS,
        BOOLEAN,
        STRING,
        OCTETARRAY,
        DATETIMESECONDS,
        DATETIMEMILLISECONDS,
        DATETIMEMICROSECONDS,
        DATETIMENANOSECONDS,
        BASICLIST,
        SUBTEMPLATELIST,
        SUBTEMPLATEMULTILIST,
    }
}
