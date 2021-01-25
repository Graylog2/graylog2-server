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
package org.graylog.grn;

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class GRNTest {

    private static final GRNRegistry GRN_REGISTRY = GRNRegistry.createWithBuiltinTypes();

    @Test
    public void parse() {
        final String testGRN = "grn::::stream:000000000000000001";
        final GRN grn = GRN.parse(testGRN, GRN_REGISTRY);

        assertEquals(grn.type(), "stream");
        assertEquals(grn.entity(), "000000000000000001");

        assertEquals(grn.toString(), testGRN);
    }

    @Test
    public void parseNormalize() {
        final String testGRN = "gRN::::stREAM:000000000000000001";
        final GRN grn = GRN.parse(testGRN, GRN_REGISTRY);

        assertEquals(grn.type(), "stream");
        assertEquals(grn.entity(), "000000000000000001");

        assertEquals(grn.toString(), testGRN.toLowerCase(Locale.ENGLISH));
    }

    @Test
    public void builderWithEntitytTest() {
        final GRN grn = GRNType.create("dashboard", "dashboards:").newGRNBuilder().entity("54e3deadbeefdeadbeef0000").build();

        assertEquals(grn.toString(), "grn::::dashboard:54e3deadbeefdeadbeef0000");
    }

    @Test
    public void compareGRNs() {
        final String testGRN = "grn::::stream:000000000000000002";
        final GRN grn1 = GRN.parse(testGRN, GRN_REGISTRY);
        final GRN grn2 = GRNType.create("stream", "streams:").newGRNBuilder().entity("000000000000000002").build();

        assertEquals(grn1, grn2);
    }

}
