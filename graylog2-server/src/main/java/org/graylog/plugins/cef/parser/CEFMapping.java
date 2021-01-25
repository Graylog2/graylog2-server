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

import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.Map;
import java.util.function.Function;

@SuppressWarnings("unused")
public enum CEFMapping {
    // CEF Key Names For Event Producers
    act("act", "deviceAction", CEFMapping::convertString),
    app("app", "applicationProtocol", CEFMapping::convertString),
    c6a1("c6a1", "deviceCustomIPv6Address1", CEFMapping::convertIPv6Address),
    c6a1Label("c6a1Label", "deviceCustomIPv6Address1Label", CEFMapping::convertString),
    c6a2("c6a2", "deviceCustomIPv6Address2", CEFMapping::convertIPv6Address),
    c6a2Label("c6a2Label", "deviceCustomIPv6Address2Label", CEFMapping::convertString),
    c6a3("c6a3", "deviceCustomIPv6Address3", CEFMapping::convertIPv6Address),
    c6a3Label("c6a3Label", "deviceCustomIPv6Address3Label", CEFMapping::convertString),
    c6a4("c6a4", "deviceCustomIPv6Address4", CEFMapping::convertIPv6Address),
    c6a4Label("c6a4Label", "deviceCustomIPv6Address4Label", CEFMapping::convertString),
    cfp1("cfp1", "deviceCustomFloatingPoint1", CEFMapping::convertFloat),
    cfp1Label("cfp1Label", "deviceCustomFloatingPoint1Label", CEFMapping::convertString),
    cfp2("cfp2", "deviceCustomFloatingPoint2", CEFMapping::convertFloat),
    cfp2Label("cfp2Label", "deviceCustomFloatingPoint2Label", CEFMapping::convertString),
    cfp3("cfp3", "deviceCustomFloatingPoint3", CEFMapping::convertFloat),
    cfp3Label("cfp3Label", "deviceCustomFloatingPoint3Label", CEFMapping::convertString),
    cfp4("cfp4", "deviceCustomFloatingPoint4", CEFMapping::convertFloat),
    cfp4Label("cfp4Label", "deviceCustomFloatingPoint4Label", CEFMapping::convertString),
    cn1("cn1", "deviceCustomNumber1", CEFMapping::convertBigInteger),
    cn1Label("cn1Label", "deviceCustomNumber1Label", CEFMapping::convertString),
    cn2("cn2", "deviceCustomNumber2", CEFMapping::convertBigInteger),
    cn2Label("cn2Label", "deviceCustomNumber2Label", CEFMapping::convertString),
    cn3("cn3", "deviceCustomNumber3", CEFMapping::convertBigInteger),
    cn3Label("cn3Label", "deviceCustomNumber3Label", CEFMapping::convertString),
    cn4("cn4", "deviceCustomNumber4", CEFMapping::convertBigInteger),
    cn4Label("cn4Label", "deviceCustomNumber4Label", CEFMapping::convertString),
    cnt("cnt", "baseEventCount", CEFMapping::convertInteger),
    cs1("cs1", "deviceCustomString1", CEFMapping::convertString),
    cs1Label("cs1Label", "deviceCustomString1Label", CEFMapping::convertString),
    cs2("cs2", "deviceCustomString2", CEFMapping::convertString),
    cs2Label("cs2Label", "deviceCustomString2Label", CEFMapping::convertString),
    cs3("cs3", "deviceCustomString3", CEFMapping::convertString),
    cs3Label("cs3Label", "deviceCustomString3Label", CEFMapping::convertString),
    cs4("cs4", "deviceCustomString4", CEFMapping::convertString),
    cs4Label("cs4Label", "deviceCustomString4Label", CEFMapping::convertString),
    cs5("cs5", "deviceCustomString5", CEFMapping::convertString),
    cs5Label("cs5Label", "deviceCustomString5Label", CEFMapping::convertString),
    cs6("cs6", "deviceCustomString6", CEFMapping::convertString),
    cs6Label("cs6Label", "deviceCustomString6Label", CEFMapping::convertString),
    destinationDnsDomain("destinationDnsDomain", "destinationDnsDomain", CEFMapping::convertString),
    destinationServiceName("destinationServiceName", "destinationServiceName", CEFMapping::convertString),
    destinationTranslatedAddress("destinationTranslatedAddress", "destinationTranslatedAddress", CEFMapping::convertIPv4Address),
    destinationTranslatedPort("destinationTranslatedPort", "destinationTranslatedPort", CEFMapping::convertInteger),
    deviceCustomDate1("deviceCustomDate1", "deviceCustomDate1", CEFMapping::convertTimestamp),
    deviceCustomDate1Label("deviceCustomDate1Label", "deviceCustomDate1Label", CEFMapping::convertString),
    deviceCustomDate2("deviceCustomDate2", "deviceCustomDate2", CEFMapping::convertTimestamp),
    deviceCustomDate2Label("deviceCustomDate2Label", "deviceCustomDate2Label", CEFMapping::convertString),
    deviceDirection("deviceDirection", "deviceDirection", CEFMapping::convertDirection),
    deviceDnsDomain("deviceDnsDomain", "deviceDnsDomain", CEFMapping::convertString),
    deviceExternalId("deviceExternalId", "deviceExternalId", CEFMapping::convertString),
    deviceFacility("deviceFacility", "deviceFacility", CEFMapping::convertString),
    deviceInboundInterface("deviceInboundInterface", "deviceInboundInterface", CEFMapping::convertString),
    deviceNtDomain("deviceNtDomain", "deviceNtDomain", CEFMapping::convertString),
    DeviceOutboundInterface("deviceOutboundInterface", "deviceOutboundInterface", CEFMapping::convertString),
    DevicePayloadId("devicePayloadId", "devicePayloadId", CEFMapping::convertString),
    deviceProcessName("deviceProcessName", "deviceProcessName", CEFMapping::convertString),
    deviceTranslatedAddress("deviceTranslatedAddress", "deviceTranslatedAddress", CEFMapping::convertIPv4Address),
    dhost("dhost", "destinationHostName", CEFMapping::convertString),
    dmac("dmac", "destinationMacAddress", CEFMapping::convertMacAddress),
    dntdom("dntdom", "destinationNtDomain", CEFMapping::convertString),
    dpid("dpid", "destinationProcessId", CEFMapping::convertInteger),
    dpriv("dpriv", "destinationUserPrivileges", CEFMapping::convertString),
    dproc("dproc", "destinationProcessName", CEFMapping::convertString),
    dpt("dpt", "destinationPort", CEFMapping::convertInteger),
    dst("dst", "destinationAddress", CEFMapping::convertIPv4Address),
    dtz("dtz", "deviceTimeZone", CEFMapping::convertString),
    duid("duid", "destinationUserId", CEFMapping::convertString),
    duser("duser", "destinationUserName", CEFMapping::convertString),
    dvc("dvc", "deviceAddress", CEFMapping::convertIPv4Address),
    dvchost("dvchost", "deviceHostName", CEFMapping::convertString),
    dvcmac("dvcmac", "deviceMacAddress", CEFMapping::convertMacAddress),
    dvcpid("dvcpid", "deviceProcessId", CEFMapping::convertInteger),
    end("end", "endTime", CEFMapping::convertTimestamp),
    externalId("externalId", "externalId", CEFMapping::convertString),
    fileCreateTime("fileCreateTime", "fileCreateTime", CEFMapping::convertTimestamp),
    fileHash("fileHash", "fileHash", CEFMapping::convertString),
    fileId("fileId", "fileId", CEFMapping::convertString),
    fileModificationTime("fileModificationTime", "fileModificationTime", CEFMapping::convertTimestamp),
    filePath("filePath", "filePath", CEFMapping::convertString),
    filePermission("filePermission", "filePermission", CEFMapping::convertString),
    fileType("fileType", "fileType", CEFMapping::convertString),
    flexDate1("flexDate1", "flexDate1", CEFMapping::convertTimestamp),
    flexDate1Label("flexDate1Label", "flexDate1Label", CEFMapping::convertString),
    flexDate2("flexDate2", "flexDate2", CEFMapping::convertTimestamp),
    flexDate2Label("flexDate2Label", "flexDate2Label", CEFMapping::convertString),
    flexDate3("flexDate3", "flexDate3", CEFMapping::convertTimestamp),
    flexDate3Label("flexDate3Label", "flexDate3Label", CEFMapping::convertString),
    flexDate4("flexDate4", "flexDate4", CEFMapping::convertTimestamp),
    flexDate4Label("flexDate4Label", "flexDate4Label", CEFMapping::convertString),
    flexString1("flexString1", "flexString1", CEFMapping::convertString),
    flexString1Label("flexString1Label", "flexString1Label", CEFMapping::convertString),
    flexString2("flexString2", "flexString2", CEFMapping::convertString),
    flexString2Label("flexString2Label", "flexString2Label", CEFMapping::convertString),
    flexString3("flexString3", "flexString3", CEFMapping::convertString),
    flexString3Label("flexString3Label", "flexString3Label", CEFMapping::convertString),
    flexString4("flexString4", "flexString4", CEFMapping::convertString),
    flexString4Label("flexString4Label", "flexString4Label", CEFMapping::convertString),
    flexNumber1("flexNumber1", "flexNumber1", CEFMapping::convertLong),
    flexNumber1Label("flexNumber1Label", "flexNumber1Label", CEFMapping::convertString),
    flexNumber2("flexNumber2", "flexNumber2", CEFMapping::convertLong),
    flexNumber2Label("flexNumber2Label", "flexNumber2Label", CEFMapping::convertString),
    flexNumber3("flexNumber3", "flexNumber3", CEFMapping::convertLong),
    flexNumber3Label("flexNumber3Label", "flexNumber3Label", CEFMapping::convertString),
    flexNumber4("flexNumber4", "flexNumber4", CEFMapping::convertLong),
    flexNumber4Label("flexNumber4Label", "flexNumber4Label", CEFMapping::convertString),
    fname("fname", "filename", CEFMapping::convertString),
    fsize("fsize", "fileSize", CEFMapping::convertInteger),
    in("in", "bytesIn", CEFMapping::convertInteger),
    msg("msg", "message", CEFMapping::convertString),
    oldFileCreateTime("oldFileCreateTime", "oldFileCreateTime", CEFMapping::convertTimestamp),
    oldFileHash("oldFileHash", "oldFileHash", CEFMapping::convertString),
    oldFileId("oldFileId", "oldFileId", CEFMapping::convertString),
    oldFileModificationTime("oldFileModificationTime", "oldFileModificationTime", CEFMapping::convertTimestamp),
    oldFileName("oldFileName", "oldFileName", CEFMapping::convertString),
    oldFilePath("oldFilePath", "oldFilePath", CEFMapping::convertString),
    oldFilePermission("oldFilePermission", "oldFilePermission", CEFMapping::convertString),
    oldFileSize("oldFileSize", "oldFileSize", CEFMapping::convertInteger),
    oldFileType("oldFileType", "oldFileType", CEFMapping::convertString),
    out("out", "bytesOut", CEFMapping::convertInteger),
    outcome("outcome", "eventOutcome", CEFMapping::convertString),
    proto("proto", "transportProtocol", CEFMapping::convertString),
    reason("reason", "reason", CEFMapping::convertString),
    request("request", "requestUrl", CEFMapping::convertString),
    requestClientApplication("requestClientApplication", "requestClientApplication", CEFMapping::convertString),
    requestContext("requestContext", "requestContext", CEFMapping::convertString),
    requestCookies("requestCookies", "requestCookies", CEFMapping::convertString),
    requestMethod("requestMethod", "requestMethod", CEFMapping::convertString),
    rt("rt", "deviceReceiptTime", CEFMapping::convertTimestamp),
    shost("shost", "sourceHostName", CEFMapping::convertString),
    smac("smac", "sourceMacAddress", CEFMapping::convertMacAddress),
    sntdom("sntdom", "sourceNtDomain", CEFMapping::convertString),
    sourceDnsDomain("sourceDnsDomain", "sourceDnsDomain", CEFMapping::convertString),
    sourceServiceName("sourceServiceName", "sourceServiceName", CEFMapping::convertString),
    sourceTranslatedAddress("sourceTranslatedAddress", "sourceTranslatedAddress", CEFMapping::convertIPv4Address),
    sourceTranslatedPort("sourceTranslatedPort", "sourceTranslatedPort", CEFMapping::convertInteger),
    spid("spid", "sourceProcessId", CEFMapping::convertInteger),
    spriv("spriv", "sourceUserPrivileges", CEFMapping::convertString),
    sproc("sproc", "sourceProcessName", CEFMapping::convertString),
    spt("spt", "sourcePort", CEFMapping::convertInteger),
    src("src", "sourceAddress", CEFMapping::convertIPv4Address),
    start("start", "startTime", CEFMapping::convertTimestamp),
    suid("suid", "sourceUserId", CEFMapping::convertString),
    suser("suser", "sourceUserName", CEFMapping::convertString),
    type("type", "type", CEFMapping::convertType),

    // CEF Key Names for Event Consumers
    agentDnsDomain("agentDnsDomain", "agentDnsDomain", CEFMapping::convertString),
    agentNtDomain("agentNtDomain", "agentNtDomain", CEFMapping::convertString),
    agentTranslatedAddress("agentTranslatedAddress", "agentTranslatedAddress", CEFMapping::convertIPAddress),
    agentTranslatedZoneExternalID("agentTranslatedZoneExternalID", "agentTranslatedZoneExternalID", CEFMapping::convertString),
    agentTranslatedZoneURI("agentTranslatedZoneURI", "agentTranslatedZoneURI", CEFMapping::convertString),
    agentZoneExternalID("agentZoneExternalID", "agentZoneExternalID", CEFMapping::convertString),
    agentZoneURI("agentZoneURI", "agentZoneURI", CEFMapping::convertString),
    agt("agt", "agentAddress", CEFMapping::convertIPAddress),
    ahost("ahost", "agentHostName", CEFMapping::convertString),
    aid("aid", "agentId", CEFMapping::convertString),
    amac("amac", "agentMacAddress", CEFMapping::convertMacAddress),
    art("art", "agentReceiptTime", CEFMapping::convertTimestamp),
    at("at", "agentType", CEFMapping::convertString),
    atz("atz", "agentTimeZone", CEFMapping::convertString),
    av("av", "agentVersion", CEFMapping::convertString),
    cat("cat", "deviceEventCategory", CEFMapping::convertString),
    customerExternalID("customerExternalID", "customerExternalID", CEFMapping::convertString),
    customerURI("customerURI", "customerURI", CEFMapping::convertString),
    destinationTranslatedZoneExternalID("destinationTranslatedZoneExternalID", "destinationTranslatedZoneExternalID", CEFMapping::convertString),
    destinationTranslatedZoneURI("destinationTranslatedZoneURI", "destinationTranslatedZoneURI", CEFMapping::convertString),
    destinationZoneExternalID("destinationZoneExternalID", "destinationZoneExternalID", CEFMapping::convertString),
    destinationZoneURI("destinationZoneURI", "destinationZoneURI", CEFMapping::convertString),
    deviceTranslatedZoneExternalID("deviceTranslatedZoneExternalID", "deviceTranslatedZoneExternalID", CEFMapping::convertString),
    deviceTranslatedZoneURI("deviceTranslatedZoneURI", "deviceTranslatedZoneURI", CEFMapping::convertString),
    deviceZoneExternalID("deviceZoneExternalID", "deviceZoneExternalID", CEFMapping::convertString),
    deviceZoneURI("deviceZoneURI", "deviceZoneURI", CEFMapping::convertString),
    dlat("dlat", "destinationGeoLatitude", CEFMapping::convertDouble),
    dlong("dlong", "destinationGeoLongitude", CEFMapping::convertDouble),
    eventId("eventId", "eventId", CEFMapping::convertLong),
    rawEvent("rawEvent", "rawEvent", CEFMapping::convertString),
    slat("slat", "sourceGeoLatitude", CEFMapping::convertDouble),
    slong("slong", "sourceGeoLongitude", CEFMapping::convertDouble),
    sourceTranslatedZoneExternalID("sourceTranslatedZoneExternalID", "sourceTranslatedZoneExternalID", CEFMapping::convertString),
    sourceTranslatedZoneURI("sourceTranslatedZoneURI", "sourceTranslatedZoneURI", CEFMapping::convertString),
    sourceZoneExternalID("sourceZoneExternalID", "sourceZoneExternalID", CEFMapping::convertString),
    sourceZoneURI("sourceZoneURI", "sourceZoneURI", CEFMapping::convertString);

    // Lookup tables for faster access
    private static final ImmutableMap<String, CEFMapping> KEY_NAMES;
    private static final ImmutableMap<String, CEFMapping> FULL_NAMES;

    static {
        final ImmutableMap.Builder<String, CEFMapping> keyNamesBuilder = ImmutableMap.builder();
        final ImmutableMap.Builder<String, CEFMapping> fullNamesBuilder = ImmutableMap.builder();
        for (CEFMapping cefMapping : values()) {
            keyNamesBuilder.put(cefMapping.keyName, cefMapping);
            fullNamesBuilder.put(cefMapping.fullName, cefMapping);
        }
        KEY_NAMES = keyNamesBuilder.build();
        FULL_NAMES = fullNamesBuilder.build();
    }

    private static String convertString(String s) {
        return s;
    }

    private static Float convertFloat(String s) {
        return Float.parseFloat(s);
    }

    private static Double convertDouble(String s) {
        return Double.parseDouble(s);
    }

    private static Long convertLong(String s) {
        return Long.parseLong(s);
    }

    private static BigInteger convertBigInteger(String s) {
        return new BigInteger(s);
    }

    private static Integer convertInteger(String s) {
        return Integer.parseInt(s);
    }

    private static Object convertTimestamp(String s) {
        final DateTime dateTime = CEFTimestampParser.parse(s);
        return dateTime == null ? s : dateTime;
    }

    private static String convertIPv4Address(String s) {
        return s;
    }

    private static String convertIPv6Address(String s) {
        return s;
    }

    private static String convertIPAddress(String s) {
        return s;
    }

    private static String convertMacAddress(String s) {
        return s;
    }

    private static Integer convertType(String s) {
        return convertInteger(s);
    }

    private static Integer convertDirection(String s) {
        return convertInteger(s);
    }

    @Nullable
    public static CEFMapping forKeyName(String keyName) {
        return KEY_NAMES.get(keyName);
    }

    @Nullable
    public static CEFMapping forFullName(String fullName) {
        return FULL_NAMES.get(fullName);
    }

    private final String keyName;
    private final String fullName;
    private final Function<String, ?> converter;

    CEFMapping(String keyName, String fullName, Function<String, ?> converter) {
        this.keyName = keyName;
        this.fullName = fullName;
        this.converter = converter;
    }

    public String getKeyName() {
        return keyName;
    }

    public String getFullName() {
        return fullName;
    }

    public Function<String, ?> getConverter() {
        return converter;
    }

    public Object convert(String s) {
        return converter.apply(s);
    }
}
