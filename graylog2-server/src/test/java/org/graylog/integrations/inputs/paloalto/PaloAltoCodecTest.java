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
package org.graylog.integrations.inputs.paloalto;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.io.FileWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class PaloAltoCodecTest {

    // These messages are in Panorama format. Panorama is Palo Alto's log management system.
    // Messages forwarded from Panorama will have the  - - - -  delimiter.
    private final static String PANORAMA_TRAFFIC_MESSAGE = "<14>1 2018-09-19T11:50:32-05:00 Panorama--2 - - - - 1,2018/09/19 11:50:32,453524335,TRAFFIC,end,2049,2018/09/19 11:50:32,10.20.30.40,10.20.30.40,10.20.30.40,10.20.30.40,HTTPS-strict,,,incomplete,vsys1,Public,Public,ethernet1/1,ethernet1/1,ALK Logging,2018/09/19 11:50:32,205742,1,64575,443,41304,443,0x400070,tcp,allow,412,272,140,6,2018/09/19 11:50:15,0,any,0,54196730,0x8000000000000000,10.20.30.40-10.20.30.40,10.20.30.40-10.20.30.40,0,4,2,tcp-fin,13,16,0,0,,Prod--2,from-policy,,,0,,0,,N/A,0,0,0,0";
    private final static String PANORAMA_SYSTEM_MESSAGE = "<14>1 2018-09-19T11:50:35-05:00 Panorama-1 - - - - 1,2018/09/19 11:50:35,000710000506,SYSTEM,general,0,2018/09/19 11:50:35,,general,,0,0,general,informational,\"Deviating device: Prod--2, Serial: 453524335, Object: N/A, Metric: mp-cpu, Value: 34\",1163103,0x0,0,0,0,0,,Panorama-1";
    private final static String PANORAMA_THREAT_MESSAGE = "<14>1 2018-09-19T11:50:33-05:00 Panorama--1 - - - - 1,2018/09/19 11:50:33,007255000045716,THREAT,spyware,2049,2018/09/19 11:50:33,10.20.30.40,10.20.30.40,10.20.30.40,10.20.30.40,HTTPS-strict,,,ssl,vsys1,Public,Public,ethernet1/1,ethernet1/1,ALK Logging,2018/09/19 11:50:33,201360,1,21131,443,56756,443,0x80403000,tcp,alert,\"test.com/\",Suspicious TLS Evasion Found(14978),online_test.com,informational,client-to-server,1007133,0xa000000000000000,10.20.30.40-10.20.30.40,10.20.30.40-10.20.30.40,0,,1204440535977427988,,,0,,,,,,,,0,13,16,0,0,,Prod--1,,,,,0,,0,,N/A,spyware,AppThreat-8065-5006,0x0,0,4294967295";
    private final static String PANORAMA_WITH_LINE_BREAK = "<14>1 2018-09-19T11:50:35-05:00 Panorama-1 - - - - 1,2018/09/19 11:50:35,000710000506,SYSTEM,general,0,2018/09/19 11:50:35,,general,,0,0,general,informational,\\\"Deviating device: Prod--2, Serial: 453524335, Object: N/A, Metric: mp-cpu, Value: 34\\\",1163103,0x0,0,0,0,0,,Panorama-1\n";

    // Raw PAN device messages.
    // These help to test the various combinations that we might see.
    private final static String SYSLOG_THREAT_MESSAGE = "<14>Aug 22 11:21:04 hq-lx-net-7.dart.org 1,2018/08/22 11:21:04,013201001141,THREAT,vulnerability,0,2018/08/22 11:21:02,10.0.190.116,10.0.2.225,0.0.0.0,0.0.0.0,DMZ-to-LAN_hq-direct-access,dart\\abluitt,dart\\kmendoza_admin,msrpc,vsys1,DMZ-2_L3,LAN_L3,ethernet1/3,ethernet1/6,Panorama,2018/08/22 11:21:02,398906,1,26475,135,0,0,0x2000,tcp,alert,\"\",Microsoft RPC Endpoint Mapper Detection(30845),any,informational,client-to-server,6585310726021616818,0x8000000000000000,10.0.0.0-10.255.255.255,10.0.0.0-10.255.255.255,0,,0,,,0,,,,,,,,0,346,12,0,0,,pa5220-hq-mdf-1,,,,,0,,0,,N/A,info-leak,AppThreat-8054-4933,0x0";
    private final static String SYSLOG_THREAT_MESSAGE_DOUBLE_SPACE_DATE = "<14>Aug  2 11:21:04 hq-lx-net-7.dart.org 1,2018/08/22 11:21:04,013201001141,THREAT,vulnerability,0,2018/08/22 11:21:02,10.0.190.116,10.0.2.225,0.0.0.0,0.0.0.0,DMZ-to-LAN_hq-direct-access,dart\\abluitt,dart\\kmendoza_admin,msrpc,vsys1,DMZ-2_L3,LAN_L3,ethernet1/3,ethernet1/6,Panorama,2018/08/22 11:21:02,398906,1,26475,135,0,0,0x2000,tcp,alert,\"\",Microsoft RPC Endpoint Mapper Detection(30845),any,informational,client-to-server,6585310726021616818,0x8000000000000000,10.0.0.0-10.255.255.255,10.0.0.0-10.255.255.255,0,,0,,,0,,,,,,,,0,346,12,0,0,,pa5220-hq-mdf-1,,,,,0,,0,,N/A,info-leak,AppThreat-8054-4933,0x0";
    private final static String SYSLOG_THREAT_MESSAGE_NO_HOST = "<14>Apr  8 01:47:32 1,2012/04/08 01:47:32,001606001116,THREAT,file,1,2012/04/08 01:47:27,217.31.49.10,192.168.0.2,0.0.0.0,0.0.0.0,rule1,,tng\\crusher,web-browsing,vsys1,untrust,trust,ethernet1/2,ethernet1/1,forwardAll,2012/04/08 01:47:32,1628,1,80,51060,0,0,0x200000,tcp,block-continue,\"imer.up\",Windows Executable (EXE)(52020),any,low,server-to-client,0,0x0,Czech Republic,192.168.0.0-192.168.255.255,0,";
    private final static String SYSLOG_THREAT_MESSAGE_NO_HOST_DOUBLE_SPACE_DATE = "<14>Apr  8 01:47:32 1,2012/04/08 01:47:32,001606001116,THREAT,file,1,2012/04/08 01:47:27,217.31.49.10,192.168.0.2,0.0.0.0,0.0.0.0,rule1,,tng\\crusher,web-browsing,vsys1,untrust,trust,ethernet1/2,ethernet1/1,forwardAll,2012/04/08 01:47:32,1628,1,80,51060,0,0,0x200000,tcp,block-continue,\"imer.up\",Windows Executable (EXE)(52020),any,low,server-to-client,0,0x0,Czech Republic,192.168.0.0-192.168.255.255,0,";
    private final static String SYSLOG_WITH_LINE_BREAK = "<14>Aug 22 11:21:04 hq-lx-net-7.dart.org 1,2018/08/22 11:21:04,013201001141,THREAT,vulnerability,0,2018/08/22 11:21:02,10.0.190.116,10.0.2.225,0.0.0.0,0.0.0.0,DMZ-to-LAN_hq-direct-access,dart\\abluitt,dart\\kmendoza_admin,msrpc,vsys1,DMZ-2_L3,LAN_L3,ethernet1/3,ethernet1/6,Panorama,2018/08/22 11:21:02,398906,1,26475,135,0,0,0x2000,tcp,alert,\"\",Microsoft RPC Endpoint Mapper Detection(30845),any,informational,client-to-server,6585310726021616818,0x8000000000000000,10.0.0.0-10.255.255.255,10.0.0.0-10.255.255.255,0,,0,,,0,,,,,,,,0,346,12,0,0,,pa5220-hq-mdf-1,,,,,0,,0,,N/A,info-leak,AppThreat-8054-4933,0x0\n";

    private final static String[] MORE_SYSLOG_THREAT_MESSAGES =
            {"<14>Aug  8 11:21:04 hq-lx-net-7.dart.org 1,2018/08/22 11:21:04,013201001141,THREAT,vulnerability,0,2018/08/22 11:21:02,10.0.190.116,10.0.2.225,0.0.0.0,0.0.0.0,DMZ-to-LAN_hq-direct-access,dart\\abluitt,dart\\kmendoza_admin,msrpc,vsys1,DMZ-2_L3,LAN_L3,ethernet1/3,ethernet1/6,Panorama,2018/08/22 11:21:02,398906,1,26475,135,0,0,0x2000,tcp,alert,\"\",Microsoft RPC Endpoint Mapper Detection(30845),any,informational,client-to-server,6585310726021616818,0x8000000000000000,10.0.0.0-10.255.255.255,10.0.0.0-10.255.255.255,0,,0,,,0,,,,,,,,0,346,12,0,0,,pa5220-hq-mdf-1,,,,,0,,0,,N/A,info-leak,AppThreat-8054-4933,0x0",
                    "<14>Aug 22 11:21:04 hq-lx-net-7.dart.org 1,2018/08/22 11:21:04,013201001141,THREAT,vulnerability,0,2018/08/22 11:21:02,10.0.190.116,10.0.2.225,0.0.0.0,0.0.0.0,DMZ-to-LAN_hq-direct-access,dart\\abluitt,dart\\kmendoza_admin,msrpc,vsys1,DMZ-2_L3,LAN_L3,ethernet1/3,ethernet1/6,Panorama,2018/08/22 11:21:02,398906,1,26475,135,0,0,0x2000,tcp,alert,\"\",Microsoft RPC Endpoint Mapper Detection(30845),any,informational,client-to-server,6585310726021616818,0x8000000000000000,10.0.0.0-10.255.255.255,10.0.0.0-10.255.255.255,0,,0,,,0,,,,,,,,0,346,12,0,0,,pa5220-hq-mdf-1,,,,,0,,0,,N/A,info-leak,AppThreat-8054-4933,0x0",
                    "<13>Aug 22 11:21:04 hq-lx-net-7.dart.org 1,2018/08/22 11:21:04,013201001141,THREAT,vulnerability,0,2018/08/22 11:21:02,10.0.58.109,13.66.22.101,198.51.223.40,13.66.22.101,LAN-to-WAN-90,dart\\rulak,,ftp,vsys1,LAN_L3,WAN_L3,ethernet1/6,ethernet1/1,Panorama,2018/08/22 11:21:02,3829077,1,52670,21,35329,21,0x402000,tcp,alert,\"Web.config\",FTP REST(36419),any,low,client-to-server,6585310726021616817,0x8000000000000000,10.0.0.0-10.255.255.255,United States,0,,0,,,0,,,,,,,,0,346,12,0,0,,pa5220-hq-mdf-1,,,,,0,,0,,N/A,info-leak,AppThreat-8054-4933,0x0",
                    "<13>Aug 22 11:21:04 hq-lx-net-7.dart.org 1,2018/08/22 11:21:04,013201001141,THREAT,vulnerability,0,2018/08/22 11:21:02,10.0.58.109,13.66.22.101,198.51.223.40,13.66.22.101,LAN-to-WAN-90,dart\\rulak,,ftp,vsys1,LAN_L3,WAN_L3,ethernet1/6,ethernet1/1,Panorama,2018/08/22 11:21:02,3829077,1,52670,21,35329,21,0x402000,tcp,alert,\"Web.config\",FTP REST(36419),any,low,client-to-server,6585310726021616817,0x8000000000000000,10.0.0.0-10.255.255.255,United States,0,,0,,,0,,,,,,,,0,346,12,0,0,,pa5220-hq-mdf-1,,,,,0,,0,,N/A,info-leak,AppThreat-8054-4933,0x0",
                    "<14>Aug 22 11:21:04 hq-lx-net-7.dart.org 1,2018/08/22 11:21:04,013201001141,THREAT,vulnerability,0,2018/08/22 11:20:59,10.0.2.251,10.0.190.117,0.0.0.0,0.0.0.0,DMZ-to-LAN_hq-direct-access,,dart\\dmartin2,ms-ds-smbv2,vsys1,LAN_L3,DMZ-2_L3,ethernet1/6,ethernet1/3,Panorama,2018/08/22 11:20:59,667895,2,445,20738,0,0,0x2000,tcp,alert,\"27758 Oliver 801702018 Bus Drawing.doc\",Microsoft Office File with Macros Detected(39154),any,informational,server-to-client,6585310726021616815,0x8000000000000000,10.0.0.0-10.255.255.255,10.0.0.0-10.255.255.255,0,,0,,,0,,,,,,,,0,346,12,0,0,,pa5220-hq-mdf-1,,,,,0,,0,,N/A,code-execution,AppThreat-8054-4933,0x0",
                    "<14>Aug 22 11:21:04 hq-lx-net-7.dart.org 1,2018/08/22 11:21:04,013201001141,THREAT,vulnerability,0,2018/08/22 11:20:59,10.0.2.251,10.0.190.117,0.0.0.0,0.0.0.0,DMZ-to-LAN_hq-direct-access,,dart\\dmartin2,ms-ds-smbv2,vsys1,LAN_L3,DMZ-2_L3,ethernet1/6,ethernet1/3,Panorama,2018/08/22 11:20:59,667895,2,445,20738,0,0,0x2000,tcp,alert,\"27758 Oliver 801702018 Bus Drawing.doc\",Microsoft Office File with Macros Detected(39154),any,informational,server-to-client,6585310726021616815,0x8000000000000000,10.0.0.0-10.255.255.255,10.0.0.0-10.255.255.255,0,,0,,,0,,,,,,,,0,346,12,0,0,,pa5220-hq-mdf-1,,,,,0,,0,,N/A,code-execution,AppThreat-8054-4933,0x0",
                    "<14>Aug 22 11:21:04 hq-lx-net-7.dart.org 1,2018/08/22 11:21:04,013201001141,THREAT,vulnerability,0,2018/08/22 11:20:55,10.0.50.176,10.0.190.103,0.0.0.0,0.0.0.0,LAN-to-DMZ-103_L4,dart\\opendns_connector,,msrpc,vsys1,LAN_L3,DMZ-2_L3,ethernet1/6,ethernet1/3,Panorama,2018/08/22 11:20:55,259887,2,55836,135,0,0,0x2000,tcp,alert,\"\",Microsoft RPC ISystemActivator bind(30846),any,informational,client-to-server,6585310726021616810,0x8000000000000000,10.0.0.0-10.255.255.255,10.0.0.0-10.255.255.255,0,,0,,,0,,,,,,,,0,346,12,0,0,,pa5220-hq-mdf-1,,,,,0,,0,,N/A,info-leak,AppThreat-8054-4933,0x0",
                    "<14>Aug 22 11:21:04 hq-lx-net-7.dart.org 1,2018/08/22 11:21:04,013201001141,THREAT,vulnerability,0,2018/08/22 11:20:55,10.0.50.176,10.0.190.103,0.0.0.0,0.0.0.0,LAN-to-DMZ-103_L4,dart\\opendns_connector,,msrpc,vsys1,LAN_L3,DMZ-2_L3,ethernet1/6,ethernet1/3,Panorama,2018/08/22 11:20:55,259887,2,55836,135,0,0,0x2000,tcp,alert,\"\",Microsoft RPC ISystemActivator bind(30846),any,informational,client-to-server,6585310726021616810,0x8000000000000000,10.0.0.0-10.255.255.255,10.0.0.0-10.255.255.255,0,,0,,,0,,,,,,,,0,346,12,0,0,,pa5220-hq-mdf-1,,,,,0,,0,,N/A,info-leak,AppThreat-8054-4933,0x0",
                    "<14>Aug 22 11:21:04 hq-lx-net-7.dart.org 1,2018/08/22 11:21:04,013201001141,THREAT,wildfire,0,2018/08/22 11:20:55,23.43.62.88,10.4.25.93,23.43.62.88,198.51.223.40,LAN-to-WAN-Known-User,,dart\\dhoftender,web-browsing,vsys1,WAN_L3,LAN_L3,ethernet1/1,ethernet1/6,Panorama,2018/08/22 11:20:55,895841,1,80,50844,80,35947,0x402000,tcp,allow,\"stream.x86.x-none.dat\",Windows Dynamic Link Library (DLL)(52019),benign,informational,server-to-client,6585310726021616809,0x8000000000000000,United States,10.0.0.0-10.255.255.255,0,,0,9e5c336d886db943e0e464efb1e375d2a29d4ba117255bc6aa9a7053b86577c0,wildfire.paloaltonetworks.com,50,,pe,,,,,,11069273395,346,12,0,0,,pa5220-hq-mdf-1,,,,,0,,0,,N/A,unknown,WildFire-0-0,0x0",
                    "<14>Aug 22 11:21:04 hq-lx-net-7.dart.org 1,2018/08/22 11:21:04,013201001141,THREAT,wildfire,0,2018/08/22 11:20:55,23.43.62.88,10.4.25.93,23.43.62.88,198.51.223.40,LAN-to-WAN-Known-User,,dart\\dhoftender,web-browsing,vsys1,WAN_L3,LAN_L3,ethernet1/1,ethernet1/6,Panorama,2018/08/22 11:20:55,895841,1,80,50844,80,35947,0x402000,tcp,allow,\"stream.x86.x-none.dat\",Windows Dynamic Link Library (DLL)(52019),benign,informational,server-to-client,6585310726021616809,0x8000000000000000,United States,10.0.0.0-10.255.255.255,0,,0,9e5c336d886db943e0e464efb1e375d2a29d4ba117255bc6aa9a7053b86577c0,wildfire.paloaltonetworks.com,50,,pe,,,,,,11069273395,346,12,0,0,,pa5220-hq-mdf-1,,,,,0,,0,,N/A,unknown,WildFire-0-0,0x0"};

    private final MessageFactory messageFactory = new TestMessageFactory();

    @Test
    public void testAllSyslogFormats() {

        PaloAltoCodec codec = new PaloAltoCodec(Configuration.EMPTY_CONFIGURATION, messageFactory);

        Message message = codec.decode(new RawMessage(SYSLOG_THREAT_MESSAGE.getBytes(StandardCharsets.UTF_8)));
        assertEquals("THREAT", message.getField("type"));

        message = codec.decode(new RawMessage(SYSLOG_THREAT_MESSAGE_DOUBLE_SPACE_DATE.getBytes(StandardCharsets.UTF_8)));
        assertEquals("THREAT", message.getField("type"));

        message = codec.decode(new RawMessage(SYSLOG_THREAT_MESSAGE_NO_HOST.getBytes(StandardCharsets.UTF_8)));
        assertEquals("THREAT", message.getField("type"));

        message = codec.decode(new RawMessage(SYSLOG_THREAT_MESSAGE_NO_HOST_DOUBLE_SPACE_DATE.getBytes(StandardCharsets.UTF_8)));
        assertEquals("THREAT", message.getField("type"));
    }

    @Test
    public void testMessageWithLineBreak() {

        // Verify that a messages with a line break at the end does not break parsing.
        PaloAltoCodec codec = new PaloAltoCodec(Configuration.EMPTY_CONFIGURATION, messageFactory);
        Message message = codec.decode(new RawMessage(PANORAMA_WITH_LINE_BREAK.getBytes(StandardCharsets.UTF_8)));
        assertEquals("SYSTEM", message.getField("type"));

        codec = new PaloAltoCodec(Configuration.EMPTY_CONFIGURATION, messageFactory);
        message = codec.decode(new RawMessage(SYSLOG_WITH_LINE_BREAK.getBytes(StandardCharsets.UTF_8)));
        assertEquals("THREAT", message.getField("type"));
    }

    @Test
    public void testMoreSyslogFormats() {

        // Test an extra list of messages.
        for (String threatString : MORE_SYSLOG_THREAT_MESSAGES) {
            PaloAltoCodec codec = new PaloAltoCodec(Configuration.EMPTY_CONFIGURATION, messageFactory);
            Message message = codec.decode(new RawMessage(threatString.getBytes(StandardCharsets.UTF_8)));
            assertEquals("THREAT", message.getField("type"));
        }
    }

    @Test
    public void syslogValuesTest() {

        // Test System message results
        PaloAltoCodec codec = new PaloAltoCodec(Configuration.EMPTY_CONFIGURATION, messageFactory);
        Message message = codec.decode(new RawMessage(SYSLOG_THREAT_MESSAGE_NO_HOST_DOUBLE_SPACE_DATE.getBytes(StandardCharsets.UTF_8)));
        assertEquals("THREAT", message.getField("type"));
    }

    @Test
    public void valuesTest() {

        // Test System message results
        PaloAltoCodec codec = new PaloAltoCodec(Configuration.EMPTY_CONFIGURATION, messageFactory);
        Message message = codec.decode(new RawMessage(PANORAMA_SYSTEM_MESSAGE.getBytes(StandardCharsets.UTF_8)));
        assertEquals("SYSTEM", message.getField("type"));
        assertEquals(message.getField("module"), "general");

        // Test quoted value with embedded commas.
        assertEquals(message.getField("description"), "Deviating device: Prod--2, Serial: 453524335, Object: N/A, Metric: mp-cpu, Value: 34");
        assertEquals(message.getField("serial_number"), "000710000506");
        assertEquals(message.getField("source"), "Panorama-1");
        assertEquals(message.getField("message"), "1,2018/09/19 11:50:35,000710000506,SYSTEM,general,0,2018/09/19 11:50:35,,general,,0,0,general,informational,\"Deviating device: Prod--2, Serial: 453524335, Object: N/A, Metric: mp-cpu, Value: 34\",1163103,0x0,0,0,0,0,,Panorama-1");
        assertEquals(message.getField("severity"), "informational");
        assertEquals(message.getField("generated_time"), "2018/09/19 11:50:35");
        assertEquals(message.getField("event_id"), "general");
        assertEquals(message.getField("device_name"), "Panorama-1");
        assertEquals(message.getField("content_threat_type"), "general");
        assertEquals(message.getField("virtual_system_name"), null);
        assertEquals(0, ((DateTime) message.getField("timestamp")).compareTo(new DateTime("2018-09-19T11:50:35.000-05:00", DateTimeZone.UTC)));

        // Test Traffic message results
        message = codec.decode(new RawMessage(PANORAMA_TRAFFIC_MESSAGE.getBytes(StandardCharsets.UTF_8)));
        assertEquals(message.getField("bytes_received"), 140L);
        assertEquals(message.getField("source"), "Panorama--2");
        assertEquals(message.getField("repeat_count"), 1L);
        assertEquals(message.getField("receive_time"), "2018/09/19 11:50:32");
        assertEquals(message.getField("outbound_interface"), "ethernet1/1");
        assertEquals(message.getField("packets"), 6L);
        assertEquals(message.getField("dest_location"), "10.20.30.40-10.20.30.40");
        assertEquals(message.getField("src_addr"), "10.20.30.40");
        assertEquals(message.getField("generated_time"), "2018/09/19 11:50:32");
        assertEquals(message.getField("protocol"), "tcp");
        assertEquals(message.getField("threat_content_type"), "end");
        assertEquals(message.getField("packets_sent"), 4L);
        assertEquals(message.getField("packets_received"), 2L);
        assertEquals(message.getField("action"), "allow");
        assertEquals(message.getField("virtual_system"), "vsys1");
        assertEquals(message.getField("dest_port"), 443L);
        assertEquals(((DateTime) message.getField("timestamp")).compareTo(new DateTime("2018-09-19T11:50:32.000-05:00", DateTimeZone.UTC)), 0);
        assertEquals(message.getField("rule_name"), "HTTPS-strict");
        assertEquals(message.getField("nat_src_addr"), "10.20.30.40");
        assertEquals(message.getField("session_id"), 205742L);
        assertEquals(message.getField("serial_number"), "453524335");
        assertEquals(message.getField("message"), "1,2018/09/19 11:50:32,453524335,TRAFFIC,end,2049,2018/09/19 11:50:32,10.20.30.40,10.20.30.40,10.20.30.40,10.20.30.40,HTTPS-strict,,,incomplete,vsys1,Public,Public,ethernet1/1,ethernet1/1,ALK Logging,2018/09/19 11:50:32,205742,1,64575,443,41304,443,0x400070,tcp,allow,412,272,140,6,2018/09/19 11:50:15,0,any,0,54196730,0x8000000000000000,10.20.30.40-10.20.30.40,10.20.30.40-10.20.30.40,0,4,2,tcp-fin,13,16,0,0,,Prod--2,from-policy,,,0,,0,,N/A,0,0,0,0");
        assertEquals(message.getField("bytes_sent"), 272L);
        assertEquals(message.getField("dest_zone"), "Public");
        assertEquals(message.getField("nat_src_port"), 41304L);
        assertEquals(message.getField("src_port"), 64575L);
        assertEquals(message.getField("src_location"), "10.20.30.40-10.20.30.40");
        assertEquals(message.getField("log_action"), "ALK Logging");
        assertEquals(message.getField("inbound_interface"), "ethernet1/1");
        assertEquals(message.getField("application"), "incomplete");
        assertEquals(message.getField("src_zone"), "Public");
        assertEquals(message.getField("bytes"), 412L);
        assertEquals(message.getField("dest_addr"), "10.20.30.40");
        assertEquals(message.getField("type"), "TRAFFIC");
        assertEquals(message.getField("nat_dest_addr"), "10.20.30.40");
        assertEquals(message.getField("category"), "any");
        assertEquals(message.getField("nat_dest_port"), 443L);
    }

    /**
     * Helper for parsing PAN messages from HEX export
     */
    public void dataParserTest() throws Exception {

        List<String> hexVals = new ArrayList<>();
        String buffer = "";
        for (String textLine : getTextLines()) {
            if (!textLine.equals("")) {
                buffer += textLine;
            } else {
                hexVals.add(buffer);
                buffer = "";
            }
        }

        hexVals = hexVals.stream().map(s -> s.replace(" ", "")).map(h -> {
            byte[] bytes = new byte[0];
            try {
                bytes = Hex.decodeHex(h.toCharArray());
            } catch (DecoderException e) {
                e.printStackTrace();
            }
            try {
                return new String(bytes, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return "";
        })
                         .filter(s -> s.contains("- - - -"))
                         .filter(s -> s.contains(">1"))
                         .filter(s -> s.contains("<"))
                         .map(s -> s.substring(s.indexOf(">1") - 3, s.length()))
                         .collect(Collectors.toList());


        FileWriter writer = new FileWriter("capture-clean.txt", StandardCharsets.UTF_8);
        for (String str : hexVals) {
            writer.write(str + "\n");
        }
        writer.close();
    }

    private List<String> getTextLines() throws Exception {

        String s = new String(Files.readAllBytes(Paths.get("capture")), StandardCharsets.UTF_8);
        return Arrays.asList(s.replace("\t", "").split("\\n")).stream()
                .map(v -> {
                            String withoutPrefix = v.length() > 7 ? v.substring(7, v.length()) : v;

                            if (withoutPrefix.length() > 32) {
                                withoutPrefix = withoutPrefix.substring(0, 39);
                            }
                            return withoutPrefix;
                        }
                ).collect(Collectors.toList());
    }
}
