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
package org.graylog.plugins.threatintel.whois.ip.parsers;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LACNICResponseParserTest {

    private static final String MATCH = "% Joint Whois - whois.lacnic.net\n" +
            "%  This server accepts single ASN, IPv4 or IPv6 queries\n" +
            " \n" +
            "% LACNIC resource: whois.lacnic.net\n" +
            "\n" +
            "\n" +
            "% Copyright LACNIC lacnic.net\n" +
            "%  The data below is provided for information purposes\n" +
            "%  and to assist persons in obtaining information about or\n" +
            "%  related to AS and IP numbers registrations\n" +
            "%  By submitting a whois query, you agree to use this data\n" +
            "%  only for lawful purposes.\n" +
            "%  2016-11-23 23:41:56 (BRST -02:00)\n" +
            "\n" +
            "inetnum:     190.46/15\n" +
            "status:      allocated\n" +
            "aut-num:     N/A\n" +
            "owner:       VTR BANDA ANCHA S.A.\n" +
            "ownerid:     CL-VPNS-LACNIC\n" +
            "responsible: Oscar Osorio\n" +
            "address:     Avenida del Valle Sur - Ciudad Empresarial, 534, 4th floor\n" +
            "address:     8581151 - Santiago - \n" +
            "country:     CL\n" +
            "phone:       +56 22 3101609 []\n" +
            "owner-c:     ISO\n" +
            "tech-c:      ISO\n" +
            "abuse-c:     ISO\n" +
            "inetrev:     190.46/15\n" +
            "nserver:     NS00.VTR.NET  \n" +
            "nsstat:      20161122 AA\n" +
            "nslastaa:    20161122\n" +
            "nserver:     NS01.VTR.NET  \n" +
            "nsstat:      20161122 AA\n" +
            "nslastaa:    20161122\n" +
            "created:     20060627\n" +
            "changed:     20060627\n" +
            "\n" +
            "nic-hdl:     ISO\n" +
            "person:      Administrador VTR\n" +
            "e-mail:      contactovtr@VTR.NET\n" +
            "address:     Apoquindo, 4800, 7 th floor\n" +
            "address:      - Santiago - \n" +
            "country:     CL\n" +
            "phone:       +56 2 23101502 []\n" +
            "created:     20020906\n" +
            "changed:     20150921\n" +
            "\n" +
            "% whois.lacnic.net accepts only direct match queries.\n" +
            "% Types of queries are: POCs, ownerid, CIDR blocks, IP\n" +
            "% and AS numbers.\n";

    @Test
    public void testRunDirectMatch() throws Exception {
        LACNICResponseParser parser = new LACNICResponseParser();
        for (String line : MATCH.split("\n")) {
            parser.readLine(line);
        }

        assertEquals("CL", parser.getCountryCode());
        assertEquals("VTR BANDA ANCHA S.A.", parser.getOrganization());
    }

}
