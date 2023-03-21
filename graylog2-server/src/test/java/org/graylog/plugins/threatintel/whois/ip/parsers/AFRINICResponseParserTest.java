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

public class AFRINICResponseParserTest {

    private static final String MATCH = "% Note: this output has been filtered.\n" +
            "%       To receive output for a database update, use the \"-B\" flag.\n" +
            "\n" +
            "% Information related to '196.25.255.0 - 196.25.255.255'\n" +
            "\n" +
            "% No abuse contact registered for 196.25.255.0 - 196.25.255.255\n" +
            "\n" +
            "inetnum:        196.25.255.0 - 196.25.255.255\n" +
            "netname:        IPNET-CHE-1\n" +
            "descr:          Telkom SA Limited\n" +
            "descr:          Integrated Network Planning\n" +
            "descr:          Private Bag X74\n" +
            "descr:          Pretoria\n" +
            "descr:          Gauteng\n" +
            "descr:          0001\n" +
            "country:        ZA\n" +
            "admin-c:        MST95-AFRINIC\n" +
            "tech-c:         JDU24-AFRINIC\n" +
            "tech-c:         PB455-AFRINIC\n" +
            "status:         ASSIGNED PA\n" +
            "remarks:        noc e-mail: <nnoc@saix.net>, phone: +27-12-680-0224\n" +
            "remarks:        abuse e-mail: <abuse@saix.net>, phone: +27-12-680-7561\n" +
            "mnt-by:         TELKOM-SA-IPNET-MNT\n" +
            "source:         AFRINIC # Filtered\n" +
            "parent:         196.25.0.0 - 196.25.255.255\n" +
            "\n" +
            "person:         Johan du Preez\n" +
            "address:        Telkom SA Ltd\n" +
            "address:        PO Box 2753\n" +
            "address:        Pretoria\n" +
            "address:        Gauteng\n" +
            "address:        0001\n" +
            "address:        ZA\n" +
            "phone:          +1 111 1111111\n" +
            "fax-no:         +2721 3111111\n" +
            "abuse-mailbox:  abuse@saix.net\n" +
            "nic-hdl:        JDU24-AFRINIC\n" +
            "remarks:        Abuse complaints can be directed to abuse@saix.net\n" +
            "remarks:        DNS Issues can be directed to dnsadmin@saix.net\n" +
            "source:         AFRINIC # Filtered\n" +
            "\n" +
            "person:         Markus Stoltz\n" +
            "nic-hdl:        MST95-AFRINIC\n" +
            "address:        Integrated Network Planning\n" +
            "address:        Private Bag X74\n" +
            "address:        Gauteng\n" +
            "address:        Pretoria 0001\n" +
            "address:        South Africa\n" +
            "phone:          +27-12-311-1429\n" +
            "source:         AFRINIC # Filtered\n" +
            "\n" +
            "person:         Pieter Bezuidenhout\n" +
            "address:        Telkom SA Ltd\n" +
            "address:        PO Box 2753\n" +
            "address:        Pretoria\n" +
            "address:        Gauteng\n" +
            "address:        0001\n" +
            "address:        ZA\n" +
            "phone:          +1 111 1111111\n" +
            "fax-no:         +2721 3111111\n" +
            "nic-hdl:        PB455-AFRINIC\n" +
            "remarks:        Abuse complaints can be directed to abuse@saix.net\n" +
            "remarks:        DNS Issues can be directed to dnsadmin@saix.net. Alex, can you see this\n" +
            "abuse-mailbox:  abuse@saix.net\n" +
            "source:         AFRINIC # Filtered\n";

    @Test
    public void testRunDirectMatch() throws Exception {
        AFRINICResponseParser parser = new AFRINICResponseParser();
        for (String line : MATCH.split("\n")) {
            parser.readLine(line);
        }

        assertEquals("ZA", parser.getCountryCode());
        assertEquals("Telkom SA Limited", parser.getOrganization());
    }

}
