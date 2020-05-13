package org.graylog2.utilities;

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
        final GRN grn = GRN.builder().type("dashboard").entity("54e3deadbeefdeadbeef0000").permissionPrefix("dashboards:").build();

        assertEquals(grn.toString(), "grn::::dashboard:54e3deadbeefdeadbeef0000");
    }

    @Test
    public void compareGRNs() {
        final String testGRN = "grn::::stream:000000000000000002";
        final GRN grn1 = GRN.parse(testGRN, GRN_REGISTRY);
        final GRN grn2 = GRN.builder().type("stream").entity("000000000000000002").permissionPrefix("streams:").build();

        assertEquals(grn1, grn2);
    }

}
