/*
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.graylog2.utilities;

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.*;

public class GRNTest {

    @Test
    public void parse() {
        final String testGRN = "grn::::stream:000000000000000001";
        final GRN grn = GRN.parse(testGRN);

        assertEquals(grn.type(), "stream");
        assertEquals(grn.entity(), "000000000000000001");

        assertEquals(grn.toString(), testGRN);
    }

    @Test
    public void parseNormalize() {
        final String testGRN = "gRN::::stREAM:000000000000000001";
        final GRN grn = GRN.parse(testGRN);

        assertEquals(grn.type(), "stream");
        assertEquals(grn.entity(), "000000000000000001");

        assertEquals(grn.toString(), testGRN.toLowerCase(Locale.ENGLISH));
    }

    @Test
    public void builderWithEntitytTest() {
        final GRN grn = GRN.builder().type("dashboard").entity("54e3deadbeefdeadbeef0000").build();

        assertEquals(grn.toString(), "grn::::dashboard:54e3deadbeefdeadbeef0000");
    }

    @Test
    public void compareGRNs() {
        final String testGRN = "grn::::stream:000000000000000002";
        final GRN grn1 = GRN.parse(testGRN);
        final GRN grn2 = GRN.builder().type("stream").entity("000000000000000002").build();

        assertEquals(grn1, grn2);
    }

}
