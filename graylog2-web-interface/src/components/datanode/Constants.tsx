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
export const TIME_UNITS = ['hours', 'days', 'months', 'years'] as const;
export const TIME_UNITS_UPPER = TIME_UNITS.map((unit) => unit.toLocaleUpperCase());

export const MIGRATION_STEP = {
  CA_CONFIGURATION: {
    key: 'CA_CONFIGURATION',
    descriptionKey: 'migration_steps.ca_configuration',
  },
  RENEWAL_POLICY_CONFIGURATION: {
    key: 'RENEWAL_POLICY_CONFIGURATION',
    descriptionKey: 'migration_steps.renewal_policy_configuration',
  },
  COMPATIBILITY_CHECK: {
    key: 'COMPATIBILITY_CHECK',
    descriptionKey: 'migration_steps.compatibility_check',
  },
  MANUAL_MIGRATION_STEP: {
    key: 'MANUAL_MIGRATION_STEP',
    descriptionKey: 'migration_steps.manual_migration_step',
  },
  MIGRATION_FINISHED: {
    key: 'MIGRATION_FINISHED',
    descriptionKey: 'migration_steps.migration_finished',
  },
} as const;

export const MIGRATION_STATE = {
  NEW: {
    key: 'NEW',
    descriptionKey: 'migration_states.new',
  },
  MIGRATION_WELCOME_PAGE: {
    key: 'MIGRATION_WELCOME_PAGE',
    descriptionKey: 'migration_states.migration_welcome_page',
  },
  CA_CREATION_PAGE: {
    key: 'CA_CREATION_PAGE',
    descriptionKey: 'migration_states.ca_creation_page',
  },
  RENEWAL_POLICY_CREATION_PAGE: {
    key: 'RENEWAL_POLICY_CREATION_PAGE',
    descriptionKey: 'migration_states.renewal_policy_creation_page',
  },
  MIGRATION_SELECTION_PAGE: {
    key: 'MIGRATION_SELECTION_PAGE',
    descriptionKey: 'migration_states.migration_selection_page',
  },
  ROLLING_UPGRADE_MIGRATION_WELCOME_PAGE: {
    key: 'ROLLING_UPGRADE_MIGRATION_WELCOME_PAGE',
    descriptionKey: 'migration_states.rolling_upgrade_migration_welcome_page',
  },
  ASK_TO_SHUTDOWN_OLD_CLUSTER: {
    key: 'ASK_TO_SHUTDOWN_OLD_CLUSTER',
    descriptionKey: 'migration_states.ask_to_shutdown_old_cluster',
  },
  MANUALLY_REMOVE_OLD_CONNECTION_STRING_FROM_CONFIG: {
    key: 'MANUALLY_REMOVE_OLD_CONNECTION_STRING_FROM_CONFIG',
    descriptionKey: 'migration_states.manually_remove_old_connection_string_from_config',
  },
  MESSAGE_PROCESSING_STOP: {
    key: 'MESSAGE_PROCESSING_STOP',
    descriptionKey: 'migration_states.message_processing_stop',
  },
  REPLACE_CLUSTER: {
    key: 'REPLACE_CLUSTER',
    descriptionKey: 'migration_states.replace_cluster',
  },
  RESTART_GRAYLOG: {
    key: 'RESTART_GRAYLOG',
    descriptionKey: 'migration_states.restart_graylog',
  },
  DIRECTORY_COMPATIBILITY_CHECK_PAGE: {
    key: 'DIRECTORY_COMPATIBILITY_CHECK_PAGE',
    descriptionKey: 'migration_states.directory_compatibility_check_page',
  },
  PROVISION_ROLLING_UPGRADE_NODES_WITH_CERTIFICATES: {
    key: 'PROVISION_ROLLING_UPGRADE_NODES_WITH_CERTIFICATES',
    descriptionKey: 'migration_states.provision_rolling_upgrade_nodes_with_certificates',
  },
  PROVISION_ROLLING_UPGRADE_NODES_RUNNING: {
    key: 'PROVISION_ROLLING_UPGRADE_NODES_RUNNING',
    descriptionKey: 'migration_states.provision_rolling_upgrade_nodes_running',
  },
  JOURNAL_SIZE_DOWNTIME_WARNING: {
    key: 'JOURNAL_SIZE_DOWNTIME_WARNING',
    descriptionKey: 'migration_states.journal_size_downtime_warning',
  },
  FAILED: {
    key: 'FAILED',
    descriptionKey: 'migration_states.failed',
  },
  FINISHED: {
    key: 'FINISHED',
    descriptionKey: 'migration_states.finished',
  },
} as const;

export const IN_PLACE_MIGRATION_STEPS = [
  MIGRATION_STATE.ROLLING_UPGRADE_MIGRATION_WELCOME_PAGE.key,
  MIGRATION_STATE.DIRECTORY_COMPATIBILITY_CHECK_PAGE.key,
  MIGRATION_STATE.PROVISION_ROLLING_UPGRADE_NODES_RUNNING.key,
  MIGRATION_STATE.JOURNAL_SIZE_DOWNTIME_WARNING.key,
  MIGRATION_STATE.MESSAGE_PROCESSING_STOP.key,
  MIGRATION_STATE.RESTART_GRAYLOG.key,
];

export const MIGRATION_WIZARD_STEPS = [
  MIGRATION_STATE.NEW.key,
  MIGRATION_STATE.MIGRATION_WELCOME_PAGE.key,
  MIGRATION_STATE.CA_CREATION_PAGE.key,
  MIGRATION_STATE.RENEWAL_POLICY_CREATION_PAGE.key,
  MIGRATION_STATE.MIGRATION_SELECTION_PAGE.key,
  MIGRATION_STATE.FINISHED.key,
];

export const MIGRATION_ACTIONS = {
  SHOW_RENEWAL_POLICY_CREATION: {
    key: 'SHOW_RENEWAL_POLICY_CREATION',
    labelKey: 'migration_actions.show_renewal_policy_creation',
  },
  SHOW_MIGRATION_SELECTION: {
    key: 'SHOW_MIGRATION_SELECTION',
    labelKey: 'migration_actions.show_migration_selection',
  },
  RUN_DIRECTORY_COMPATIBILITY_CHECK: {
    key: 'INSTALL_DATANODES_ON_EVERY_NODE',
    labelKey: 'migration_actions.run_directory_compatibility_check',
  },
  PROVISION_DATANODE_CERTIFICATES: {
    key: 'PROVISION_DATANODE_CERTIFICATES',
    labelKey: 'migration_actions.provision_datanode_certificates',
  },
};
export default MIGRATION_STEP;
