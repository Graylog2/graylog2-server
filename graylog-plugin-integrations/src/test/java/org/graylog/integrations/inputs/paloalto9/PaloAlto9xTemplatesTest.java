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
package org.graylog.integrations.inputs.paloalto9;

import com.google.common.collect.ImmutableMap;
import org.graylog.integrations.inputs.paloalto.PaloAltoParser;
import org.graylog.schema.AlertFields;
import org.graylog.schema.ApplicationFields;
import org.graylog.schema.ContainerFields;
import org.graylog.schema.DestinationFields;
import org.graylog.schema.EmailFields;
import org.graylog.schema.EventFields;
import org.graylog.schema.FileFields;
import org.graylog.schema.HostFields;
import org.graylog.schema.HttpFields;
import org.graylog.schema.NetworkFields;
import org.graylog.schema.PolicyFields;
import org.graylog.schema.RuleFields;
import org.graylog.schema.SessionFields;
import org.graylog.schema.SourceFields;
import org.graylog.schema.ThreatFields;
import org.graylog.schema.UserFields;
import org.graylog.schema.VendorFields;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.journal.RawMessage;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class PaloAlto9xTemplatesTest {
    private static final String SYSLOG_PREFIX = "<14>1 2020-06-02T14:01:00.000Z PYTHON_TEST_SENDER - - - - ";
    
    // Code Under Test
    PaloAlto9xCodec cut;

    @Before
    public void setUp() {
        Configuration config = new Configuration(ImmutableMap.of(PaloAlto9xCodec.CK_STORE_FULL_MESSAGE, true));
        PaloAltoParser rawParser = new PaloAltoParser();
        PaloAlto9xParser palo9xParser = new PaloAlto9xParser();
        cut = new PaloAlto9xCodec(config, rawParser, palo9xParser);
    }

    @Test
    public void verifyConfigurationMessageParsing() {
        String log = "1,2020/05/26 04:11:09,007000000018919,CONFIG,0,0,2020/05/26 04:11:09,86.181.133.251,,multi-clone,aduncan@paloaltonetworks.com,Web,Succeeded,vsys  vsys1 profiles virus,,default-1  { decoder { http  { action default; wildfire-action default; } http2  { action default; wildfire-action default; } smtp  { action default; wildfire-action default; } imap  { action default; wildfire-action default; } pop3  { action default; wildfire-action default; } ftp  { action default; wildfire-action default; } smb  { action default; wildfire-action default; } } } ,5481,0x8000000000000000,0,0,0,0,,uk1,0,";
        String rawMessage = SYSLOG_PREFIX + log;
        RawMessage in = new RawMessage(rawMessage.getBytes());

        Message out = cut.decode(in);

        assertThat(out, notNullValue());
        assertThat(out.getField(Message.FIELD_FULL_MESSAGE), is(rawMessage));
        assertThat(out.getField(Message.FIELD_MESSAGE), is(log));
        assertThat(out.getField(EventFields.EVENT_SOURCE_PRODUCT), is("PAN"));
        
        assertThat(out.getField(EventFields.EVENT_CREATED), is("2020/05/26 04:11:09"));
        assertThat(out.getField(HostFields.HOST_ID), is("007000000018919"));
        assertThat(out.getField(EventFields.EVENT_LOG_NAME), is("CONFIG"));
        assertThat(out.getField(PaloAlto9xFields.PAN_LOG_SUBTYPE), is("0"));
        assertThat(out.getField(Message.FIELD_TIMESTAMP), is("2020/05/26 04:11:09"));
        assertThat(out.getField(SourceFields.SOURCE_REFERENCE), is("86.181.133.251"));
        assertThat(out.getField(HostFields.HOST_VIRTFW_ID), nullValue());
        assertThat(out.getField(UserFields.USER_COMMAND), is("multi-clone"));
        assertThat(out.getField(UserFields.USER_NAME), is("aduncan@paloaltonetworks.com"));
        assertThat(out.getField(VendorFields.VENDOR_SIGNIN_PROTOCOL), is("Web"));
        assertThat(out.getField(VendorFields.VENDOR_EVENT_OUTCOME), is("Succeeded"));
        assertThat(out.getField(UserFields.USER_COMMAND_PATH), is("vsys  vsys1 profiles virus"));
        assertThat(out.getField(PaloAlto9xFields.PAN_BEFORE_CHANGE_DETAIL), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_AFTER_CHANGE_DETAIL), is("default-1  { decoder { http  { action default; wildfire-action default; } http2  { action default; wildfire-action default; } smtp  { action default; wildfire-action default; } imap  { action default; wildfire-action default; } pop3  { action default; wildfire-action default; } ftp  { action default; wildfire-action default; } smb  { action default; wildfire-action default; } } }"));
        assertThat(out.getField(EventFields.EVENT_UID), is("5481"));
        assertThat(out.getField(PaloAlto9xFields.PAN_LOG_PANORAMA), is("0x8000000000000000"));
        assertThat(out.getField(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_1), is(0L));
        assertThat(out.getField(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_2), is(0L));
        assertThat(out.getField(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_3), is(0L));
        assertThat(out.getField(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_4), is(0L));
        assertThat(out.getField(HostFields.HOST_VIRTFW_HOSTNAME), nullValue());
        assertThat(out.getField(HostFields.HOST_HOSTNAME), is("uk1"));
    }

    @Test
    public void verifyCorrelationMessageParsing() {
        String log = "1,2020/05/31 17:19:44,0007SE00209,CORRELATION,,,2020/05/31 17:19:44,10.154.8.125,pancademo\\david.mccoy,,compromised-host,medium,31,40,0,0,,uk1rama,,beacon-heuristics,6005,\"Host has made use of Internet Relay Chat (IRC), a protocol popular with command-and-control activity.\"";
        String rawMessage = SYSLOG_PREFIX + log;
        RawMessage in = new RawMessage(rawMessage.getBytes());

        Message out = cut.decode(in);

        assertThat(out, notNullValue());
        assertThat(out.getField(Message.FIELD_FULL_MESSAGE), is(rawMessage));
        assertThat(out.getField(Message.FIELD_MESSAGE), is(log));
        assertThat(out.getField(EventFields.EVENT_SOURCE_PRODUCT), is("PAN"));

        assertThat(out.getField(EventFields.EVENT_CREATED), is("2020/05/31 17:19:44"));
        assertThat(out.getField(HostFields.HOST_ID), is("0007SE00209"));
        assertThat(out.getField(EventFields.EVENT_LOG_NAME), is("CORRELATION"));
        assertThat(out.getField(PaloAlto9xFields.PAN_LOG_SUBTYPE), nullValue());
        assertThat(out.getField(Message.FIELD_TIMESTAMP), is("2020/05/31 17:19:44"));
        assertThat(out.getField(SourceFields.SOURCE_IP), is("10.154.8.125"));
        assertThat(out.getField(UserFields.USER_NAME), is("pancademo\\david.mccoy"));
        assertThat(out.getField(HostFields.HOST_VIRTFW_ID), nullValue());
        assertThat(out.getField(ThreatFields.THREAT_CATEGORY), is("compromised-host"));
        assertThat(out.getField(EventFields.EVENT_SEVERITY), is("medium"));
        assertThat(out.getField(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_1), is(31L));
        assertThat(out.getField(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_2), is(40L));
        assertThat(out.getField(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_3), is(0L));
        assertThat(out.getField(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_4), is(0L));
        assertThat(out.getField(HostFields.HOST_VIRTFW_HOSTNAME), nullValue());
        assertThat(out.getField(HostFields.HOST_HOSTNAME), is("uk1rama"));
        assertThat(out.getField(HostFields.HOST_VIRTFW_UID), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_OBJECTNAME), is("beacon-heuristics"));
        assertThat(out.getField(PaloAlto9xFields.PAN_OBJECT_ID), is("6005"));
        assertThat(out.getField(PaloAlto9xFields.PAN_EVIDENCE), is("Host has made use of Internet Relay Chat (IRC), a protocol popular with command-and-control activity."));
    }

    @Test
    public void verifyGlobalProtectMessageParsing() {
        String log = "1,2020/04/01 10:49:35,015351000040055,11,0x0,GLOBALPROTECT,0,2305,2020/04/01 10:49:35,vsys1,portal-prelogin,before-login,,,,192.168.0.0-192.168.255.255,,192.168.45.33,0.0.0.0,0.0.0.0,0.0.0.0,2c2ec970-de09-444c-b84f-2c0be75e13cd,,Browser,Windows,\"Microsoft Windows 7  Service Pack 1, 64-bit\",1,,,\"\",success,,0,,0,gp-portal";
        String rawMessage = SYSLOG_PREFIX + log;
        RawMessage in = new RawMessage(rawMessage.getBytes());

        Message out = cut.decode(in);

        assertThat(out, notNullValue());
        assertThat(out.getField(Message.FIELD_FULL_MESSAGE), is(rawMessage));
        assertThat(out.getField(Message.FIELD_MESSAGE), is(log));
        assertThat(out.getField(EventFields.EVENT_SOURCE_PRODUCT), is("PAN"));

        assertThat(out.getField(EventFields.EVENT_RECEIVED_TIME), is("2020/04/01 10:49:35"));
        assertThat(out.getField(HostFields.HOST_ID), is("015351000040055"));
        assertThat(out.getField(EventFields.EVENT_UID), is("11"));
        assertThat(out.getField(PaloAlto9xFields.PAN_LOG_PANORAMA), is("0x0"));
        assertThat(out.getField(EventFields.EVENT_LOG_NAME), is("GLOBALPROTECT"));
        assertThat(out.getField(Message.FIELD_TIMESTAMP), is("2020/04/01 10:49:35"));
        assertThat(out.getField(HostFields.HOST_VIRTFW_ID), is("vsys1"));
        assertThat(out.getField(PaloAlto9xFields.PAN_EVENT_NAME), is("portal-prelogin"));
        assertThat(out.getField(PaloAlto9xFields.PAN_TUNNEL_STAGE), is("before-login"));
        assertThat(out.getField(PaloAlto9xFields.PAN_AUTH_METHOD), nullValue());
        assertThat(out.getField(NetworkFields.NETWORK_TUNNEL_TYPE), nullValue());
        assertThat(out.getField(SourceFields.SOURCE_USER), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_SOURCE_REGION), is("192.168.0.0-192.168.255.255"));
        assertThat(out.getField(SourceFields.SOURCE_HOSTNAME), nullValue());
        assertThat(out.getField(VendorFields.VENDOR_PUBLIC_IP), is("192.168.45.33"));
        assertThat(out.getField(VendorFields.VENDOR_PUBLIC_IPV6), is("0.0.0.0"));
        assertThat(out.getField(VendorFields.VENDOR_PRIVATE_IP), is("0.0.0.0"));
        assertThat(out.getField(VendorFields.VENDOR_PRIVATE_IPV6), is("0.0.0.0"));
        assertThat(out.getField(PaloAlto9xFields.PAN_GP_HOSTID), is("2c2ec970-de09-444c-b84f-2c0be75e13cd"));
        assertThat(out.getField(PaloAlto9xFields.PAN_GP_CLIENT_VERSION), is("Browser"));
        assertThat(out.getField(HostFields.HOST_TYPE), is("Windows"));
        assertThat(out.getField(HostFields.HOST_TYPE_VERSION), is("Microsoft Windows 7  Service Pack 1, 64-bit"));
        assertThat(out.getField(EventFields.EVENT_REPEAT_COUNT), is(1L));
        assertThat(out.getField(PaloAlto9xFields.PAN_GP_REASON), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_GP_ERROR), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_GP_ERROR_EXTENDED), nullValue());
        assertThat(out.getField(VendorFields.VENDOR_EVENT_ACTION), is("success"));
        assertThat(out.getField(PaloAlto9xFields.PAN_GP_LOCATION_NAME), nullValue());
        assertThat(out.getField(NetworkFields.NETWORK_TUNNEL_DURATION), is(0L));
        assertThat(out.getField(PaloAlto9xFields.PAN_GP_CONNECT_METHOD), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_GP_ERROR_CODE), is(0L));
        assertThat(out.getField(PaloAlto9xFields.PAN_GP_HOSTNAME), is("gp-portal"));
    }

    @Test
    public void verifyGlobalProtect913MessageParsing() {
        String log = "1,2020/04/01 10:49:35,015351000040055,GLOBALPROTECT,0,2305,2020/04/01 10:49:35,vsys1,portal-prelogin,before-login,,,,192.168.0.0-192.168.255.255,,192.168.45.33,0.0.0.0,0.0.0.0,0.0.0.0,2c2ec970-de09-444c-b84f-2c0be75e13cd,,Browser,Windows,\"Microsoft Windows 7  Service Pack 1, 64-bit\",1,,,\"\",success,,0,,0,gp-portal,11,0x0";
        String rawMessage = SYSLOG_PREFIX + log;
        RawMessage in = new RawMessage(rawMessage.getBytes());

        Message out = cut.decode(in);

        assertThat(out, notNullValue());
        assertThat(out.getField(Message.FIELD_FULL_MESSAGE), is(rawMessage));
        assertThat(out.getField(Message.FIELD_MESSAGE), is(log));
        assertThat(out.getField(EventFields.EVENT_SOURCE_PRODUCT), is("PAN"));

        assertThat(out.getField(EventFields.EVENT_RECEIVED_TIME), is("2020/04/01 10:49:35"));
        assertThat(out.getField(HostFields.HOST_ID), is("015351000040055"));
        assertThat(out.getField(EventFields.EVENT_LOG_NAME), is("GLOBALPROTECT"));
        assertThat(out.getField(Message.FIELD_TIMESTAMP), is("2020/04/01 10:49:35"));
        assertThat(out.getField(HostFields.HOST_VIRTFW_ID), is("vsys1"));
        assertThat(out.getField(PaloAlto9xFields.PAN_EVENT_NAME), is("portal-prelogin"));
        assertThat(out.getField(PaloAlto9xFields.PAN_TUNNEL_STAGE), is("before-login"));
        assertThat(out.getField(PaloAlto9xFields.PAN_AUTH_METHOD), nullValue());
        assertThat(out.getField(NetworkFields.NETWORK_TUNNEL_TYPE), nullValue());
        assertThat(out.getField(UserFields.USER_NAME), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_SOURCE_REGION), is("192.168.0.0-192.168.255.255"));
        assertThat(out.getField(SourceFields.SOURCE_HOSTNAME), nullValue());
        assertThat(out.getField(VendorFields.VENDOR_PUBLIC_IP), is("192.168.45.33"));
        assertThat(out.getField(VendorFields.VENDOR_PUBLIC_IPV6), is("0.0.0.0"));
        assertThat(out.getField(VendorFields.VENDOR_PRIVATE_IP), is("0.0.0.0"));
        assertThat(out.getField(VendorFields.VENDOR_PRIVATE_IPV6), is("0.0.0.0"));
        assertThat(out.getField(PaloAlto9xFields.PAN_GP_HOSTID), is("2c2ec970-de09-444c-b84f-2c0be75e13cd"));
        assertThat(out.getField(SourceFields.SOURCE_ID), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_GP_CLIENT_VERSION), is("Browser"));
        assertThat(out.getField(SourceFields.SOURCE_OS_NAME), is("Windows"));
        assertThat(out.getField(SourceFields.SOURCE_OS_VERSION), is("Microsoft Windows 7  Service Pack 1, 64-bit"));
        assertThat(out.getField(EventFields.EVENT_REPEAT_COUNT), is(1L));
        assertThat(out.getField(PaloAlto9xFields.PAN_GP_REASON), nullValue());
        assertThat(out.getField(EventFields.EVENT_ERROR_DESCRIPTION), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_GP_ERROR_EXTENDED), nullValue());
        assertThat(out.getField(VendorFields.VENDOR_EVENT_OUTCOME), is("success"));
        assertThat(out.getField(PaloAlto9xFields.PAN_GP_LOCATION_NAME), nullValue());
        assertThat(out.getField(NetworkFields.NETWORK_TUNNEL_DURATION), is(0L));
        assertThat(out.getField(PaloAlto9xFields.PAN_GP_CONNECT_METHOD), nullValue());
        assertThat(out.getField(EventFields.EVENT_ERROR_CODE), is(0L));
        assertThat(out.getField(DestinationFields.DESTINATION_HOSTNAME), is("gp-portal"));
        assertThat(out.getField(EventFields.EVENT_UID), is("11"));
        assertThat(out.getField(PaloAlto9xFields.PAN_LOG_PANORAMA), is("0x0"));
        assertThat(out.getField(PaloAlto9xFields.PAN_SELECTION_TYPE), nullValue());
        assertThat(out.getField(ApplicationFields.APPLICATION_RESPONSE_TIME), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_GATEWAY_PRIORITY), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_ATTEMPTED_GATEWAYS), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_GATEWAY), nullValue());
    }

    @Test
    public void verifyHipMatchMessageParsing() {
        String log = "0,2020/03/18 04:03:19,,HIPMATCH,0,0,2020/03/18 04:02:55,user1@prismaissase.com,vsys1,DFWMACW12KG8WL,Mac,172.1.19.3,test-Object,1,object,0,0,28,0x8600000000000000,15,18,0,0,,GP cloud service,1,0.0.0.0,4c:32:75:9a:5f:ed,hostId,MAC Address,YYYY-MM-DDThh:ss:sssTZD";
        String rawMessage = SYSLOG_PREFIX + log;
        RawMessage in = new RawMessage(rawMessage.getBytes());

        Message out = cut.decode(in);

        assertThat(out, notNullValue());
        assertThat(out.getField(Message.FIELD_FULL_MESSAGE), is(rawMessage));
        assertThat(out.getField(Message.FIELD_MESSAGE), is(log));
        assertThat(out.getField(EventFields.EVENT_SOURCE_PRODUCT), is("PAN"));

        assertThat(out.getField(EventFields.EVENT_CREATED), is("2020/03/18 04:03:19"));
        assertThat(out.getField(EventFields.EVENT_OBSERVER_UID), nullValue());
        assertThat(out.getField(EventFields.EVENT_LOG_NAME), is("HIPMATCH"));
        assertThat(out.getField(PaloAlto9xFields.PAN_LOG_SUBTYPE), is("0"));
        assertThat(out.getField(Message.FIELD_TIMESTAMP), is("2020/03/18 04:02:55"));
        assertThat(out.getField(UserFields.USER_NAME), is("user1@prismaissase.com"));
        assertThat(out.getField(HostFields.HOST_VIRTFW_ID), is("vsys1"));
        assertThat(out.getField(HostFields.HOST_HOSTNAME), is("DFWMACW12KG8WL"));
        assertThat(out.getField(HostFields.HOST_TYPE), is("Mac"));
        assertThat(out.getField(HostFields.HOST_IP), is("172.1.19.3"));
        assertThat(out.getField(PaloAlto9xFields.PAN_HIP), is("test-Object"));
        assertThat(out.getField(EventFields.EVENT_REPEAT_COUNT), is(1L));
        assertThat(out.getField(PaloAlto9xFields.PAN_HIP_TYPE), is("object"));
        assertThat(out.getField(EventFields.EVENT_UID), is("28"));
        assertThat(out.getField(PaloAlto9xFields.PAN_LOG_PANORAMA), is("0x8600000000000000"));
        assertThat(out.getField(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_1), is(15L));
        assertThat(out.getField(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_2), is(18L));
        assertThat(out.getField(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_3), is(0L));
        assertThat(out.getField(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_4), is(0L));
        assertThat(out.getField(HostFields.HOST_VIRTFW_HOSTNAME), nullValue());
        assertThat(out.getField(EventFields.EVENT_OBSERVER_HOSTNAME), is("GP cloud service"));
        assertThat(out.getField(HostFields.HOST_VIRTFW_UID), is("1"));
        assertThat(out.getField(HostFields.HOST_IPV6), is("0.0.0.0"));
        assertThat(out.getField(PaloAlto9xFields.PAN_GP_HOSTID), is("4c:32:75:9a:5f:ed"));
        assertThat(out.getField(HostFields.HOST_ID), is("hostId"));
        assertThat(out.getField(SourceFields.SOURCE_MAC), is("MAC Address"));
        assertThat(out.getField(PaloAlto9xFields.PAN_HIGH_RES_TIME), is("YYYY-MM-DDThh:ss:sssTZD"));
    }

    @Test
    public void verifySystemMessageParsing() {
        String log = "1,2020/03/19 10:12:57,007000016479,SYSTEM,general,0,2020/03/19 10:12:57,,general,,0,0,general,informational,\"Failed to connect to address: (null) port: 3978, conn id: triallr-(null)-2-192.168.1.232\",21682381,0x8000000000000000,0,0,0,0,,sg2,YYYY-MM-DDThh:ss:sssTZD";
        String rawMessage = SYSLOG_PREFIX + log;
        RawMessage in = new RawMessage(rawMessage.getBytes());

        Message out = cut.decode(in);

        assertThat(out, notNullValue());
        assertThat(out.getField(Message.FIELD_FULL_MESSAGE), is(rawMessage));
        assertThat(out.getField(Message.FIELD_MESSAGE), is(log));
        assertThat(out.getField(EventFields.EVENT_SOURCE_PRODUCT), is("PAN"));
        
        assertThat(out.getField(EventFields.EVENT_CREATED), is("2020/03/19 10:12:57"));
        assertThat(out.getField(HostFields.HOST_ID), is("007000016479"));
        assertThat(out.getField(EventFields.EVENT_LOG_NAME), is("SYSTEM"));
        assertThat(out.getField(PaloAlto9xFields.PAN_LOG_SUBTYPE), is("general"));
        assertThat(out.getField(Message.FIELD_TIMESTAMP), is("2020/03/19 10:12:57"));
        assertThat(out.getField(HostFields.HOST_VIRTFW_ID), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_EVENT_NAME), is("general"));
        assertThat(out.getField(PaloAlto9xFields.PAN_EVENT_OBJECT), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_MODULE), is("general"));
        assertThat(out.getField(EventFields.EVENT_SEVERITY), is("informational"));
        assertThat(out.getField(VendorFields.VENDOR_EVENT_DESCRIPTION), is("Failed to connect to address: (null) port: 3978, conn id: triallr-(null)-2-192.168.1.232"));
        assertThat(out.getField(EventFields.EVENT_UID), is("21682381"));
        assertThat(out.getField(PaloAlto9xFields.PAN_LOG_PANORAMA), is("0x8000000000000000"));
        assertThat(out.getField(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_1), is(0L));
        assertThat(out.getField(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_2), is(0L));
        assertThat(out.getField(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_3), is(0L));
        assertThat(out.getField(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_4), is(0L));
        assertThat(out.getField(HostFields.HOST_VIRTFW_HOSTNAME), nullValue());
        assertThat(out.getField(HostFields.HOST_HOSTNAME), is("sg2"));
        assertThat(out.getField(PaloAlto9xFields.PAN_HIGH_RES_TIME), is("YYYY-MM-DDThh:ss:sssTZD"));
    }

    @Test
    public void verifyThreatMessageParsing() {
        String log = "1,2020/05/19 07:37:27,007200002536,THREAT,spyware,2305,2020/05/19 07:37:27,10.154.229.167,190.253.254.254,,,General Business Apps,pancademo\\andy.miller,,unknown-udp,vsys1,L3-TAP,L3-TAP,ethernet1/2,ethernet1/2,,2020/05/19 07:37:27,70860,1,1111,16471,0,0,0x80002000,udp,drop,\"\",ZeroAccess.Gen Command and Control Traffic(13235),any,critical,client-to-server,6241468001,0x2000000000000000,10.0.0.0-10.255.255.255,Colombia,0,,1206236073597030482,,,0,,,,,,,,0,31,12,0,0,,us1,,,,,0,,0,,N/A,botnet,AppThreat-8270-6076,0x0,0,4294967295,,,f0724261-cf8b-479b-8208-fd3c7ac3af0b,0,";
        String rawMessage = SYSLOG_PREFIX + log;
        RawMessage in = new RawMessage(rawMessage.getBytes());

        Message out = cut.decode(in);

        assertThat(out, notNullValue());
        assertThat(out.getField(Message.FIELD_FULL_MESSAGE), is(rawMessage));
        assertThat(out.getField(Message.FIELD_MESSAGE), is(log));
        assertThat(out.getField(EventFields.EVENT_SOURCE_PRODUCT), is("PAN"));

        // Field 0 is FUTURE USE
        assertThat(out.getField(EventFields.EVENT_RECEIVED_TIME), is("2020/05/19 07:37:27"));
        assertThat(out.getField(EventFields.EVENT_OBSERVER_ID), is("007200002536"));
        assertThat(out.getField(EventFields.EVENT_LOG_NAME), is("THREAT"));
        assertThat(out.getField(PaloAlto9xFields.PAN_LOG_SUBTYPE), is("spyware"));

        // Field 5 is FUTURE USE
        assertThat(out.getField(Message.FIELD_TIMESTAMP), is("2020/05/19 07:37:27"));
        assertThat(out.getField(SourceFields.SOURCE_IP), is("10.154.229.167"));
        assertThat(out.getField(DestinationFields.DESTINATION_IP), is("190.253.254.254"));
        assertThat(out.getField(SourceFields.SOURCE_NAT_IP), nullValue());

        assertThat(out.getField(DestinationFields.DESTINATION_NAT_IP), nullValue());
        assertThat(out.getField(RuleFields.RULE_NAME), is("General Business Apps"));
        assertThat(out.getField(SourceFields.SOURCE_USER_NAME), is("pancademo\\andy.miller"));
        assertThat(out.getField(DestinationFields.DESTINATION_USER_NAME), nullValue());
        assertThat(out.getField(ApplicationFields.APPLICATION_NAME), is("unknown-udp"));

        assertThat(out.getField(HostFields.HOST_VIRTFW_ID), is("vsys1"));
        assertThat(out.getField(SourceFields.SOURCE_ZONE), is("L3-TAP"));
        assertThat(out.getField(DestinationFields.DESTINATION_ZONE), is("L3-TAP"));
        assertThat(out.getField(NetworkFields.NETWORK_INTERFACE_IN), is("ethernet1/2"));
        assertThat(out.getField(NetworkFields.NETWORK_INTERFACE_OUT), is("ethernet1/2"));

        assertThat(out.getField(PaloAlto9xFields.PAN_LOG_ACITON), nullValue());
        // Field 21 is FUTURE USE
        assertThat(out.getField(SessionFields.SESSION_ID), is(70860L));
        assertThat(out.getField(EventFields.EVENT_REPEAT_COUNT), is(1L));
        assertThat(out.getField(SourceFields.SOURCE_PORT), is(1111L));

        assertThat(out.getField(DestinationFields.DESTINATION_PORT), is(16471L));
        assertThat(out.getField(SourceFields.SOURCE_NAT_PORT), is(0L));
        assertThat(out.getField(DestinationFields.DESTINATION_NAT_PORT), is(0L));
        assertThat(out.getField(PaloAlto9xFields.PAN_FLAGS), is("0x80002000"));
        assertThat(out.getField(NetworkFields.NETWORK_TRANSPORT), is("udp"));

        assertThat(out.getField(VendorFields.VENDOR_EVENT_ACTION), is("drop"));
        assertThat(out.getField(AlertFields.ALERT_INDICATOR), nullValue());
        assertThat(out.getField(AlertFields.ALERT_SIGNATURE), is("ZeroAccess.Gen Command and Control Traffic(13235)"));

        assertThat(out.getField(AlertFields.ALERT_CATEGORY), notNullValue());
        List<String> alertCategoryList = (List) out.getField(AlertFields.ALERT_CATEGORY);
        assertThat(alertCategoryList.size(), is(2));
        assertThat(alertCategoryList, hasItems("any", "botnet"));

        assertThat(out.getField(VendorFields.VENDOR_ALERT_SEVERITY), is("critical"));

        assertThat(out.getField(PaloAlto9xFields.PAN_ALERT_DIRECTION), is("client-to-server"));
        assertThat(out.getField(EventFields.EVENT_UID), is("6241468001"));
        assertThat(out.getField(PaloAlto9xFields.PAN_LOG_PANORAMA), is("0x2000000000000000"));
        assertThat(out.getField(SourceFields.SOURCE_LOCATION_NAME), is("10.0.0.0-10.255.255.255"));
        assertThat(out.getField(DestinationFields.DESTINATION_LOCATION_NAME), is("Colombia"));

        // Field 40 is FUTURE USE
        assertThat(out.getField(HttpFields.HTTP_CONTENT_TYPE), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_PCAP_ID), is("1206236073597030482"));
        assertThat(out.getField(PaloAlto9xFields.PAN_WILDFIRE_HASH), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_CLOUD_HOSTNAME), nullValue());

        assertThat(out.getField(PaloAlto9xFields.PAN_URL_INDEX), is(0L));
        assertThat(out.getField(HttpFields.HTTP_USER_AGENT_NAME), nullValue());
        assertThat(out.getField(FileFields.FILE_TYPE), nullValue());
        assertThat(out.getField(HttpFields.HTTP_XFF), nullValue());
        assertThat(out.getField(HttpFields.HTTP_REFERER), nullValue());

        assertThat(out.getField(SourceFields.SOURCE_USER_EMAIL), nullValue());
        assertThat(out.getField(EmailFields.EMAIL_SUBJECT), nullValue());
        assertThat(out.getField(UserFields.TARGET_USER_EMAIL), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_WILDFIRE_REPORT_ID), is(0L));
        assertThat(out.getField(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_1), is(31L));

        assertThat(out.getField(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_2), is(12L));
        assertThat(out.getField(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_3), is(0L));
        assertThat(out.getField(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_4), is(0L));
        assertThat(out.getField(HostFields.HOST_VIRTFW_HOSTNAME), nullValue());
        assertThat(out.getField(EventFields.EVENT_OBSERVER_HOSTNAME), is("us1"));

        // Field 60 is FUTURE USE
        assertThat(out.getField(SourceFields.SOURCE_VSYS_UUID), nullValue());
        assertThat(out.getField(DestinationFields.DESTINATION_VSYS_UUID), nullValue());
        assertThat(out.getField(HttpFields.HTTP_METHOD), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_TUNNEL_ID), is("0"));

        assertThat(out.getField(PaloAlto9xFields.PAN_MONITOR_TAG), is(0L));
        assertThat(out.getField(PaloAlto9xFields.PAN_PARENT_SESSION_ID), is("0"));
        assertThat(out.getField(PaloAlto9xFields.PAN_PARENT_START_TIME), nullValue());
        assertThat(out.getField(NetworkFields.NETWORK_TUNNEL_TYPE), is("N/A"));

        assertThat(out.getField(AlertFields.ALERT_DEFINITIONS_VERSION), is("AppThreat-8270-6076"));
        // Field 71 is FUTURE USE
        assertThat(out.getField(PaloAlto9xFields.PAN_ASSOC_ID), is(0L));
        assertThat(out.getField(PaloAlto9xFields.PAN_PPID), is(4294967295L));
        assertThat(out.getField(HttpFields.HTTP_HEADERS), nullValue());

        assertThat(out.getField(HttpFields.HTTP_URI_CATEGORY), nullValue());
        assertThat(out.getField(PolicyFields.POLICY_UID), is("f0724261-cf8b-479b-8208-fd3c7ac3af0b"));
        assertThat(out.getField(PaloAlto9xFields.PAN_HTTP2), is("0"));
        assertThat(out.getField(PaloAlto9xFields.PAN_DYNUSERGROUP_NAME), nullValue());
        assertThat(out.getField(HttpFields.HTTP_XFF), nullValue());

        assertThat(out.getField(SourceFields.SOURCE_CATEGORY), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_SOURCE_PROFILE), nullValue());
        assertThat(out.getField(SourceFields.SOURCE_DEVICE_MODEL), nullValue());
        assertThat(out.getField(SourceFields.SOURCE_DEVICE_VENDOR), nullValue());
        assertThat(out.getField(SourceFields.SOURCE_OS_NAME), nullValue());

        assertThat(out.getField(SourceFields.SOURCE_OS_VERSION), nullValue());
        assertThat(out.getField(SourceFields.SOURCE_HOSTNAME), nullValue());
        assertThat(out.getField(SourceFields.SOURCE_MAC), nullValue());
        assertThat(out.getField(DestinationFields.DESTINATION_CATEGORY), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_DESTINATION_PROFILE), nullValue());

        assertThat(out.getField(DestinationFields.DESTINATION_DEVICE_MODEL), nullValue());
        assertThat(out.getField(DestinationFields.DESTINATION_DEVICE_VENDOR), nullValue());
        assertThat(out.getField(DestinationFields.DESTINATION_OS_NAME), nullValue());
        assertThat(out.getField(DestinationFields.DESTINATION_OS_VERSION), nullValue());
        assertThat(out.getField(DestinationFields.DESTINATION_HOSTNAME), nullValue());

        assertThat(out.getField(DestinationFields.DESTINATION_MAC), nullValue());
        assertThat(out.getField(ContainerFields.CONTAINER_ID), nullValue());
        assertThat(out.getField(ContainerFields.CONTAINER_NAMESPACE), nullValue());
        assertThat(out.getField(ContainerFields.CONTAINER_NAME), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_SRC_EDL), nullValue());

        assertThat(out.getField(PaloAlto9xFields.PAN_DST_EDL), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_HOST_ID), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_HOST_SN), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_DOMAIN_EDL), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_SRC_DAG), nullValue());

        assertThat(out.getField(PaloAlto9xFields.PAN_DST_DAG), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_PARTIAL_HASH), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_HIGH_RES_TIME), nullValue());
        assertThat(out.getField(VendorFields.VENDOR_EVENT_OUTCOME_REASON), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_EVENT_JUSTIFICATION), nullValue());

        assertThat(out.getField(PaloAlto9xFields.PAN_NSDSAI_SST), nullValue());
    }

    @Test
    public void verifyThreatMessageParsing_withRepeatedSameXFF() {
        String log = "1,2020/05/19 07:37:27,007200002536,THREAT,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,FOO,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,FOO,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,";
        String rawMessage = SYSLOG_PREFIX + log;
        RawMessage in = new RawMessage(rawMessage.getBytes());

        Message out = cut.decode(in);

        assertThat(out, notNullValue());
        assertThat(out.getField(Message.FIELD_FULL_MESSAGE), is(rawMessage));
        assertThat(out.getField(Message.FIELD_MESSAGE), is(log));
        assertThat(out.getField(EventFields.EVENT_SOURCE_PRODUCT), is("PAN"));

        assertThat(out.getField(HttpFields.HTTP_XFF), is("FOO"));
    }

    @Test
    public void verifyThreatMessageParsing_withDifferentXFF() {
        String log = "1,2020/05/19 07:37:27,007200002536,THREAT,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,FOO,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,BAR,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,";
        String rawMessage = SYSLOG_PREFIX + log;
        RawMessage in = new RawMessage(rawMessage.getBytes());

        Message out = cut.decode(in);

        assertThat(out, notNullValue());
        assertThat(out.getField(Message.FIELD_FULL_MESSAGE), is(rawMessage));
        assertThat(out.getField(Message.FIELD_MESSAGE), is(log));
        assertThat(out.getField(EventFields.EVENT_SOURCE_PRODUCT), is("PAN"));

        assertThat(out.getField(HttpFields.HTTP_XFF), notNullValue());
        List<String> xffList = (List) out.getField(HttpFields.HTTP_XFF);
        assertThat(xffList.size(), is(2));
        assertThat(xffList, hasItems("FOO", "BAR"));
    }

    @Test
    public void verifyTrafficMessageParsing() {
        String log = "1,2020/05/19 07:34:54,007200002536,TRAFFIC,end,2305,2020/05/19 07:34:54,10.154.172.134,151.151.88.132,,,IT Sanctioned SaaS Apps-443,pancademo\\steven.reid,,ssl,vsys1,L3-TAP,L3-TAP,ethernet1/2,ethernet1/2,,2020/05/19 07:34:54,33903,1,57090,443,0,0,0x6c,tcp,allow,6802,3876,2926,26,2020/05/19 07:32:48,97,financial-services,0,18621234943,0x0,10.0.0.0-10.255.255.255,United States,0,17,9,tcp-rst-from-server,31,12,0,0,,us1,from-policy,,,0,,0,,N/A,0,0,0,0,30468339-a760-46b2-b80b-ee873e6d11e4,0,0,,,,,,,";
        String rawMessage = SYSLOG_PREFIX + log;
        RawMessage in = new RawMessage(rawMessage.getBytes());

        Message out = cut.decode(in);

        assertThat(out, notNullValue());
        assertThat(out.getField(Message.FIELD_FULL_MESSAGE), is(rawMessage));
        assertThat(out.getField(Message.FIELD_MESSAGE), is(log));
        assertThat(out.getField(EventFields.EVENT_SOURCE_PRODUCT), is("PAN"));

        // Field 0 is FUTURE USE
        assertThat(out.getField(EventFields.EVENT_RECEIVED_TIME), is("2020/05/19 07:34:54"));
        assertThat(out.getField(EventFields.EVENT_OBSERVER_ID), is("007200002536"));
        assertThat(out.getField(EventFields.EVENT_LOG_NAME), is("TRAFFIC"));
        assertThat(out.getField(PaloAlto9xFields.PAN_LOG_SUBTYPE), is("end"));

        // Field 5 is FUTURE USE
        assertThat(out.getField(Message.FIELD_TIMESTAMP), is("2020/05/19 07:34:54"));
        assertThat(out.getField(SourceFields.SOURCE_IP), is("10.154.172.134"));
        assertThat(out.getField(DestinationFields.DESTINATION_IP), is("151.151.88.132"));
        assertThat(out.getField(SourceFields.SOURCE_NAT_IP), nullValue());

        assertThat(out.getField(DestinationFields.DESTINATION_NAT_IP), nullValue());
        assertThat(out.getField(RuleFields.RULE_NAME), is("IT Sanctioned SaaS Apps-443")); // TODO: Need constant
        assertThat(out.getField(SourceFields.SOURCE_USER_NAME), is("pancademo\\steven.reid"));
        assertThat(out.getField(DestinationFields.DESTINATION_USER_NAME), nullValue());
        assertThat(out.getField(ApplicationFields.APPLICATION_NAME), is("ssl"));

        assertThat(out.getField(HostFields.HOST_VIRTFW_ID), is("vsys1"));
        assertThat(out.getField(SourceFields.SOURCE_ZONE), is("L3-TAP"));
        assertThat(out.getField(DestinationFields.DESTINATION_ZONE), is("L3-TAP"));
        assertThat(out.getField(NetworkFields.NETWORK_INTERFACE_IN), is("ethernet1/2"));
        assertThat(out.getField(NetworkFields.NETWORK_INTERFACE_OUT), is("ethernet1/2"));

        assertThat(out.getField(PaloAlto9xFields.PAN_LOG_ACITON), nullValue());
        // Field 21 is FUTURE USE
        assertThat(out.getField(SessionFields.SESSION_ID), is(33903L));
        assertThat(out.getField(EventFields.EVENT_REPEAT_COUNT), is(1L));
        assertThat(out.getField(SourceFields.SOURCE_PORT), is(57090L));

        assertThat(out.getField(DestinationFields.DESTINATION_PORT), is(443L));
        assertThat(out.getField(SourceFields.SOURCE_NAT_PORT), is(0L));
        assertThat(out.getField(DestinationFields.DESTINATION_NAT_PORT), is(0L));
        assertThat(out.getField(PaloAlto9xFields.PAN_FLAGS), is("0x6c"));
        assertThat(out.getField(NetworkFields.NETWORK_TRANSPORT), is("tcp"));

        assertThat(out.getField(VendorFields.VENDOR_EVENT_ACTION), is("allow"));
        assertThat(out.getField(NetworkFields.NETWORK_BYTES), is(6802L));
        assertThat(out.getField(SourceFields.SOURCE_BYTES_SENT), is(3876L));
        assertThat(out.getField(DestinationFields.DESTINATION_BYTES_SENT), is(2926L));
        assertThat(out.getField(NetworkFields.NETWORK_PACKETS), is(26L));

        assertThat(out.getField(EventFields.EVENT_START), is("2020/05/19 07:32:48"));
        assertThat(out.getField(EventFields.EVENT_DURATION), is(97L));
        assertThat(out.getField(HttpFields.HTTP_URI_CATEGORY), is("financial-services"));
        // Field 38 is FUTURE USE
        assertThat(out.getField(EventFields.EVENT_UID), is("18621234943"));

        assertThat(out.getField(PaloAlto9xFields.PAN_LOG_PANORAMA), is("0x0"));
        assertThat(out.getField(SourceFields.SOURCE_LOCATION_NAME), is("10.0.0.0-10.255.255.255"));
        assertThat(out.getField(DestinationFields.DESTINATION_LOCATION_NAME), is("United States"));
        // Field 43 is FUTURE USE
        assertThat(out.getField(SourceFields.SOURCE_PACKETS_SENT), is(17L));

        assertThat(out.getField(DestinationFields.DESTINATION_PACKETS_SENT), is(9L));
        assertThat(out.getField(PaloAlto9xFields.PAN_SESSION_END_REASON), is("tcp-rst-from-server"));
        assertThat(out.getField(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_1), is(31L));
        assertThat(out.getField(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_2), is(12L));
        assertThat(out.getField(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_3), is(0L));

        assertThat(out.getField(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_4), is(0L));
        assertThat(out.getField(HostFields.HOST_VIRTFW_HOSTNAME), nullValue());
        assertThat(out.getField(EventFields.EVENT_OBSERVER_HOSTNAME), is("us1"));
        assertThat(out.getField(VendorFields.VENDOR_EVENT_DESCRIPTION), is("from-policy"));
        assertThat(out.getField(SourceFields.SOURCE_VSYS_UUID), nullValue());

        assertThat(out.getField(DestinationFields.DESTINATION_VSYS_UUID), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_TUNNEL_ID), is("0"));
        assertThat(out.getField(PaloAlto9xFields.PAN_MONITOR_TAG), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_PARENT_SESSION_ID), is("0"));
        assertThat(out.getField(PaloAlto9xFields.PAN_PARENT_START_TIME), nullValue());

        assertThat(out.getField(NetworkFields.NETWORK_TUNNEL_TYPE), is("N/A"));
        assertThat(out.getField(PaloAlto9xFields.PAN_ASSOC_ID), is(0L));
        assertThat(out.getField(PaloAlto9xFields.PAN_SCTP_CHUNKS_SUM), is("0"));
        assertThat(out.getField(PaloAlto9xFields.PAN_SCTP_CHUNKS_TX), is("0"));
        assertThat(out.getField(PaloAlto9xFields.PAN_SCTP_CHUNKS_RX), is("0"));

        assertThat(out.getField(PolicyFields.POLICY_UID), is("30468339-a760-46b2-b80b-ee873e6d11e4"));
        assertThat(out.getField(PaloAlto9xFields.PAN_HTTP2), is("0"));
        assertThat(out.getField(PaloAlto9xFields.PAN_LINK_CHANGES), is(0L));
        assertThat(out.getField(PaloAlto9xFields.PAN_SDWAN_POLICY_ID), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_LINK_SWITCHES), nullValue());

        assertThat(out.getField(PaloAlto9xFields.PAN_SDWAN_CLUSTER), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_SDWAN_DEVICE_TYPE), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_SDWAN_CLUSTER_TYPE), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_SDWAN_SITE_NAME), nullValue());
        assertThat(out.getField(PaloAlto9xFields.PAN_DYNUSERGROUP_NAME), nullValue());

         assertThat(out.getField(HttpFields.HTTP_XFF), nullValue());
         assertThat(out.getField(SourceFields.SOURCE_CATEGORY), nullValue());
         assertThat(out.getField(PaloAlto9xFields.PAN_SOURCE_PROFILE), nullValue());
         assertThat(out.getField(SourceFields.SOURCE_DEVICE_MODEL), nullValue());
         assertThat(out.getField(SourceFields.SOURCE_DEVICE_VENDOR), nullValue());

         assertThat(out.getField(SourceFields.SOURCE_OS_NAME), nullValue());
         assertThat(out.getField(SourceFields.SOURCE_OS_VERSION), nullValue());
         assertThat(out.getField(SourceFields.SOURCE_HOSTNAME), nullValue());
         assertThat(out.getField(SourceFields.SOURCE_MAC), nullValue());
         assertThat(out.getField(DestinationFields.DESTINATION_CATEGORY), nullValue());

         assertThat(out.getField(PaloAlto9xFields.PAN_DESTINATION_PROFILE), nullValue());
         assertThat(out.getField(DestinationFields.DESTINATION_DEVICE_MODEL), nullValue());
         assertThat(out.getField(DestinationFields.DESTINATION_DEVICE_VENDOR), nullValue());
         assertThat(out.getField(DestinationFields.DESTINATION_OS_NAME), nullValue());
         assertThat(out.getField(DestinationFields.DESTINATION_OS_VERSION), nullValue());

         assertThat(out.getField(DestinationFields.DESTINATION_HOSTNAME), nullValue());
         assertThat(out.getField(DestinationFields.DESTINATION_MAC), nullValue());
         assertThat(out.getField(ContainerFields.CONTAINER_ID), nullValue());
         assertThat(out.getField(ContainerFields.CONTAINER_NAMESPACE), nullValue());
         assertThat(out.getField(ContainerFields.CONTAINER_NAME), nullValue());

         assertThat(out.getField(PaloAlto9xFields.PAN_SRC_EDL), nullValue());
         assertThat(out.getField(PaloAlto9xFields.PAN_DST_EDL), nullValue());
         assertThat(out.getField(PaloAlto9xFields.PAN_HOST_ID), nullValue());
         assertThat(out.getField(PaloAlto9xFields.PAN_HOST_SN), nullValue());
         assertThat(out.getField(PaloAlto9xFields.PAN_SRC_DAG), nullValue());

         assertThat(out.getField(PaloAlto9xFields.PAN_DST_DAG), nullValue());
         assertThat(out.getField(PaloAlto9xFields.PAN_SESSION_OWNER), nullValue());
         assertThat(out.getField(PaloAlto9xFields.PAN_HIGH_RES_TIME), nullValue());
         assertThat(out.getField(PaloAlto9xFields.PAN_NSDSAI_SST), nullValue());
         assertThat(out.getField(PaloAlto9xFields.PAN_NSDSAI_SD), nullValue());
    }
}
