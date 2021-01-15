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
package org.graylog2.audit;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

public class AuditEventTypes implements PluginAuditEventTypes {
    public static final String NAMESPACE = "server";
    private static final String PREFIX = NAMESPACE + ":";

    public static final String ALARM_CALLBACK_CREATE = PREFIX + "alarm_callback:create";
    public static final String ALARM_CALLBACK_DELETE = PREFIX + "alarm_callback:delete";
    public static final String ALARM_CALLBACK_UPDATE = PREFIX + "alarm_callback:update";
    public static final String ALERT_CONDITION_CREATE = PREFIX + "alert_condition:create";
    public static final String ALERT_CONDITION_DELETE = PREFIX + "alert_condition:delete";
    public static final String ALERT_CONDITION_UPDATE = PREFIX + "alert_condition:update";
    public static final String ALERT_RECEIVER_CREATE = PREFIX + "alert_receiver:create";
    public static final String ALERT_RECEIVER_DELETE = PREFIX + "alert_receiver:delete";
    public static final String ALERT_RECEIVER_UPDATE = PREFIX + "alert_receiver:update";
    public static final String AUTHENTICATION_HTTP_HEADER_CONFIG_UPDATE = PREFIX + "authentication_http_header_config:update";
    public static final String AUTHENTICATION_PROVIDER_CONFIGURATION_UPDATE = PREFIX + "authentication_provider_configuration:update";
    public static final String CLUSTER_CONFIGURATION_CREATE = PREFIX + "cluster_configuration:create";
    public static final String CLUSTER_CONFIGURATION_DELETE = PREFIX + "cluster_configuration:delete";
    public static final String CLUSTER_CONFIGURATION_UPDATE = PREFIX + "cluster_configuration:update";
    public static final String CONTENT_PACK_CREATE = PREFIX + "content_pack:create";
    public static final String CONTENT_PACK_DELETE = PREFIX + "content_pack:delete";
    public static final String CONTENT_PACK_DELETE_REV = PREFIX + "content_pack:delete_rev";
    public static final String CONTENT_PACK_INSTALL = PREFIX + "content_pack:install";
    public static final String CONTENT_PACK_UNINSTALL = PREFIX + "content_pack:uninstall";
    public static final String DASHBOARD_CREATE = PREFIX + "dashboard:create";
    public static final String DASHBOARD_DELETE = PREFIX + "dashboard:delete";
    public static final String DASHBOARD_UPDATE = PREFIX + "dashboard:update";
    public static final String DASHBOARD_WIDGET_CREATE = PREFIX + "dashboard_widget:create";
    public static final String DASHBOARD_WIDGET_DELETE = PREFIX + "dashboard_widget:delete";
    public static final String DASHBOARD_WIDGET_POSITIONS_UPDATE = PREFIX + "dashboard_widget_positions:update";
    public static final String DASHBOARD_WIDGET_UPDATE = PREFIX + "dashboard_widget:update";
    public static final String ES_INDEX_CLOSE = PREFIX + "es_index:close";
    public static final String ES_INDEX_CREATE = PREFIX + "es_index:create";
    public static final String ES_INDEX_DELETE = PREFIX + "es_index:delete";
    public static final String ES_INDEX_OPEN = PREFIX + "es_index:open";
    public static final String ES_INDEX_RANGE_CREATE = PREFIX + "es_index_range:create";
    public static final String ES_INDEX_RANGE_DELETE = PREFIX + "es_index_range:delete";
    public static final String ES_INDEX_RANGE_UPDATE_JOB = PREFIX + "es_index_range_update_job:start";
    public static final String ES_INDEX_RETENTION_CLOSE = PREFIX + "es_index_retention:close";
    public static final String ES_INDEX_RETENTION_DELETE = PREFIX + "es_index_retention:delete";
    public static final String ES_INDEX_RETENTION_STRATEGY_UPDATE = PREFIX + "es_index_retention_strategy:update";
    public static final String ES_INDEX_ROTATION_COMPLETE = PREFIX + "es_index_rotation:complete";
    public static final String ES_INDEX_ROTATION_STRATEGY_UPDATE = PREFIX + "es_index_rotation_strategy:update";
    public static final String ES_INDEX_TEMPLATE_UPDATE = PREFIX + "es_index_template:update";
    public static final String ES_WRITE_INDEX_UPDATE = PREFIX + "es_write_index:update";
    public static final String ES_WRITE_INDEX_UPDATE_JOB_START = PREFIX + "es_write_index_update_job:start";
    public static final String EXTRACTOR_CREATE = PREFIX + "extractor:create";
    public static final String EXTRACTOR_DELETE = PREFIX + "extractor:delete";
    public static final String EXTRACTOR_ORDER_UPDATE = PREFIX + "extractor_order:update";
    public static final String EXTRACTOR_UPDATE = PREFIX + "extractor:update";
    public static final String GETTING_STARTED_GUIDE_OPT_OUT_CREATE = PREFIX + "getting_started_guide_opt_out:create";
    public static final String GRANTS_UPDATE = PREFIX + "grants:update";
    public static final String GROK_PATTERN_CREATE = PREFIX + "grok_pattern:create";
    public static final String GROK_PATTERN_DELETE = PREFIX + "grok_pattern:delete";
    public static final String GROK_PATTERN_IMPORT_CREATE = PREFIX + "grok_pattern_import:create";
    public static final String GROK_PATTERN_UPDATE = PREFIX + "grok_pattern:update";
    public static final String INDEX_SET_CREATE = PREFIX + "index_set:create";
    public static final String INDEX_SET_DELETE = PREFIX + "index_set:delete";
    public static final String INDEX_SET_UPDATE = PREFIX + "index_set:update";
    public static final String LOAD_BALANCER_STATUS_UPDATE = PREFIX + "load_balancer_status:update";
    public static final String LOG_LEVEL_UPDATE = PREFIX + "log_level:update";
    public static final String LOOKUP_ADAPTER_CREATE = PREFIX + "lut_adapter:create";
    public static final String LOOKUP_ADAPTER_DELETE = PREFIX + "lut_adapter:delete";
    public static final String LOOKUP_ADAPTER_UPDATE = PREFIX + "lut_adapter:update";
    public static final String LOOKUP_CACHE_CREATE = PREFIX + "lut_cache:create";
    public static final String LOOKUP_CACHE_DELETE = PREFIX + "lut_cache:delete";
    public static final String LOOKUP_CACHE_UPDATE = PREFIX + "lut_cache:update";
    public static final String LOOKUP_TABLE_CREATE = PREFIX + "lut_table:create";
    public static final String LOOKUP_TABLE_DELETE = PREFIX + "lut_table:delete";
    public static final String LOOKUP_TABLE_UPDATE = PREFIX + "lut_table:update";
    public static final String MESSAGE_DECORATOR_CREATE = PREFIX + "message_decorator:create";
    public static final String MESSAGE_DECORATOR_DELETE = PREFIX + "message_decorator:delete";
    public static final String MESSAGE_DECORATOR_UPDATE = PREFIX + "message_decorator:update";
    public static final String MESSAGE_INPUT_CREATE = PREFIX + "message_input:create";
    public static final String MESSAGE_INPUT_DELETE = PREFIX + "message_input:delete";
    public static final String MESSAGE_INPUT_START = PREFIX + "message_input:start";
    public static final String MESSAGE_INPUT_STOP = PREFIX + "message_input:stop";
    public static final String MESSAGE_INPUT_UPDATE = PREFIX + "message_input:update";
    public static final String MESSAGE_OUTPUT_CREATE = PREFIX + "message_output:create";
    public static final String MESSAGE_OUTPUT_DELETE = PREFIX + "message_output:delete";
    public static final String MESSAGE_OUTPUT_START = PREFIX + "message_output:start";
    public static final String MESSAGE_OUTPUT_STOP = PREFIX + "message_output:stop";
    public static final String MESSAGE_OUTPUT_UPDATE = PREFIX + "message_output:update";
    public static final String MESSAGE_PROCESSING_LOCK = PREFIX + "message_processing:lock";
    public static final String MESSAGE_PROCESSING_START = PREFIX + "message_processing:start";
    public static final String MESSAGE_PROCESSING_STOP = PREFIX + "message_processing:stop";
    public static final String MESSAGE_PROCESSING_UNLOCK = PREFIX + "message_processing:unlock";
    public static final String MESSAGE_PROCESSOR_CONFIGURATION_UPDATE = PREFIX + "message_processor_configuration:update";
    public static final String NODE_SHUTDOWN_COMPLETE = PREFIX + "node_shutdown:complete";
    public static final String NODE_SHUTDOWN_INITIATE = PREFIX + "node_shutdown:initiate";
    public static final String NODE_STARTUP_COMPLETE = PREFIX + "node_startup:complete";
    public static final String NODE_STARTUP_INITIATE = PREFIX + "node_startup:initiate";
    public static final String ROLE_CREATE = PREFIX + "role:create";
    public static final String ROLE_DELETE = PREFIX + "role:delete";
    public static final String ROLE_MEMBERSHIP_DELETE = PREFIX + "role_membership:delete";
    public static final String ROLE_MEMBERSHIP_UPDATE = PREFIX + "role_membership:update";
    public static final String ROLE_UPDATE = PREFIX + "role:update";
    public static final String SAVED_SEARCH_CREATE = PREFIX + "saved_search:create";
    public static final String SAVED_SEARCH_DELETE = PREFIX + "saved_search:delete";
    public static final String SAVED_SEARCH_UPDATE = PREFIX + "saved_search:update";
    public static final String SESSION_CREATE = PREFIX + "session:create";
    public static final String SESSION_DELETE = PREFIX + "session:delete";
    public static final String STATIC_FIELD_CREATE = PREFIX + "static_field:create";
    public static final String STATIC_FIELD_DELETE = PREFIX + "static_field:delete";
    public static final String STREAM_CREATE = PREFIX + "stream:create";
    public static final String STREAM_DELETE = PREFIX + "stream:delete";
    public static final String STREAM_OUTPUT_ASSIGNMENT_CREATE = PREFIX + "stream_output_assignment:create";
    public static final String STREAM_OUTPUT_ASSIGNMENT_DELETE = PREFIX + "stream_output_assignment:delete";
    public static final String STREAM_RULE_CREATE = PREFIX + "stream_rule:create";
    public static final String STREAM_RULE_DELETE = PREFIX + "stream_rule:delete";
    public static final String STREAM_RULE_UPDATE = PREFIX + "stream_rule:update";
    public static final String STREAM_START = PREFIX + "stream:start";
    public static final String STREAM_STOP = PREFIX + "stream:stop";
    public static final String STREAM_UPDATE = PREFIX + "stream:update";
    public static final String SYSTEM_JOB_START = PREFIX + "system_job:start";
    public static final String SYSTEM_JOB_STOP = PREFIX + "system_job:stop";
    public static final String SYSTEM_NOTIFICATION_CREATE = PREFIX + "system_notification:create";
    public static final String SYSTEM_NOTIFICATION_DELETE = PREFIX + "system_notification:delete";
    public static final String URL_WHITELIST_UPDATE = PREFIX + "url_whitelist:update";
    public static final String USER_ACCESS_TOKEN_CREATE = PREFIX + "user_access_token:create";
    public static final String USER_ACCESS_TOKEN_DELETE = PREFIX + "user_access_token:delete";
    public static final String USER_CREATE = PREFIX + "user:create";
    public static final String USER_DELETE = PREFIX + "user:delete";
    public static final String USER_PASSWORD_UPDATE = PREFIX + "user_password:update";
    public static final String USER_PERMISSIONS_UPDATE = PREFIX + "user_permissions:update";
    public static final String USER_PERMISSIONS_DELETE = PREFIX + "user_permissions:delete";
    public static final String USER_PREFERENCES_UPDATE = PREFIX + "user_preferences:update";
    public static final String USER_UPDATE = PREFIX + "user:update";

    private static final ImmutableSet<String> EVENT_TYPES = ImmutableSet.<String>builder()
            .add(ALARM_CALLBACK_CREATE)
            .add(ALARM_CALLBACK_DELETE)
            .add(ALARM_CALLBACK_UPDATE)
            .add(ALERT_CONDITION_CREATE)
            .add(ALERT_CONDITION_DELETE)
            .add(ALERT_CONDITION_UPDATE)
            .add(ALERT_RECEIVER_CREATE)
            .add(ALERT_RECEIVER_DELETE)
            .add(ALERT_RECEIVER_UPDATE)
            .add(AUTHENTICATION_HTTP_HEADER_CONFIG_UPDATE)
            .add(AUTHENTICATION_PROVIDER_CONFIGURATION_UPDATE)
            .add(CLUSTER_CONFIGURATION_CREATE)
            .add(CLUSTER_CONFIGURATION_DELETE)
            .add(CLUSTER_CONFIGURATION_UPDATE)
            .add(CONTENT_PACK_CREATE)
            .add(CONTENT_PACK_DELETE)
            .add(CONTENT_PACK_DELETE_REV)
            .add(CONTENT_PACK_INSTALL)
            .add(CONTENT_PACK_UNINSTALL)
            .add(DASHBOARD_CREATE)
            .add(DASHBOARD_DELETE)
            .add(DASHBOARD_UPDATE)
            .add(DASHBOARD_WIDGET_CREATE)
            .add(DASHBOARD_WIDGET_DELETE)
            .add(DASHBOARD_WIDGET_POSITIONS_UPDATE)
            .add(DASHBOARD_WIDGET_UPDATE)
            .add(ES_INDEX_CLOSE)
            .add(ES_INDEX_CREATE)
            .add(ES_INDEX_DELETE)
            .add(ES_INDEX_OPEN)
            .add(ES_INDEX_RANGE_CREATE)
            .add(ES_INDEX_RANGE_DELETE)
            .add(ES_INDEX_RANGE_UPDATE_JOB)
            .add(ES_INDEX_RETENTION_CLOSE)
            .add(ES_INDEX_RETENTION_DELETE)
            .add(ES_INDEX_RETENTION_STRATEGY_UPDATE)
            .add(ES_INDEX_ROTATION_COMPLETE)
            .add(ES_INDEX_ROTATION_STRATEGY_UPDATE)
            .add(ES_INDEX_TEMPLATE_UPDATE)
            .add(ES_WRITE_INDEX_UPDATE)
            .add(ES_WRITE_INDEX_UPDATE_JOB_START)
            .add(EXTRACTOR_CREATE)
            .add(EXTRACTOR_DELETE)
            .add(EXTRACTOR_ORDER_UPDATE)
            .add(EXTRACTOR_UPDATE)
            .add(GETTING_STARTED_GUIDE_OPT_OUT_CREATE)
            .add(GRANTS_UPDATE)
            .add(GROK_PATTERN_CREATE)
            .add(GROK_PATTERN_DELETE)
            .add(GROK_PATTERN_IMPORT_CREATE)
            .add(GROK_PATTERN_UPDATE)
            .add(INDEX_SET_CREATE)
            .add(INDEX_SET_DELETE)
            .add(INDEX_SET_UPDATE)
            .add(LOAD_BALANCER_STATUS_UPDATE)
            .add(LOG_LEVEL_UPDATE)
            .add(LOOKUP_ADAPTER_CREATE)
            .add(LOOKUP_ADAPTER_DELETE)
            .add(LOOKUP_ADAPTER_UPDATE)
            .add(LOOKUP_CACHE_CREATE)
            .add(LOOKUP_CACHE_DELETE)
            .add(LOOKUP_CACHE_UPDATE)
            .add(LOOKUP_TABLE_CREATE)
            .add(LOOKUP_TABLE_DELETE)
            .add(LOOKUP_TABLE_UPDATE)
            .add(MESSAGE_DECORATOR_CREATE)
            .add(MESSAGE_DECORATOR_DELETE)
            .add(MESSAGE_DECORATOR_UPDATE)
            .add(MESSAGE_INPUT_CREATE)
            .add(MESSAGE_INPUT_DELETE)
            .add(MESSAGE_INPUT_START)
            .add(MESSAGE_INPUT_STOP)
            .add(MESSAGE_INPUT_UPDATE)
            .add(MESSAGE_OUTPUT_CREATE)
            .add(MESSAGE_OUTPUT_DELETE)
            .add(MESSAGE_OUTPUT_START)
            .add(MESSAGE_OUTPUT_STOP)
            .add(MESSAGE_OUTPUT_UPDATE)
            .add(MESSAGE_PROCESSING_LOCK)
            .add(MESSAGE_PROCESSING_START)
            .add(MESSAGE_PROCESSING_STOP)
            .add(MESSAGE_PROCESSING_UNLOCK)
            .add(MESSAGE_PROCESSOR_CONFIGURATION_UPDATE)
            .add(NODE_SHUTDOWN_COMPLETE)
            .add(NODE_SHUTDOWN_INITIATE)
            .add(NODE_STARTUP_COMPLETE)
            .add(NODE_STARTUP_INITIATE)
            .add(ROLE_CREATE)
            .add(ROLE_DELETE)
            .add(ROLE_MEMBERSHIP_DELETE)
            .add(ROLE_MEMBERSHIP_UPDATE)
            .add(ROLE_UPDATE)
            .add(SAVED_SEARCH_CREATE)
            .add(SAVED_SEARCH_DELETE)
            .add(SAVED_SEARCH_UPDATE)
            .add(SESSION_CREATE)
            .add(SESSION_DELETE)
            .add(STATIC_FIELD_CREATE)
            .add(STATIC_FIELD_DELETE)
            .add(STREAM_CREATE)
            .add(STREAM_DELETE)
            .add(STREAM_OUTPUT_ASSIGNMENT_CREATE)
            .add(STREAM_OUTPUT_ASSIGNMENT_DELETE)
            .add(STREAM_RULE_CREATE)
            .add(STREAM_RULE_DELETE)
            .add(STREAM_RULE_UPDATE)
            .add(STREAM_START)
            .add(STREAM_STOP)
            .add(STREAM_UPDATE)
            .add(SYSTEM_JOB_START)
            .add(SYSTEM_JOB_STOP)
            .add(SYSTEM_NOTIFICATION_CREATE)
            .add(SYSTEM_NOTIFICATION_DELETE)
            .add(URL_WHITELIST_UPDATE)
            .add(USER_ACCESS_TOKEN_CREATE)
            .add(USER_ACCESS_TOKEN_DELETE)
            .add(USER_CREATE)
            .add(USER_DELETE)
            .add(USER_PASSWORD_UPDATE)
            .add(USER_PERMISSIONS_UPDATE)
            .add(USER_PERMISSIONS_DELETE)
            .add(USER_PREFERENCES_UPDATE)
            .add(USER_UPDATE)
            .build();

    @Override
    public Set<String> auditEventTypes() {
        return EVENT_TYPES;
    }
}
