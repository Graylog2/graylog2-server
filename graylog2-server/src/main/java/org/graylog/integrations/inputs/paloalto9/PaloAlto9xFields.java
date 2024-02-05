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

public class PaloAlto9xFields {
    public static final String PAN_AFTER_CHANGE_DETAIL = "pan_after_change_detail";
    public static final String PAN_ALERT_DIRECTION = "pan_alert_direction";
    public static final String PAN_ASSOC_ID = "pan_assoc_id";
    public static final String PAN_ATTEMPTED_GATEWAYS = "pan_attempted_gateways";
    public static final String PAN_AUTH_METHOD = "pan_auth_method";
    public static final String PAN_BEFORE_CHANGE_DETAIL = "pan_before_change_detail";

    public static final String PAN_CLOUD_HOSTNAME = "pan_cloud_hostname";
    public static final String PAN_DATASOURCE = "pan_datasource";
    public static final String PAN_DATASOURCE_NAME = "pan_datasource_name";
    public static final String PAN_DATASOURCE_TYPE = "pan_datasource_type";
    public static final String PAN_DESTINATION_PROFILE = "pan_destination_profile";
    public static final String PAN_DEV_GROUP_LEVEL_1 = "pan_dev_group_level_1";
    public static final String PAN_DEV_GROUP_LEVEL_2 = "pan_dev_group_level_2";
    public static final String PAN_DEV_GROUP_LEVEL_3 = "pan_dev_group_level_3";
    public static final String PAN_DEV_GROUP_LEVEL_4 = "pan_dev_group_level_4";

    public static final String PAN_DOMAIN_EDL = "pan_domain_edl";
    public static final String PAN_DST_DAG = "pan_dst_dag";
    public static final String PAN_DST_EDL = "pan_dst_edl";

    public static final String PAN_DYNUSERGROUP_NAME = "pan_dynusergroup_name";
    public static final String PAN_EVENT_NAME = "pan_event_name";
    public static final String PAN_EVENT_OBJECT = "pan_event_object";
    public static final String PAN_EVENT_JUSTIFICATION = "pan_event_justification";
    public static final String PAN_EVIDENCE = "pan_evidence";
    public static final String PAN_FACTOR_COMPLETION_TIME = "pan_factor_completion_time";
    public static final String PAN_FACTOR_NUMBER = "pan_factor_number";
    public static final String PAN_FACTOR_TYPE = "pan_factor_type";
    public static final String PAN_FLAGS = "pan_flags";

    public static final String PAN_GATEWAY = "pan_gateway";
    public static final String PAN_GATEWAY_PRIORITY = "pan_gateway_priority";
    public static final String PAN_GP_CLIENT_VERSION = "pan_gp_client_version";
    public static final String PAN_GP_CONNECT_METHOD = "pan_gp_connect_method";
    public static final String PAN_GP_ERROR = "pan_gp_error";
    public static final String PAN_GP_ERROR_CODE = "pan_gp_error_code";
    public static final String PAN_GP_ERROR_EXTENDED = "pan_gp_error_extended";

    public static final String PAN_GP_HOSTID = "pan_gp_hostid";
    public static final String PAN_GP_HOSTNAME = "pan_gp_hostname";
    public static final String PAN_GP_LOCATION_NAME = "pan_gp_location_name";
    public static final String PAN_GP_REASON = "pan_gp_reason";
    public static final String PAN_HIGH_RES_TIME = "pan_high_res_time";
    public static final String PAN_HIP = "pan_hip";

    public static final String PAN_HIP_TYPE = "pan_hip_type";
    public static final String PAN_HOST_ID = "pan_host_id";
    public static final String PAN_HOST_SN = "pan_host_sn";
    public static final String PAN_HTTP2 = "pan_http2";
    public static final String PAN_LINK_CHANGES = "pan_link_changes";
    public static final String PAN_LINK_SWITCHES = "pan_link_switches";
    public static final String PAN_LOG_ACITON = "pan_log_action";

    public static final String PAN_LOG_PANORAMA = "pan_log_panorama";
    public static final String PAN_LOG_SUBTYPE = "pan_log_subtype";
    public static final String PAN_MODULE = "pan_module";
    public static final String PAN_MONITOR_TAG = "pan_monitor_tag";
    public static final String PAN_NSDSAI_SD = "pan_nsdsai_sd";
    public static final String PAN_NSDSAI_SST = "pan_nsdsai_sst";
    public static final String PAN_OBJECT_ID = "pan_object_id";

    public static final String PAN_OBJECTNAME = "pan_objectname";
    public static final String PAN_PARENT_SESSION_ID = "pan_parent_session_id";
    public static final String PAN_PARENT_START_TIME = "pan_parent_start_time";
    public static final String PAN_PARTIAL_HASH = "pan_partial_hash";
    public static final String PAN_PCAP_ID = "pan_pcap_id";
    public static final String PAN_PPID = "pan_ppid";

    public static final String PAN_SCTP_CHUNKS_RX = "pan_sctp_chunks_rx";
    public static final String PAN_SCTP_CHUNKS_SUM = "pan_sctp_chunks_sum";
    public static final String PAN_SCTP_CHUNKS_TX = "pan_sctp_chunks_tx";
    public static final String PAN_SDWAN_CLUSTER = "pan_sdwan_cluster";
    public static final String PAN_SDWAN_CLUSTER_TYPE = "pan_sdwan_cluster_type";

    public static final String PAN_SDWAN_DEVICE_TYPE = "pan_sdwan_device_type";
    public static final String PAN_SDWAN_POLICY_ID = "pan_sdwan_policyid";
    public static final String PAN_SDWAN_SITE_NAME = "pan_sdwan_site_name";
    public static final String PAN_SELECTION_TYPE = "pan_selection_type";
    public static final String PAN_SESSION_END_REASON = "pan_session_end_reason";
    public static final String PAN_SESSION_OWNER = "pan_session_owner";
    public static final String PAN_SOURCE_PROFILE = "pan_source_profile";
    public static final String PAN_SOURCE_REGION = "pan_source_region";
    public static final String PAN_SOURCE_USER = "pan_source_user";

    public static final String PAN_SRC_DAG = "pan_src_dag";
    public static final String PAN_SRC_EDL = "pan_src_edl";
    public static final String PAN_TIMEOUT = "pan_timeout";
    public static final String PAN_TUNNEL_ID = "pan_tunnel_id";
    public static final String PAN_TUNNEL_STAGE = "pan_tunnel_stage";
    public static final String PAN_URL_INDEX = "pan_url_index";
    public static final String PAN_USER_GROUP_FLAGS = "pan_user_group_flags";
    public static final String PAN_WILDFIRE_HASH = "pan_wildfire_hash";
    public static final String PAN_WILDFIRE_REPORT_ID = "pan_wildfire_report_id";
}
