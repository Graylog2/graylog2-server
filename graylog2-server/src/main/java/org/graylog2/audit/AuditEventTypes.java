/**
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
package org.graylog2.audit;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

public class AuditEventTypes implements PluginAuditEventTypes {
    private static final String NAMESPACE = "server:";

    public static final String ALARM_CALLBACK_CREATE = NAMESPACE + "alarm_callback:create";
    public static final String ALARM_CALLBACK_DELETE = NAMESPACE + "alarm_callback:delete";
    public static final String ALARM_CALLBACK_UPDATE = NAMESPACE + "alarm_callback:update";
    public static final String ALERT_CONDITION_CREATE = NAMESPACE + "alert_condition:create";
    public static final String ALERT_CONDITION_DELETE = NAMESPACE + "alert_condition:delete";
    public static final String ALERT_CONDITION_UPDATE = NAMESPACE + "alert_condition:update";
    public static final String ALERT_RECEIVER_CREATE = NAMESPACE + "alert_receiver:create";
    public static final String ALERT_RECEIVER_DELETE = NAMESPACE + "alert_receiver:delete";
    public static final String ALERT_RECEIVER_UPDATE = NAMESPACE + "alert_receiver:update";
    public static final String AUTHENTICATION_PROVIDER_CONFIGURATION_UPDATE = NAMESPACE + "authentication_provider_configuration:update";
    public static final String BLACKLIST_FILTER_CREATE = NAMESPACE + "blacklist_filter:create";
    public static final String BLACKLIST_FILTER_DELETE = NAMESPACE + "blacklist_filter:delete";
    public static final String BLACKLIST_FILTER_UPDATE = NAMESPACE + "blacklist_filter:update";
    public static final String CLUSTER_CONFIGURATION_CREATE = NAMESPACE + "cluster_configuration:create";
    public static final String CLUSTER_CONFIGURATION_DELETE = NAMESPACE + "cluster_configuration:delete";
    public static final String CLUSTER_CONFIGURATION_UPDATE = NAMESPACE + "cluster_configuration:update";
    public static final String CONTENT_PACK_APPLY = NAMESPACE + "content_pack:apply";
    public static final String CONTENT_PACK_CREATE = NAMESPACE + "content_pack:create";
    public static final String CONTENT_PACK_DELETE = NAMESPACE + "content_pack:delete";
    public static final String CONTENT_PACK_EXPORT = NAMESPACE + "content_pack:export";
    public static final String CONTENT_PACK_UPDATE = NAMESPACE + "content_pack:update";
    public static final String DASHBOARD_CREATE = NAMESPACE + "dashboard:create";
    public static final String DASHBOARD_DELETE = NAMESPACE + "dashboard:delete";
    public static final String DASHBOARD_UPDATE = NAMESPACE + "dashboard:update";
    public static final String DASHBOARD_WIDGET_CREATE = NAMESPACE + "dashboard_widget:create";
    public static final String DASHBOARD_WIDGET_DELETE = NAMESPACE + "dashboard_widget:delete";
    public static final String DASHBOARD_WIDGET_POSITIONS_UPDATE = NAMESPACE + "dashboard_widget_positions:update";
    public static final String DASHBOARD_WIDGET_UPDATE = NAMESPACE + "dashboard_widget:update";
    public static final String ES_INDEX_CLOSE = NAMESPACE + "es_index:close";
    public static final String ES_INDEX_CREATE = NAMESPACE + "es_index:create";
    public static final String ES_INDEX_DELETE = NAMESPACE + "es_index:delete";
    public static final String ES_INDEX_OPEN = NAMESPACE + "es_index:open";
    public static final String ES_INDEX_RANGE_CREATE = NAMESPACE + "es_index_range:create";
    public static final String ES_INDEX_RANGE_DELETE = NAMESPACE + "es_index_range:delete";
    public static final String ES_INDEX_RANGE_UPDATE = NAMESPACE + "es_index_range:update";
    public static final String ES_INDEX_RETENTION_COMPLETE = NAMESPACE + "es_index_retention:complete";
    public static final String ES_INDEX_RETENTION_INITIATE = NAMESPACE + "es_index_retention:initiate";
    public static final String ES_INDEX_RETENTION_STRATEGY_UPDATE = NAMESPACE + "es_index_retention_strategy:update";
    public static final String ES_INDEX_ROTATION_COMPLETE = NAMESPACE + "es_index_rotation:complete";
    public static final String ES_INDEX_ROTATION_INITIATE = NAMESPACE + "es_index_rotation:initiate";
    public static final String ES_INDEX_ROTATION_STRATEGY_UPDATE = NAMESPACE + "es_index_rotation_strategy:update";
    public static final String ES_WRITE_INDEX_UPDATE = NAMESPACE + "es_write_index:update";
    public static final String EXTRACTOR_CREATE = NAMESPACE + "extractor:create";
    public static final String EXTRACTOR_DELETE = NAMESPACE + "extractor:delete";
    public static final String EXTRACTOR_EXPORT = NAMESPACE + "extractor:export";
    public static final String EXTRACTOR_IMPORT = NAMESPACE + "extractor:import";
    public static final String EXTRACTOR_ORDER_UPDATE = NAMESPACE + "extractor_order:update";
    public static final String EXTRACTOR_UPDATE = NAMESPACE + "extractor:update";
    public static final String GETTING_STARTED_GUIDE_OPT_OUT_CREATE = NAMESPACE + "getting_started_guide_opt_out:create";
    public static final String GROK_PATTERN_CREATE = NAMESPACE + "grok_pattern:create";
    public static final String GROK_PATTERN_DELETE = NAMESPACE + "grok_pattern:delete";
    public static final String GROK_PATTERN_EXPORT = NAMESPACE + "grok_pattern:export";
    public static final String GROK_PATTERN_IMPORT = NAMESPACE + "grok_pattern:import";
    public static final String GROK_PATTERN_UPDATE = NAMESPACE + "grok_pattern:update";
    public static final String LDAP_CONFIGURATION_CREATE = NAMESPACE + "ldap_configuration:create";
    public static final String LDAP_CONFIGURATION_DELETE = NAMESPACE + "ldap_configuration:delete";
    public static final String LDAP_CONFIGURATION_UPDATE = NAMESPACE + "ldap_configuration:update";
    public static final String LDAP_GROUP_MAPPING_CREATE = NAMESPACE + "ldap_group_mapping:create";
    public static final String LDAP_GROUP_MAPPING_DELETE = NAMESPACE + "ldap_group_mapping:delete";
    public static final String LDAP_GROUP_MAPPING_UPDATE = NAMESPACE + "ldap_group_mapping:update";
    public static final String LOAD_BALANCER_STATUS_UPDATE = NAMESPACE + "load_balancer_status:update";
    public static final String LOG_LEVEL_UPDATE = NAMESPACE + "log_level:update";
    public static final String MESSAGE_DECORATOR_CREATE = NAMESPACE + "message_decorator:create";
    public static final String MESSAGE_DECORATOR_DELETE = NAMESPACE + "message_decorator:delete";
    public static final String MESSAGE_DECORATOR_UPDATE = NAMESPACE + "message_decorator:update";
    public static final String MESSAGE_INPUT_CREATE = NAMESPACE + "message_input:create";
    public static final String MESSAGE_INPUT_DELETE = NAMESPACE + "message_input:delete";
    public static final String MESSAGE_INPUT_START = NAMESPACE + "message_input:start";
    public static final String MESSAGE_INPUT_STOP = NAMESPACE + "message_input:stop";
    public static final String MESSAGE_INPUT_UPDATE = NAMESPACE + "message_input:update";
    public static final String MESSAGE_OUTPUT_CREATE = NAMESPACE + "message_output:create";
    public static final String MESSAGE_OUTPUT_DELETE = NAMESPACE + "message_output:delete";
    public static final String MESSAGE_OUTPUT_START = NAMESPACE + "message_output:start";
    public static final String MESSAGE_OUTPUT_STOP = NAMESPACE + "message_output:stop";
    public static final String MESSAGE_OUTPUT_UPDATE = NAMESPACE + "message_output:update";
    public static final String MESSAGE_PROCESSING_LOCK = NAMESPACE + "message_processing:lock";
    public static final String MESSAGE_PROCESSING_START = NAMESPACE + "message_processing:start";
    public static final String MESSAGE_PROCESSING_STOP = NAMESPACE + "message_processing:stop";
    public static final String MESSAGE_PROCESSING_UNLOCK = NAMESPACE + "message_processing:unlock";
    public static final String MESSAGE_PROCESSOR_CONFIGURATION_UPDATE = NAMESPACE + "message_processor_configuration:update";
    public static final String NODE_SHUTDOWN_COMPLETE = NAMESPACE + "node_shutdown:complete";
    public static final String NODE_SHUTDOWN_INITIATE = NAMESPACE + "node_shutdown:initiate";
    public static final String NODE_STARTUP_COMPLETE = NAMESPACE + "node_startup:complete";
    public static final String NODE_STARTUP_INITIATE = NAMESPACE + "node_startup:initiate";
    public static final String ROLE_CREATE = NAMESPACE + "role:create";
    public static final String ROLE_DELETE = NAMESPACE + "role:delete";
    public static final String ROLE_MEMBERSHIP_DELETE = NAMESPACE + "role_membership:delete";
    public static final String ROLE_MEMBERSHIP_UPDATE = NAMESPACE + "role_membership:update";
    public static final String ROLE_UPDATE = NAMESPACE + "role:update";
    public static final String SAVED_SEARCH_CREATE = NAMESPACE + "saved_search:create";
    public static final String SAVED_SEARCH_DELETE = NAMESPACE + "saved_search:delete";
    public static final String SAVED_SEARCH_UPDATE = NAMESPACE + "saved_search:update";
    public static final String SESSION_CREATE = NAMESPACE + "session:create";
    public static final String SESSION_DELETE = NAMESPACE + "session:delete";
    public static final String SESSION_UPDATE = NAMESPACE + "session:update";
    public static final String STATIC_FIELD_CREATE = NAMESPACE + "static_field:create";
    public static final String STATIC_FIELD_DELETE = NAMESPACE + "static_field:delete";
    public static final String STATIC_FIELD_UPDATE = NAMESPACE + "static_field:update";
    public static final String STREAM_CREATE = NAMESPACE + "stream:create";
    public static final String STREAM_DELETE = NAMESPACE + "stream:delete";
    public static final String STREAM_OUTPUT_ASSIGNMENT_CREATE = NAMESPACE + "stream_output_assignment:create";
    public static final String STREAM_OUTPUT_ASSIGNMENT_DELETE = NAMESPACE + "stream_output_assignment:delete";
    public static final String STREAM_RULE_CREATE = NAMESPACE + "stream_rule:create";
    public static final String STREAM_RULE_DELETE = NAMESPACE + "stream_rule:delete";
    public static final String STREAM_RULE_UPDATE = NAMESPACE + "stream_rule:update";
    public static final String STREAM_START = NAMESPACE + "stream:start";
    public static final String STREAM_STOP = NAMESPACE + "stream:stop";
    public static final String STREAM_UPDATE = NAMESPACE + "stream:update";
    public static final String SYSTEM_JOB_START = NAMESPACE + "system_job:start";
    public static final String SYSTEM_JOB_STOP = NAMESPACE + "system_job:stop";
    public static final String SYSTEM_NOTIFICATION_CREATE = NAMESPACE + "system_notification:create";
    public static final String SYSTEM_NOTIFICATION_DELETE = NAMESPACE + "system_notification:delete";
    public static final String USER_ACCESS_TOKEN_CREATE = NAMESPACE + "user_access_token:create";
    public static final String USER_ACCESS_TOKEN_DELETE = NAMESPACE + "user_access_token:delete";
    public static final String USER_ACCESS_TOKEN_UPDATE = NAMESPACE + "user_access_token:update";
    public static final String USER_CREATE = NAMESPACE + "user:create";
    public static final String USER_DELETE = NAMESPACE + "user:delete";
    public static final String USER_PASSWORD_UPDATE = NAMESPACE + "user_password:update";
    public static final String USER_PERMISSIONS_UPDATE = NAMESPACE + "user_permissions:update";
    public static final String USER_PERMISSIONS_DELETE = NAMESPACE + "user_permissions:delete";
    public static final String USER_PREFERENCES_UPDATE = NAMESPACE + "user_preferences:update";
    public static final String USER_UPDATE = NAMESPACE + "user:update";

    private static final Set<String> EVENT_TYPES = ImmutableSet.<String>builder()
            .add(ALARM_CALLBACK_CREATE)
            .add(ALARM_CALLBACK_DELETE)
            .add(ALARM_CALLBACK_UPDATE)
            .add(ALERT_CONDITION_CREATE)
            .add(ALERT_CONDITION_DELETE)
            .add(ALERT_CONDITION_UPDATE)
            .add(ALERT_RECEIVER_CREATE)
            .add(ALERT_RECEIVER_DELETE)
            .add(ALERT_RECEIVER_UPDATE)
            .add(AUTHENTICATION_PROVIDER_CONFIGURATION_UPDATE)
            .add(BLACKLIST_FILTER_CREATE)
            .add(BLACKLIST_FILTER_DELETE)
            .add(BLACKLIST_FILTER_UPDATE)
            .add(CLUSTER_CONFIGURATION_CREATE)
            .add(CLUSTER_CONFIGURATION_DELETE)
            .add(CLUSTER_CONFIGURATION_UPDATE)
            .add(CONTENT_PACK_APPLY)
            .add(CONTENT_PACK_CREATE)
            .add(CONTENT_PACK_DELETE)
            .add(CONTENT_PACK_EXPORT)
            .add(CONTENT_PACK_UPDATE)
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
            .add(ES_INDEX_RANGE_UPDATE)
            .add(ES_INDEX_RETENTION_COMPLETE)
            .add(ES_INDEX_RETENTION_INITIATE)
            .add(ES_INDEX_RETENTION_STRATEGY_UPDATE)
            .add(ES_INDEX_ROTATION_COMPLETE)
            .add(ES_INDEX_ROTATION_INITIATE)
            .add(ES_INDEX_ROTATION_STRATEGY_UPDATE)
            .add(ES_WRITE_INDEX_UPDATE)
            .add(EXTRACTOR_CREATE)
            .add(EXTRACTOR_DELETE)
            .add(EXTRACTOR_EXPORT)
            .add(EXTRACTOR_IMPORT)
            .add(EXTRACTOR_ORDER_UPDATE)
            .add(EXTRACTOR_UPDATE)
            .add(GETTING_STARTED_GUIDE_OPT_OUT_CREATE)
            .add(GROK_PATTERN_CREATE)
            .add(GROK_PATTERN_DELETE)
            .add(GROK_PATTERN_EXPORT)
            .add(GROK_PATTERN_IMPORT)
            .add(GROK_PATTERN_UPDATE)
            .add(LDAP_CONFIGURATION_CREATE)
            .add(LDAP_CONFIGURATION_DELETE)
            .add(LDAP_CONFIGURATION_UPDATE)
            .add(LDAP_GROUP_MAPPING_CREATE)
            .add(LDAP_GROUP_MAPPING_DELETE)
            .add(LDAP_GROUP_MAPPING_UPDATE)
            .add(LOAD_BALANCER_STATUS_UPDATE)
            .add(LOG_LEVEL_UPDATE)
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
            .add(SESSION_UPDATE)
            .add(STATIC_FIELD_CREATE)
            .add(STATIC_FIELD_DELETE)
            .add(STATIC_FIELD_UPDATE)
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
            .add(USER_ACCESS_TOKEN_CREATE)
            .add(USER_ACCESS_TOKEN_DELETE)
            .add(USER_ACCESS_TOKEN_UPDATE)
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
