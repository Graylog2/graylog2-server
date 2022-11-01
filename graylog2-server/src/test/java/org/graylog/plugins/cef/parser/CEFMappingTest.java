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
package org.graylog.plugins.cef.parser;

import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

public class CEFMappingTest {
    private static final CEFMapping[] BIG_INT_FIELDS = new CEFMapping[]{
            CEFMapping.cn1, CEFMapping.cn2, CEFMapping.cn3, CEFMapping.cn4, CEFMapping.cnt, CEFMapping.destinationTranslatedPort,
            CEFMapping.dpid, CEFMapping.dpt, CEFMapping.dvcpid, CEFMapping.fsize, CEFMapping.in, CEFMapping.oldFileSize,
            CEFMapping.out, CEFMapping.sourceTranslatedPort, CEFMapping.spid, CEFMapping.spt
    };

    @Test
    public void forKeyName() throws Exception {
        for (CEFMapping mapping : CEFMapping.values()) {
            assertEquals(mapping, CEFMapping.forKeyName(mapping.getKeyName()));
        }
    }

    @Test
    public void forFullName() throws Exception {
        for (CEFMapping mapping : CEFMapping.values()) {
            assertEquals(mapping, CEFMapping.forFullName(mapping.getFullName()));
        }
    }

    @Test
    public void convertLargeValues() throws Exception {
        String bigValueString = String.valueOf(Integer.MAX_VALUE) + 1;
        BigInteger bigValue = new BigInteger(bigValueString);
        for (CEFMapping fieldMapping : BIG_INT_FIELDS) {
            Object mapping = fieldMapping.convert(bigValueString);
            assertEquals(bigValue, mapping);
        }
    }
}
