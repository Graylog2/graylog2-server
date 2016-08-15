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

public class AuditActions implements PluginAuditActions {
    private static final String URN_PREFIX = "urn:graylog:server:";

    public static final String ALARM_CALLBACK_CREATE = URN_PREFIX + "alarm_callback.create";
    public static final String ALARM_CALLBACK_DELETE = URN_PREFIX + "alarm_callback.delete";
    public static final String ALARM_CALLBACK_UPDATE = URN_PREFIX + "alarm_callback.update";
    public static final String ALERT_CONDITION_CREATE = URN_PREFIX + "alert_condition.create";
    public static final String ALERT_CONDITION_DELETE = URN_PREFIX + "alert_condition.delete";
    public static final String ALERT_CONDITION_UPDATE = URN_PREFIX + "alert_condition.update";
    public static final String ALERT_RECEIVER_CREATE = URN_PREFIX + "alert_receiver.create";
    public static final String ALERT_RECEIVER_DELETE = URN_PREFIX + "alert_receiver.delete";
    public static final String ALERT_RECEIVER_UPDATE = URN_PREFIX + "alert_receiver.update";
    public static final String AUTHENTICATION_PROVIDER_CONFIGURATION_UPDATE = URN_PREFIX + "authentication_provider_configuration.update";
    public static final String BLACKLIST_FILTER_CREATE = URN_PREFIX + "blacklist_filter.create";
    public static final String BLACKLIST_FILTER_DELETE = URN_PREFIX + "blacklist_filter.delete";
    public static final String BLACKLIST_FILTER_UPDATE = URN_PREFIX + "blacklist_filter.update";
    public static final String CLUSTER_CONFIGURATION_CREATE = URN_PREFIX + "cluster_configuration.create";
    public static final String CLUSTER_CONFIGURATION_DELETE = URN_PREFIX + "cluster_configuration.delete";
    public static final String CLUSTER_CONFIGURATION_UPDATE = URN_PREFIX + "cluster_configuration.update";
    public static final String CONTENT_PACK_APPLY = URN_PREFIX + "content_pack.apply";
    public static final String CONTENT_PACK_CREATE = URN_PREFIX + "content_pack.create";
    public static final String CONTENT_PACK_DELETE = URN_PREFIX + "content_pack.delete";
    public static final String CONTENT_PACK_EXPORT = URN_PREFIX + "content_pack.export";
    public static final String CONTENT_PACK_UPDATE = URN_PREFIX + "content_pack.update";
    public static final String DASHBOARD_CREATE = URN_PREFIX + "dashboard.create";
    public static final String DASHBOARD_DELETE = URN_PREFIX + "dashboard.delete";
    public static final String DASHBOARD_UPDATE = URN_PREFIX + "dashboard.update";
    public static final String DASHBOARD_WIDGET_CREATE = URN_PREFIX + "dashboard_widget.create";
    public static final String DASHBOARD_WIDGET_DELETE = URN_PREFIX + "dashboard_widget.delete";
    public static final String DASHBOARD_WIDGET_POSITIONS_UPDATE = URN_PREFIX + "dashboard_widget_positions.update";
    public static final String DASHBOARD_WIDGET_UPDATE = URN_PREFIX + "dashboard_widget.update";
    public static final String ES_INDEX_CLOSE = URN_PREFIX + "es_index.close";
    public static final String ES_INDEX_CREATE = URN_PREFIX + "es_index.create";
    public static final String ES_INDEX_DELETE = URN_PREFIX + "es_index.delete";
    public static final String ES_INDEX_OPEN = URN_PREFIX + "es_index.open";
    public static final String ES_INDEX_RANGE_CREATE = URN_PREFIX + "es_index_range.create";
    public static final String ES_INDEX_RANGE_DELETE = URN_PREFIX + "es_index_range.delete";
    public static final String ES_INDEX_RANGE_UPDATE = URN_PREFIX + "es_index_range.update";
    public static final String ES_INDEX_RETENTION_COMPLETE = URN_PREFIX + "es_index_retention.complete";
    public static final String ES_INDEX_RETENTION_INITIATE = URN_PREFIX + "es_index_retention.initiate";
    public static final String ES_INDEX_RETENTION_STRATEGY_UPDATE = URN_PREFIX + "es_index_retention_strategy.update";
    public static final String ES_INDEX_ROTATION_COMPLETE = URN_PREFIX + "es_index_rotation.complete";
    public static final String ES_INDEX_ROTATION_INITIATE = URN_PREFIX + "es_index_rotation.initiate";
    public static final String ES_INDEX_ROTATION_STRATEGY_UPDATE = URN_PREFIX + "es_index_rotation_strategy.update";
    public static final String ES_WRITE_INDEX_UPDATE = URN_PREFIX + "es_write_index.update";
    public static final String EXTRACTOR_CREATE = URN_PREFIX + "extractor.create";
    public static final String EXTRACTOR_DELETE = URN_PREFIX + "extractor.delete";
    public static final String EXTRACTOR_EXPORT = URN_PREFIX + "extractor.export";
    public static final String EXTRACTOR_IMPORT = URN_PREFIX + "extractor.import";
    public static final String EXTRACTOR_ORDER_UPDATE = URN_PREFIX + "extractor_order.update";
    public static final String EXTRACTOR_UPDATE = URN_PREFIX + "extractor.update";
    public static final String GETTING_STARTED_GUIDE_OPT_OUT_CREATE = URN_PREFIX + "getting_started_guide_opt_out.create";
    public static final String GROK_PATTERN_CREATE = URN_PREFIX + "grok_pattern.create";
    public static final String GROK_PATTERN_DELETE = URN_PREFIX + "grok_pattern.delete";
    public static final String GROK_PATTERN_EXPORT = URN_PREFIX + "grok_pattern.export";
    public static final String GROK_PATTERN_IMPORT = URN_PREFIX + "grok_pattern.import";
    public static final String GROK_PATTERN_UPDATE = URN_PREFIX + "grok_pattern.update";
    public static final String LDAP_CONFIGURATION_CREATE = URN_PREFIX + "ldap_configuration.create";
    public static final String LDAP_CONFIGURATION_DELETE = URN_PREFIX + "ldap_configuration.delete";
    public static final String LDAP_CONFIGURATION_UPDATE = URN_PREFIX + "ldap_configuration.update";
    public static final String LDAP_GROUP_MAPPING_CREATE = URN_PREFIX + "ldap_group_mapping.create";
    public static final String LDAP_GROUP_MAPPING_DELETE = URN_PREFIX + "ldap_group_mapping.delete";
    public static final String LDAP_GROUP_MAPPING_UPDATE = URN_PREFIX + "ldap_group_mapping.update";
    public static final String LOAD_BALANCER_STATUS_UPDATE = URN_PREFIX + "load_balancer_status.update";
    public static final String LOG_LEVEL_UPDATE = URN_PREFIX + "log_level.update";
    public static final String MESSAGE_DECORATOR_CREATE = URN_PREFIX + "message_decorator.create";
    public static final String MESSAGE_DECORATOR_DELETE = URN_PREFIX + "message_decorator.delete";
    public static final String MESSAGE_DECORATOR_UPDATE = URN_PREFIX + "message_decorator.update";
    public static final String MESSAGE_INPUT_CREATE = URN_PREFIX + "message_input.create";
    public static final String MESSAGE_INPUT_DELETE = URN_PREFIX + "message_input.delete";
    public static final String MESSAGE_INPUT_START = URN_PREFIX + "message_input.start";
    public static final String MESSAGE_INPUT_STOP = URN_PREFIX + "message_input.stop";
    public static final String MESSAGE_INPUT_UPDATE = URN_PREFIX + "message_input.update";
    public static final String MESSAGE_OUTPUT_CREATE = URN_PREFIX + "message_output.create";
    public static final String MESSAGE_OUTPUT_DELETE = URN_PREFIX + "message_output.delete";
    public static final String MESSAGE_OUTPUT_START = URN_PREFIX + "message_output.start";
    public static final String MESSAGE_OUTPUT_STOP = URN_PREFIX + "message_output.stop";
    public static final String MESSAGE_OUTPUT_UPDATE = URN_PREFIX + "message_output.update";
    public static final String MESSAGE_PROCESSING_LOCK = URN_PREFIX + "message_processing.lock";
    public static final String MESSAGE_PROCESSING_START = URN_PREFIX + "message_processing.start";
    public static final String MESSAGE_PROCESSING_STOP = URN_PREFIX + "message_processing.stop";
    public static final String MESSAGE_PROCESSING_UNLOCK = URN_PREFIX + "message_processing.unlock";
    public static final String MESSAGE_PROCESSOR_CONFIGURATION_UPDATE = URN_PREFIX + "message_processor_configuration.update";
    public static final String NODE_SHUTDOWN_COMPLETE = URN_PREFIX + "node_shutdown.complete";
    public static final String NODE_SHUTDOWN_INITIATE = URN_PREFIX + "node_shutdown.initiate";
    public static final String NODE_STARTUP_COMPLETE = URN_PREFIX + "node_startup.complete";
    public static final String NODE_STARTUP_INITIATE = URN_PREFIX + "node_startup.initiate";
    public static final String ROLE_CREATE = URN_PREFIX + "role.create";
    public static final String ROLE_DELETE = URN_PREFIX + "role.delete";
    public static final String ROLE_MEMBERSHIP_DELETE = URN_PREFIX + "role_membership.delete";
    public static final String ROLE_MEMBERSHIP_UPDATE = URN_PREFIX + "role_membership.update";
    public static final String ROLE_UPDATE = URN_PREFIX + "role.update";
    public static final String SAVED_SEARCH_CREATE = URN_PREFIX + "saved_search.create";
    public static final String SAVED_SEARCH_DELETE = URN_PREFIX + "saved_search.delete";
    public static final String SAVED_SEARCH_UPDATE = URN_PREFIX + "saved_search.update";
    public static final String SESSION_CREATE = URN_PREFIX + "session.create";
    public static final String SESSION_DELETE = URN_PREFIX + "session.delete";
    public static final String SESSION_UPDATE = URN_PREFIX + "session.update";
    public static final String STATIC_FIELD_CREATE = URN_PREFIX + "static_field.create";
    public static final String STATIC_FIELD_DELETE = URN_PREFIX + "static_field.delete";
    public static final String STATIC_FIELD_UPDATE = URN_PREFIX + "static_field.update";
    public static final String STREAM_CREATE = URN_PREFIX + "stream.create";
    public static final String STREAM_DELETE = URN_PREFIX + "stream.delete";
    public static final String STREAM_OUTPUT_ASSIGNMENT_CREATE = URN_PREFIX + "stream_output_assignment.create";
    public static final String STREAM_OUTPUT_ASSIGNMENT_DELETE = URN_PREFIX + "stream_output_assignment.delete";
    public static final String STREAM_RULE_CREATE = URN_PREFIX + "stream_rule.create";
    public static final String STREAM_RULE_DELETE = URN_PREFIX + "stream_rule.delete";
    public static final String STREAM_RULE_UPDATE = URN_PREFIX + "stream_rule.update";
    public static final String STREAM_START = URN_PREFIX + "stream.start";
    public static final String STREAM_STOP = URN_PREFIX + "stream.stop";
    public static final String STREAM_UPDATE = URN_PREFIX + "stream.update";
    public static final String SYSTEM_JOB_START = URN_PREFIX + "system_job.start";
    public static final String SYSTEM_JOB_STOP = URN_PREFIX + "system_job.stop";
    public static final String SYSTEM_NOTIFICATION_CREATE = URN_PREFIX + "system_notification.create";
    public static final String SYSTEM_NOTIFICATION_DELETE = URN_PREFIX + "system_notification.delete";
    public static final String USER_ACCESS_TOKEN_CREATE = URN_PREFIX + "user_access_token.create";
    public static final String USER_ACCESS_TOKEN_DELETE = URN_PREFIX + "user_access_token.delete";
    public static final String USER_ACCESS_TOKEN_UPDATE = URN_PREFIX + "user_access_token.update";
    public static final String USER_CREATE = URN_PREFIX + "user.create";
    public static final String USER_DELETE = URN_PREFIX + "user.delete";
    public static final String USER_PASSWORD_UPDATE = URN_PREFIX + "user_password.update";
    public static final String USER_PERMISSIONS_UPDATE = URN_PREFIX + "user_permissions.update";
    public static final String USER_PERMISSIONS_DELETE = URN_PREFIX + "user_permissions.delete";
    public static final String USER_PREFERENCES_UPDATE = URN_PREFIX + "user_preferences.update";
    public static final String USER_UPDATE = URN_PREFIX + "user.update";

    private static final Set<String> ACTIONS = ImmutableSet.<String>builder()
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
    public Set<String> auditActions() {
        return ACTIONS;
    }
}
