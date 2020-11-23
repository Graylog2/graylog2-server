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

import com.google.common.collect.Sets;
import org.graylog.integrations.inputs.paloalto.PaloAltoFieldTemplate;
import org.graylog.integrations.inputs.paloalto.PaloAltoMessageTemplate;
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

import java.util.Set;

import static org.graylog.integrations.inputs.paloalto.PaloAltoFieldTemplate.create;
import static org.graylog.integrations.inputs.paloalto.PaloAltoFieldType.LONG;
import static org.graylog.integrations.inputs.paloalto.PaloAltoFieldType.STRING;

public class PaloAlto9xTemplates {

    private static PaloAltoMessageTemplate toTemplate(Set<PaloAltoFieldTemplate> fields) {
        PaloAltoMessageTemplate template = new PaloAltoMessageTemplate();
        template.setFields(fields);
        return template;
    }

    public static PaloAltoMessageTemplate configTemplate() {
        Set<PaloAltoFieldTemplate> fields = Sets.newHashSet();

        // Field 0 is FUTURE USE
        fields.add(create(EventFields.EVENT_CREATED, 1, STRING));
        fields.add(create(HostFields.HOST_ID, 2, STRING));
        fields.add(create(EventFields.EVENT_LOG_NAME, 3, STRING));
        fields.add(create(PaloAlto9xFields.PAN_LOG_SUBTYPE, 4, STRING));

        // Field 5 is FUTURE USE
        fields.add(create(Message.FIELD_TIMESTAMP, 6, STRING));
        fields.add(create(SourceFields.SOURCE_REFERENCE, 7, STRING));
        fields.add(create(HostFields.HOST_VIRTFW_ID, 8, STRING));
        fields.add(create(UserFields.USER_COMMAND, 9, STRING));

        fields.add(create(UserFields.USER_NAME, 10, STRING));
        fields.add(create(VendorFields.VENDOR_SIGNIN_PROTOCOL, 11, STRING));
        fields.add(create(VendorFields.VENDOR_EVENT_OUTCOME, 12, STRING));
        fields.add(create(UserFields.USER_COMMAND_PATH, 13, STRING));
        fields.add(create(PaloAlto9xFields.PAN_BEFORE_CHANGE_DETAIL, 14, STRING));

        fields.add(create(PaloAlto9xFields.PAN_AFTER_CHANGE_DETAIL, 15, STRING));
        fields.add(create(EventFields.EVENT_UID, 16, STRING));
        fields.add(create(PaloAlto9xFields.PAN_LOG_PANORAMA, 17, STRING));
        fields.add(create(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_1, 18, LONG));
        fields.add(create(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_2, 19, LONG));

        fields.add(create(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_3, 20, LONG));
        fields.add(create(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_4, 21, LONG));
        fields.add(create(HostFields.HOST_VIRTFW_HOSTNAME, 22, STRING));
        fields.add(create(HostFields.HOST_HOSTNAME, 23, STRING));

        return toTemplate(fields);
    }

    public static PaloAltoMessageTemplate correlationTemplate() {
        Set<PaloAltoFieldTemplate> fields = Sets.newHashSet();

        // Field 0 is FUTURE USE
        fields.add(create(EventFields.EVENT_CREATED, 1, STRING));
        fields.add(create(HostFields.HOST_ID, 2, STRING));
        fields.add(create(EventFields.EVENT_LOG_NAME, 3, STRING));
        fields.add(create(PaloAlto9xFields.PAN_LOG_SUBTYPE, 4, STRING));

        // Field 5 is FUTURE USE
        fields.add(create(Message.FIELD_TIMESTAMP, 6, STRING));
        fields.add(create(SourceFields.SOURCE_IP, 7, STRING));
        fields.add(create(UserFields.USER_NAME, 8, STRING));
        fields.add(create(HostFields.HOST_VIRTFW_ID, 9, STRING));

        fields.add(create(ThreatFields.THREAT_CATEGORY, 10, STRING));
        fields.add(create(EventFields.EVENT_SEVERITY, 11, STRING));
        fields.add(create(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_1, 12, LONG));
        fields.add(create(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_2, 13, LONG));
        fields.add(create(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_3, 14, LONG));

        fields.add(create(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_4, 15, LONG));
        fields.add(create(HostFields.HOST_VIRTFW_HOSTNAME, 16, STRING));
        fields.add(create(HostFields.HOST_HOSTNAME, 17, STRING));
        fields.add(create(HostFields.HOST_VIRTFW_UID, 18, STRING));
        fields.add(create(PaloAlto9xFields.PAN_OBJECTNAME, 19, STRING));

        fields.add(create(PaloAlto9xFields.PAN_OBJECT_ID, 20, STRING));
        fields.add(create(PaloAlto9xFields.PAN_EVIDENCE, 21, STRING));

        return toTemplate(fields);
    }

    public static PaloAltoMessageTemplate hipTemplate() {
        Set<PaloAltoFieldTemplate> fields = Sets.newHashSet();

        // Field 0 is FUTURE USE
        fields.add(create(EventFields.EVENT_CREATED, 1, STRING));
        fields.add(create(EventFields.EVENT_OBSERVER_UID, 2, STRING));
        fields.add(create(EventFields.EVENT_LOG_NAME, 3, STRING));
        fields.add(create(PaloAlto9xFields.PAN_LOG_SUBTYPE, 4, STRING));

        // Field 5 is FUTURE USE
        fields.add(create(Message.FIELD_TIMESTAMP, 6, STRING));
        fields.add(create(UserFields.USER_NAME, 7, STRING));
        fields.add(create(HostFields.HOST_VIRTFW_ID, 8, STRING));
        fields.add(create(HostFields.HOST_HOSTNAME, 9, STRING));

        fields.add(create(HostFields.HOST_TYPE, 10, STRING));
        fields.add(create(HostFields.HOST_IP, 11, STRING));
        fields.add(create(PaloAlto9xFields.PAN_HIP, 12, STRING));
        fields.add(create(EventFields.EVENT_REPEAT_COUNT, 13, LONG));
        fields.add(create(PaloAlto9xFields.PAN_HIP_TYPE, 14, STRING));

        // Field 15 is FUTURE USE
        // Field 16 is FUTURE USE
        fields.add(create(EventFields.EVENT_UID, 17, STRING));
        fields.add(create(PaloAlto9xFields.PAN_LOG_PANORAMA, 18, STRING));
        fields.add(create(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_1, 19, LONG));

        fields.add(create(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_2, 20, LONG));
        fields.add(create(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_3, 21, LONG));
        fields.add(create(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_4, 22, LONG));
        fields.add(create(HostFields.HOST_VIRTFW_HOSTNAME, 23, STRING));
        fields.add(create(EventFields.EVENT_OBSERVER_HOSTNAME, 24, STRING));

        fields.add(create(HostFields.HOST_VIRTFW_UID, 25, STRING));
        fields.add(create(HostFields.HOST_IPV6, 26, STRING));
        fields.add(create(PaloAlto9xFields.PAN_GP_HOSTID, 27, STRING));
        fields.add(create(HostFields.HOST_ID, 28, STRING));
        fields.add(create(SourceFields.SOURCE_MAC, 29, STRING));

        fields.add(create(PaloAlto9xFields.PAN_HIGH_RES_TIME, 30, STRING));

        return toTemplate(fields);
    }

    public static PaloAltoMessageTemplate globalProtectPre913Template() {
        Set<PaloAltoFieldTemplate> fields = Sets.newHashSet();

        // Field 0 is FUTURE USE
        fields.add(create(EventFields.EVENT_RECEIVED_TIME, 1, STRING));
        fields.add(create(HostFields.HOST_ID, 2, STRING)); // TODO: Used twice
        fields.add(create(EventFields.EVENT_UID, 3, STRING));
        fields.add(create(PaloAlto9xFields.PAN_LOG_PANORAMA, 4, STRING));

        fields.add(create(EventFields.EVENT_LOG_NAME, 5, STRING));
        // Field 6 is FUTURE USE
        // Field 7 is FUTURE USE
        fields.add(create(Message.FIELD_TIMESTAMP, 8, STRING));
        fields.add(create(HostFields.HOST_VIRTFW_ID, 9, STRING));

        fields.add(create(PaloAlto9xFields.PAN_EVENT_NAME, 10, STRING));
        fields.add(create(PaloAlto9xFields.PAN_TUNNEL_STAGE, 11, STRING));
        fields.add(create(PaloAlto9xFields.PAN_AUTH_METHOD, 12, STRING));
        fields.add(create(NetworkFields.NETWORK_TUNNEL_TYPE, 13, STRING));
        fields.add(create(SourceFields.SOURCE_USER, 14, STRING));

        fields.add(create(PaloAlto9xFields.PAN_SOURCE_REGION, 15, STRING));
        fields.add(create(SourceFields.SOURCE_HOSTNAME, 16, STRING));
        fields.add(create(VendorFields.VENDOR_PUBLIC_IP, 17, STRING));
        fields.add(create(VendorFields.VENDOR_PUBLIC_IPV6, 18, STRING));
        fields.add(create(VendorFields.VENDOR_PRIVATE_IP, 19, STRING));

        fields.add(create(VendorFields.VENDOR_PRIVATE_IPV6, 20, STRING));
        fields.add(create(PaloAlto9xFields.PAN_GP_HOSTID, 21, STRING));
        // fields.add(create(HostFields.HOST_ID, 22, STRING)); // TODO: Used twice
        fields.add(create(PaloAlto9xFields.PAN_GP_CLIENT_VERSION, 23, STRING));
        fields.add(create(HostFields.HOST_TYPE, 24, STRING));

        fields.add(create(HostFields.HOST_TYPE_VERSION, 25, STRING));
        fields.add(create(EventFields.EVENT_REPEAT_COUNT, 26, LONG));
        fields.add(create(PaloAlto9xFields.PAN_GP_REASON, 27, STRING));
        fields.add(create(PaloAlto9xFields.PAN_GP_ERROR, 28, STRING));
        fields.add(create(PaloAlto9xFields.PAN_GP_ERROR_EXTENDED, 29, STRING));

        fields.add(create(VendorFields.VENDOR_EVENT_ACTION, 30, STRING));
        fields.add(create(PaloAlto9xFields.PAN_GP_LOCATION_NAME, 31, STRING));
        fields.add(create(NetworkFields.NETWORK_TUNNEL_DURATION, 32, LONG));
        fields.add(create(PaloAlto9xFields.PAN_GP_CONNECT_METHOD, 33, STRING));
        fields.add(create(PaloAlto9xFields.PAN_GP_ERROR_CODE, 34, LONG));

        fields.add(create(PaloAlto9xFields.PAN_GP_HOSTNAME, 35, STRING));


        return toTemplate(fields);
    }

    public static PaloAltoMessageTemplate globalProtect913Template() {
        Set<PaloAltoFieldTemplate> fields = Sets.newHashSet();

        // Field 0 is FUTURE USE
        fields.add(create(EventFields.EVENT_RECEIVED_TIME, 1, STRING));
        fields.add(create(HostFields.HOST_ID, 2, STRING));
        fields.add(create(EventFields.EVENT_LOG_NAME, 3, STRING));
        fields.add(create(PaloAlto9xFields.PAN_LOG_SUBTYPE, 4, STRING));

        // Field 5 is FUTURE USE
        fields.add(create(Message.FIELD_TIMESTAMP, 6, STRING));
        fields.add(create(HostFields.HOST_VIRTFW_ID, 7, STRING));
        fields.add(create(PaloAlto9xFields.PAN_EVENT_NAME, 8, STRING));
        fields.add(create(PaloAlto9xFields.PAN_TUNNEL_STAGE, 9, STRING));

        fields.add(create(PaloAlto9xFields.PAN_AUTH_METHOD, 10, STRING));
        fields.add(create(NetworkFields.NETWORK_TUNNEL_TYPE, 11, STRING));
        fields.add(create(UserFields.USER_NAME, 12, STRING));
        fields.add(create(PaloAlto9xFields.PAN_SOURCE_REGION, 13, STRING));
        fields.add(create(SourceFields.SOURCE_HOSTNAME, 14, STRING));

        fields.add(create(VendorFields.VENDOR_PUBLIC_IP, 15, STRING));
        fields.add(create(VendorFields.VENDOR_PUBLIC_IPV6, 16, STRING));
        fields.add(create(VendorFields.VENDOR_PRIVATE_IP, 17, STRING));
        fields.add(create(VendorFields.VENDOR_PRIVATE_IPV6, 18, STRING));
        fields.add(create(PaloAlto9xFields.PAN_GP_HOSTID, 19, STRING));

        fields.add(create(SourceFields.SOURCE_ID, 20, STRING));
        fields.add(create(PaloAlto9xFields.PAN_GP_CLIENT_VERSION, 21, STRING));
        fields.add(create(SourceFields.SOURCE_OS_NAME, 22, STRING));
        fields.add(create(SourceFields.SOURCE_OS_VERSION, 23, STRING));
        fields.add(create(EventFields.EVENT_REPEAT_COUNT, 24, LONG));

        fields.add(create(PaloAlto9xFields.PAN_GP_REASON, 25, STRING));
        fields.add(create(EventFields.EVENT_ERROR_DESCRIPTION, 26, STRING));
        fields.add(create(PaloAlto9xFields.PAN_GP_ERROR_EXTENDED, 27, STRING));
        fields.add(create(VendorFields.VENDOR_EVENT_OUTCOME, 28, STRING));
        fields.add(create(PaloAlto9xFields.PAN_GP_LOCATION_NAME, 29, STRING));

        fields.add(create(NetworkFields.NETWORK_TUNNEL_DURATION, 30, LONG));
        fields.add(create(PaloAlto9xFields.PAN_GP_CONNECT_METHOD, 31, STRING));
        fields.add(create(EventFields.EVENT_ERROR_CODE, 32, LONG));
        fields.add(create(DestinationFields.DESTINATION_HOSTNAME, 33, STRING));
        fields.add(create(EventFields.EVENT_UID, 34, STRING));

        fields.add(create(PaloAlto9xFields.PAN_LOG_PANORAMA, 35, STRING));
        fields.add(create(PaloAlto9xFields.PAN_SELECTION_TYPE, 36, STRING));
        fields.add(create(ApplicationFields.APPLICATION_RESPONSE_TIME, 37, LONG));
        fields.add(create(PaloAlto9xFields.PAN_GATEWAY_PRIORITY, 38, LONG));
        fields.add(create(PaloAlto9xFields.PAN_ATTEMPTED_GATEWAYS, 39, STRING));

        fields.add(create(PaloAlto9xFields.PAN_GATEWAY, 40, STRING));

        return toTemplate(fields);
    }

    public static PaloAltoMessageTemplate systemTemplate() {
        Set<PaloAltoFieldTemplate> fields = Sets.newHashSet();

        // Field 0 is FUTURE USE
        fields.add(create(EventFields.EVENT_CREATED, 1, STRING));
        fields.add(create(HostFields.HOST_ID, 2, STRING));
        fields.add(create(EventFields.EVENT_LOG_NAME, 3, STRING));
        fields.add(create(PaloAlto9xFields.PAN_LOG_SUBTYPE, 4, STRING));

        // Field 5 is FUTURE USE
        fields.add(create(Message.FIELD_TIMESTAMP, 6, STRING));
        fields.add(create(HostFields.HOST_VIRTFW_ID, 7, STRING));
        fields.add(create(PaloAlto9xFields.PAN_EVENT_NAME, 8, STRING));
        fields.add(create(PaloAlto9xFields.PAN_EVENT_OBJECT, 9, STRING));

        // Field 10 is FUTURE USE
        // Field 11 is FUTURE USE
        fields.add(create(PaloAlto9xFields.PAN_MODULE, 12, STRING));
        fields.add(create(EventFields.EVENT_SEVERITY, 13, STRING));
        fields.add(create(VendorFields.VENDOR_EVENT_DESCRIPTION, 14, STRING));

        fields.add(create(EventFields.EVENT_UID, 15, STRING));
        fields.add(create(PaloAlto9xFields.PAN_LOG_PANORAMA, 16, STRING));
        fields.add(create(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_1, 17, LONG));
        fields.add(create(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_2, 18, LONG));
        fields.add(create(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_3, 19, LONG));

        fields.add(create(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_4, 20, LONG));
        fields.add(create(HostFields.HOST_VIRTFW_HOSTNAME, 21, STRING));
        fields.add(create(HostFields.HOST_HOSTNAME, 22, STRING));
        fields.add(create(PaloAlto9xFields.PAN_HIGH_RES_TIME, 23, STRING));

        return toTemplate(fields);
    }

    public static PaloAltoMessageTemplate threatTemplate() {
        Set<PaloAltoFieldTemplate> fields = Sets.newHashSet();

        // Field 0 is FUTURE USE
        fields.add(create(EventFields.EVENT_RECEIVED_TIME, 1, STRING));
        fields.add(create(EventFields.EVENT_OBSERVER_ID, 2, STRING));
        fields.add(create(EventFields.EVENT_LOG_NAME, 3, STRING));
        fields.add(create(PaloAlto9xFields.PAN_LOG_SUBTYPE, 4, STRING));

        // Field 5 is FUTURE USE
        fields.add(create(Message.FIELD_TIMESTAMP, 6, STRING));
        fields.add(create(SourceFields.SOURCE_IP, 7, STRING));
        fields.add(create(DestinationFields.DESTINATION_IP, 8, STRING));
        fields.add(create(SourceFields.SOURCE_NAT_IP, 9, STRING));

        fields.add(create(DestinationFields.DESTINATION_NAT_IP, 10, STRING));
        fields.add(create(RuleFields.RULE_NAME, 11, STRING));
        fields.add(create(SourceFields.SOURCE_USER_NAME, 12, STRING));
        fields.add(create(DestinationFields.DESTINATION_USER_NAME, 13, STRING));
        fields.add(create(ApplicationFields.APPLICATION_NAME, 14, STRING));

        fields.add(create(HostFields.HOST_VIRTFW_ID, 15, STRING));
        fields.add(create(SourceFields.SOURCE_ZONE, 16, STRING));
        fields.add(create(DestinationFields.DESTINATION_ZONE, 17, STRING));
        fields.add(create(NetworkFields.NETWORK_INTERFACE_IN, 18, STRING));
        fields.add(create(NetworkFields.NETWORK_INTERFACE_OUT, 19, STRING));

        fields.add(create(PaloAlto9xFields.PAN_LOG_ACITON, 20, STRING));
        // Field 21 is FUTURE USE
        fields.add(create(SessionFields.SESSION_ID, 22, LONG));
        fields.add(create(EventFields.EVENT_REPEAT_COUNT, 23, LONG));
        fields.add(create(SourceFields.SOURCE_PORT, 24, LONG));

        fields.add(create(DestinationFields.DESTINATION_PORT, 25, LONG));
        fields.add(create(SourceFields.SOURCE_NAT_PORT, 26, LONG));
        fields.add(create(DestinationFields.DESTINATION_NAT_PORT, 27, LONG));
        fields.add(create(PaloAlto9xFields.PAN_FLAGS, 28, STRING));
        fields.add(create(NetworkFields.NETWORK_TRANSPORT, 29, STRING));

        fields.add(create(VendorFields.VENDOR_EVENT_ACTION, 30, STRING));
        fields.add(create(AlertFields.ALERT_INDICATOR, 31, STRING));
        fields.add(create(AlertFields.ALERT_SIGNATURE, 32, STRING));
        fields.add(create(AlertFields.ALERT_CATEGORY, 33, STRING));
        fields.add(create(VendorFields.VENDOR_ALERT_SEVERITY, 34, STRING));

        fields.add(create(PaloAlto9xFields.PAN_ALERT_DIRECTION, 35, STRING));
        fields.add(create(EventFields.EVENT_UID, 36, STRING));
        fields.add(create(PaloAlto9xFields.PAN_LOG_PANORAMA, 37, STRING));
        fields.add(create(SourceFields.SOURCE_LOCATION_NAME, 38, STRING));
        fields.add(create(DestinationFields.DESTINATION_LOCATION_NAME, 39, STRING));

        // Field 40 is FUTURE USE
        fields.add(create(HttpFields.HTTP_CONTENT_TYPE, 41, STRING));
        fields.add(create(PaloAlto9xFields.PAN_PCAP_ID, 42, STRING));
        fields.add(create(PaloAlto9xFields.PAN_WILDFIRE_HASH, 43, STRING));
        fields.add(create(PaloAlto9xFields.PAN_CLOUD_HOSTNAME, 44, STRING));

        fields.add(create(PaloAlto9xFields.PAN_URL_INDEX, 45, LONG));
        fields.add(create(HttpFields.HTTP_USER_AGENT_NAME, 46, STRING));
        fields.add(create(FileFields.FILE_TYPE, 47, STRING));
        fields.add(create(HttpFields.HTTP_XFF, 48, STRING));
        fields.add(create(HttpFields.HTTP_REFERER, 49, STRING));

        fields.add(create(SourceFields.SOURCE_USER_EMAIL, 50, STRING));
        fields.add(create(EmailFields.EMAIL_SUBJECT, 51, STRING));
        fields.add(create(UserFields.TARGET_USER_EMAIL, 52, STRING));
        fields.add(create(PaloAlto9xFields.PAN_WILDFIRE_REPORT_ID, 53, LONG));
        fields.add(create(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_1, 54, LONG));

        fields.add(create(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_2, 55, LONG));
        fields.add(create(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_3, 56, LONG));
        fields.add(create(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_4, 57, LONG));
        fields.add(create(HostFields.HOST_VIRTFW_HOSTNAME, 58, STRING));
        fields.add(create(EventFields.EVENT_OBSERVER_HOSTNAME, 59, STRING));

        // Field 60 is FUTURE USE
        fields.add(create(SourceFields.SOURCE_VSYS_UUID, 61, STRING));
        fields.add(create(DestinationFields.DESTINATION_VSYS_UUID, 62, STRING));
        fields.add(create(HttpFields.HTTP_METHOD, 63, STRING));
        fields.add(create(PaloAlto9xFields.PAN_TUNNEL_ID, 64, STRING));

        fields.add(create(PaloAlto9xFields.PAN_MONITOR_TAG, 65, LONG));
        fields.add(create(PaloAlto9xFields.PAN_PARENT_SESSION_ID, 66, STRING));
        fields.add(create(PaloAlto9xFields.PAN_PARENT_START_TIME, 67, STRING));
        fields.add(create(NetworkFields.NETWORK_TUNNEL_TYPE, 68, STRING));
        fields.add(create(AlertFields.ALERT_CATEGORY, 69, STRING));

        fields.add(create(AlertFields.ALERT_DEFINITIONS_VERSION, 70, STRING));
        // Field 71 is FUTURE USE
        fields.add(create(PaloAlto9xFields.PAN_ASSOC_ID, 72, LONG));
        fields.add(create(PaloAlto9xFields.PAN_PPID, 73, LONG));
        fields.add(create(HttpFields.HTTP_HEADERS, 74, STRING));

        fields.add(create(HttpFields.HTTP_URI_CATEGORY, 75, STRING));
        fields.add(create(PolicyFields.POLICY_UID, 76, STRING));
        fields.add(create(PaloAlto9xFields.PAN_HTTP2, 77, STRING));
        fields.add(create(PaloAlto9xFields.PAN_DYNUSERGROUP_NAME, 78, STRING));
        fields.add(create(HttpFields.HTTP_XFF, 79, STRING));

        fields.add(create(SourceFields.SOURCE_CATEGORY, 80, STRING));
        fields.add(create(PaloAlto9xFields.PAN_SOURCE_PROFILE, 81, STRING));
        fields.add(create(SourceFields.SOURCE_DEVICE_MODEL, 82, STRING));
        fields.add(create(SourceFields.SOURCE_DEVICE_VENDOR, 83, STRING));
        fields.add(create(SourceFields.SOURCE_OS_NAME, 84, STRING));

        fields.add(create(SourceFields.SOURCE_OS_VERSION, 85, STRING));
        fields.add(create(SourceFields.SOURCE_HOSTNAME, 86, STRING));
        fields.add(create(SourceFields.SOURCE_MAC, 87, STRING));
        fields.add(create(DestinationFields.DESTINATION_CATEGORY, 88, STRING));
        fields.add(create(PaloAlto9xFields.PAN_DESTINATION_PROFILE, 89, STRING));

        fields.add(create(DestinationFields.DESTINATION_DEVICE_MODEL, 90, STRING));
        fields.add(create(DestinationFields.DESTINATION_DEVICE_VENDOR, 91, STRING));
        fields.add(create(DestinationFields.DESTINATION_OS_NAME, 92, STRING));
        fields.add(create(DestinationFields.DESTINATION_OS_VERSION, 93, STRING));
        fields.add(create(DestinationFields.DESTINATION_HOSTNAME, 94, STRING));

        fields.add(create(DestinationFields.DESTINATION_MAC, 95, STRING));
        fields.add(create(ContainerFields.CONTAINER_ID, 96, STRING));
        fields.add(create(ContainerFields.CONTAINER_NAMESPACE, 97, STRING));
        fields.add(create(ContainerFields.CONTAINER_NAME, 98, STRING));
        fields.add(create(PaloAlto9xFields.PAN_SRC_EDL, 99, STRING));

        fields.add(create(PaloAlto9xFields.PAN_DST_EDL, 100, STRING));
        fields.add(create(PaloAlto9xFields.PAN_HOST_ID, 101, STRING));
        fields.add(create(PaloAlto9xFields.PAN_HOST_SN, 102, STRING));
        fields.add(create(PaloAlto9xFields.PAN_DOMAIN_EDL, 103, STRING));
        fields.add(create(PaloAlto9xFields.PAN_SRC_DAG, 104, STRING));

        fields.add(create(PaloAlto9xFields.PAN_DST_DAG, 105, STRING));
        fields.add(create(PaloAlto9xFields.PAN_PARTIAL_HASH, 106, STRING));
        fields.add(create(PaloAlto9xFields.PAN_HIGH_RES_TIME, 107, STRING));
        fields.add(create(VendorFields.VENDOR_EVENT_OUTCOME_REASON, 108, STRING));
        fields.add(create(PaloAlto9xFields.PAN_EVENT_JUSTIFICATION, 109, STRING));

        fields.add(create(PaloAlto9xFields.PAN_NSDSAI_SST, 110, STRING));

        return toTemplate(fields);
    }

    public static PaloAltoMessageTemplate trafficTemplate() {
        Set<PaloAltoFieldTemplate> fields = Sets.newHashSet();

        // Field 0 is FUTURE USE
        fields.add(create(EventFields.EVENT_RECEIVED_TIME, 1, STRING));
        fields.add(create(EventFields.EVENT_OBSERVER_ID, 2, STRING));
        fields.add(create(EventFields.EVENT_LOG_NAME, 3, STRING));
        fields.add(create(PaloAlto9xFields.PAN_LOG_SUBTYPE, 4, STRING));

        // Field 5 is FUTURE USE
        fields.add(create(Message.FIELD_TIMESTAMP, 6, STRING));
        fields.add(create(SourceFields.SOURCE_IP, 7, STRING));
        fields.add(create(DestinationFields.DESTINATION_IP, 8, STRING));
        fields.add(create(SourceFields.SOURCE_NAT_IP, 9, STRING));

        fields.add(create(DestinationFields.DESTINATION_NAT_IP, 10, STRING));
        fields.add(create(RuleFields.RULE_NAME, 11, STRING));
        fields.add(create(SourceFields.SOURCE_USER_NAME, 12, STRING));
        fields.add(create(DestinationFields.DESTINATION_USER_NAME, 13, STRING));
        fields.add(create(ApplicationFields.APPLICATION_NAME, 14, STRING));

        fields.add(create(HostFields.HOST_VIRTFW_ID, 15, STRING));
        fields.add(create(SourceFields.SOURCE_ZONE, 16, STRING));
        fields.add(create(DestinationFields.DESTINATION_ZONE, 17, STRING));
        fields.add(create(NetworkFields.NETWORK_INTERFACE_IN, 18, STRING));
        fields.add(create(NetworkFields.NETWORK_INTERFACE_OUT, 19, STRING));

        fields.add(create(PaloAlto9xFields.PAN_LOG_ACITON, 20, STRING));
        // Field 21 is FUTURE USE
        fields.add(create(SessionFields.SESSION_ID, 22, LONG));
        fields.add(create(EventFields.EVENT_REPEAT_COUNT, 23, LONG));
        fields.add(create(SourceFields.SOURCE_PORT, 24, LONG));

        fields.add(create(DestinationFields.DESTINATION_PORT, 25, LONG));
        fields.add(create(SourceFields.SOURCE_NAT_PORT, 26, LONG));
        fields.add(create(DestinationFields.DESTINATION_NAT_PORT, 27, LONG));
        fields.add(create(PaloAlto9xFields.PAN_FLAGS, 28, STRING));
        fields.add(create(NetworkFields.NETWORK_TRANSPORT, 29, STRING));

        fields.add(create(VendorFields.VENDOR_EVENT_ACTION, 30, STRING));
        fields.add(create(NetworkFields.NETWORK_BYTES, 31, LONG));
        fields.add(create(SourceFields.SOURCE_BYTES_SENT, 32, LONG));
        fields.add(create(DestinationFields.DESTINATION_BYTES_SENT, 33, LONG));
        fields.add(create(NetworkFields.NETWORK_PACKETS, 34, LONG));

        fields.add(create(EventFields.EVENT_START, 35, STRING));
        fields.add(create(EventFields.EVENT_DURATION, 36, LONG));
        fields.add(create(HttpFields.HTTP_URI_CATEGORY, 37, STRING));
        // Field 38 is FUTURE USE
        fields.add(create(EventFields.EVENT_UID, 39, STRING));

        fields.add(create(PaloAlto9xFields.PAN_LOG_PANORAMA, 40, STRING));
        fields.add(create(SourceFields.SOURCE_LOCATION_NAME, 41, STRING));
        fields.add(create(DestinationFields.DESTINATION_LOCATION_NAME, 42, STRING));
        // Field 43 is FUTURE USE
        fields.add(create(SourceFields.SOURCE_PACKETS_SENT, 44, LONG));

        fields.add(create(DestinationFields.DESTINATION_PACKETS_SENT, 45, LONG));
        fields.add(create(PaloAlto9xFields.PAN_SESSION_END_REASON, 46, STRING));
        fields.add(create(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_1, 47, LONG));
        fields.add(create(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_2, 48, LONG));
        fields.add(create(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_3, 49, LONG));

        fields.add(create(PaloAlto9xFields.PAN_DEV_GROUP_LEVEL_4, 50, LONG));
        fields.add(create(HostFields.HOST_VIRTFW_HOSTNAME, 51, STRING));
        fields.add(create(EventFields.EVENT_OBSERVER_HOSTNAME, 52, STRING));
        fields.add(create(VendorFields.VENDOR_EVENT_DESCRIPTION, 53, STRING));
        fields.add(create(SourceFields.SOURCE_VSYS_UUID, 54, STRING));

        fields.add(create(DestinationFields.DESTINATION_VSYS_UUID, 55, STRING));
        fields.add(create(PaloAlto9xFields.PAN_TUNNEL_ID, 56, STRING));
        fields.add(create(PaloAlto9xFields.PAN_MONITOR_TAG, 57, STRING));
        fields.add(create(PaloAlto9xFields.PAN_PARENT_SESSION_ID, 58, STRING));
        fields.add(create(PaloAlto9xFields.PAN_PARENT_START_TIME, 59, STRING));

        fields.add(create(NetworkFields.NETWORK_TUNNEL_TYPE, 60, STRING));
        fields.add(create(PaloAlto9xFields.PAN_ASSOC_ID, 61, LONG));
        fields.add(create(PaloAlto9xFields.PAN_SCTP_CHUNKS_SUM, 62, STRING));
        fields.add(create(PaloAlto9xFields.PAN_SCTP_CHUNKS_TX, 63, STRING));
        fields.add(create(PaloAlto9xFields.PAN_SCTP_CHUNKS_RX, 64, STRING));

        fields.add(create(PolicyFields.POLICY_UID, 65, STRING));
        fields.add(create(PaloAlto9xFields.PAN_HTTP2, 66, STRING));
        fields.add(create(PaloAlto9xFields.PAN_LINK_CHANGES, 67, LONG));
        fields.add(create(PaloAlto9xFields.PAN_SDWAN_POLICY_ID, 68, STRING));
        fields.add(create(PaloAlto9xFields.PAN_LINK_SWITCHES, 69, STRING));

        fields.add(create(PaloAlto9xFields.PAN_SDWAN_CLUSTER, 70, STRING));
        fields.add(create(PaloAlto9xFields.PAN_SDWAN_DEVICE_TYPE, 71, STRING));
        fields.add(create(PaloAlto9xFields.PAN_SDWAN_CLUSTER_TYPE, 72, STRING));
        fields.add(create(PaloAlto9xFields.PAN_SDWAN_SITE_NAME, 73, STRING));
        fields.add(create(PaloAlto9xFields.PAN_DYNUSERGROUP_NAME, 74, STRING));

        fields.add(create(HttpFields.HTTP_XFF, 75, STRING));
        fields.add(create(SourceFields.SOURCE_CATEGORY, 76, STRING));
        fields.add(create(PaloAlto9xFields.PAN_SOURCE_PROFILE, 77, STRING));
        fields.add(create(SourceFields.SOURCE_DEVICE_MODEL, 78, STRING));
        fields.add(create(SourceFields.SOURCE_DEVICE_VENDOR, 79, STRING));

        fields.add(create(SourceFields.SOURCE_OS_NAME, 80, STRING));
        fields.add(create(SourceFields.SOURCE_OS_VERSION, 81, STRING));
        fields.add(create(SourceFields.SOURCE_HOSTNAME, 82, STRING));
        fields.add(create(SourceFields.SOURCE_MAC, 83, STRING));
        fields.add(create(DestinationFields.DESTINATION_CATEGORY, 84, STRING));

        fields.add(create(PaloAlto9xFields.PAN_DESTINATION_PROFILE, 85, STRING));
        fields.add(create(DestinationFields.DESTINATION_DEVICE_MODEL, 86, STRING));
        fields.add(create(DestinationFields.DESTINATION_DEVICE_VENDOR, 87, STRING));
        fields.add(create(DestinationFields.DESTINATION_OS_NAME, 88, STRING));
        fields.add(create(DestinationFields.DESTINATION_OS_VERSION, 89, STRING));

        fields.add(create(DestinationFields.DESTINATION_HOSTNAME, 90, STRING));
        fields.add(create(DestinationFields.DESTINATION_MAC, 91, STRING));
        fields.add(create(ContainerFields.CONTAINER_ID, 92, STRING));
        fields.add(create(ContainerFields.CONTAINER_NAMESPACE, 93, STRING));
        fields.add(create(ContainerFields.CONTAINER_NAME, 94, STRING));

        fields.add(create(PaloAlto9xFields.PAN_SRC_EDL, 95, STRING));
        fields.add(create(PaloAlto9xFields.PAN_DST_EDL, 96, STRING));
        fields.add(create(PaloAlto9xFields.PAN_HOST_ID, 97, STRING));
        fields.add(create(PaloAlto9xFields.PAN_HOST_SN, 98, STRING));
        fields.add(create(PaloAlto9xFields.PAN_SRC_DAG, 99, STRING));

        fields.add(create(PaloAlto9xFields.PAN_DST_DAG, 100, STRING));
        fields.add(create(PaloAlto9xFields.PAN_SESSION_OWNER, 101, STRING));
        fields.add(create(PaloAlto9xFields.PAN_HIGH_RES_TIME, 102, STRING));
        fields.add(create(PaloAlto9xFields.PAN_NSDSAI_SST, 103, STRING));
        fields.add(create(PaloAlto9xFields.PAN_NSDSAI_SD, 104, STRING));

        return toTemplate(fields);
    }
}
