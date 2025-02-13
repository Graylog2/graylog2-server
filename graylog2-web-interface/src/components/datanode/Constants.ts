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
    description: 'Configure a certificate authority',
  },
  RENEWAL_POLICY_CONFIGURATION: {
    key: 'RENEWAL_POLICY_CONFIGURATION',
    description: 'Configure a renewal policy',
  },
  COMPATIBILITY_CHECK: {
    key: 'COMPATIBILITY_CHECK',
    description: 'Check OpenSearch compatibility with datanode',
  },
  MANUAL_MIGRATION_STEP: {
    key: 'MANUAL_MIGRATION_STEP',
    description: 'Migration steps.',
  },
  MIGRATION_FINISHED: {
    key: 'MIGRATION_FINISHED',
    description: 'Migration finished',
  },
} as const;

export const MIGRATION_STATE = {
  NEW: {
    key: 'NEW',
    description: 'Migration',
  },
  MIGRATION_WELCOME_PAGE: {
    key: 'MIGRATION_WELCOME_PAGE',
    description: 'Welcome',
  },
  CA_CREATION_PAGE: {
    key: 'CA_CREATION_PAGE',
    description: 'Certificate authority',
  },
  RENEWAL_POLICY_CREATION_PAGE: {
    key: 'RENEWAL_POLICY_CREATION_PAGE',
    description: 'Certificate renewal policy',
  },
  MIGRATION_SELECTION_PAGE: {
    key: 'MIGRATION_SELECTION_PAGE',
    description: 'Migration steps',
  },
  ROLLING_UPGRADE_MIGRATION_WELCOME_PAGE: {
    key: 'ROLLING_UPGRADE_MIGRATION_WELCOME_PAGE',
    description: 'Welcome to In-Place migration',
  },
  ASK_TO_SHUTDOWN_OLD_CLUSTER: {
    key: 'ASK_TO_SHUTDOWN_OLD_CLUSTER',
    description: 'Shut down old cluster',
  },
  MANUALLY_REMOVE_OLD_CONNECTION_STRING_FROM_CONFIG: {
    key: 'MANUALLY_REMOVE_OLD_CONNECTION_STRING_FROM_CONFIG',
    description: 'Remove connection string',
  },
  MESSAGE_PROCESSING_STOP: {
    key: 'MESSAGE_PROCESSING_STOP',
    description: 'Stop message processing',
  },
  REPLACE_CLUSTER: {
    key: 'REPLACE_CLUSTER',
    description: 'Replace existing cluster',
  },
  RESTART_GRAYLOG: {
    key: 'RESTART_GRAYLOG',
    description: 'Update configuration file and restart Graylog',
  },
  REMOTE_REINDEX_WELCOME_PAGE: {
    key: 'REMOTE_REINDEX_WELCOME_PAGE',
    description: 'Remote reindexing migration',
  },
  PROVISION_DATANODE_CERTIFICATES_PAGE: {
    key: 'PROVISION_DATANODE_CERTIFICATES_PAGE',
    description: 'Provision Data Node with certificates',
  },
  PROVISION_DATANODE_CERTIFICATES_RUNNING: {
    key: 'PROVISION_DATANODE_CERTIFICATES_RUNNING',
    description: "Provision the Data Node's certificate.",
  },
  EXISTING_DATA_MIGRATION_QUESTION_PAGE: {
    key: 'EXISTING_DATA_MIGRATION_QUESTION_PAGE',
    description: 'Migrate existing data question',
  },
  MIGRATE_EXISTING_DATA: {
    key: 'MIGRATE_EXISTING_DATA',
    description: 'Migrate existing data',
  },
  REMOTE_REINDEX_RUNNING: {
    key: 'REMOTE_REINDEX_RUNNING',
    description: 'Remote reindexing migration running',
  },
  DIRECTORY_COMPATIBILITY_CHECK_PAGE: {
    key: 'DIRECTORY_COMPATIBILITY_CHECK_PAGE',
    description: 'Directory compatibility check',
  },
  PROVISION_ROLLING_UPGRADE_NODES_WITH_CERTIFICATES: {
    key: 'PROVISION_ROLLING_UPGRADE_NODES_WITH_CERTIFICATES',
    description: 'Certificate provisioning overview',
  },
  PROVISION_ROLLING_UPGRADE_NODES_RUNNING: {
    key: 'PROVISION_ROLLING_UPGRADE_NODES_RUNNING',
    description: "Provision the Data Node's certificate.",
  },
  JOURNAL_SIZE_DOWNTIME_WARNING: {
    key: 'JOURNAL_SIZE_DOWNTIME_WARNING',
    description: 'Journal size downtime warning',
  },
  FAILED: {
    key: 'FAILED',
    description: 'Migration failed',
  },
  FINISHED: {
    key: 'FINISHED',
    description: 'Migration finished',
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
export const REMOTE_REINDEXING_MIGRATION_STEPS = [
  MIGRATION_STATE.REMOTE_REINDEX_WELCOME_PAGE.key,
  MIGRATION_STATE.PROVISION_DATANODE_CERTIFICATES_RUNNING.key,
  MIGRATION_STATE.EXISTING_DATA_MIGRATION_QUESTION_PAGE.key,
  MIGRATION_STATE.MIGRATE_EXISTING_DATA.key,
  MIGRATION_STATE.REMOTE_REINDEX_RUNNING.key,
  MIGRATION_STATE.ASK_TO_SHUTDOWN_OLD_CLUSTER.key,
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
    label: 'Configure certificate renewal policy',
  },
  SHOW_MIGRATION_SELECTION: {
    key: 'SHOW_MIGRATION_SELECTION',
    label: 'Go to migration steps',
  },
  RUN_DIRECTORY_COMPATIBILITY_CHECK: {
    key: 'INSTALL_DATANODES_ON_EVERY_NODE',
    label: 'Run directory compatibilty check',
  },
  PROVISION_DATANODE_CERTIFICATES: {
    key: 'PROVISION_DATANODE_CERTIFICATES',
    label: 'Provision Data Nodes with certificates',
  },
  SKIP_EXISTING_DATA_MIGRATION: {
    key: 'SKIP_EXISTING_DATA_MIGRATION',
    label: 'Skip existing data migration',
  },
  RETRY_MIGRATE_EXISTING_DATA: {
    key: 'RETRY_MIGRATE_EXISTING_DATA',
    label: 'Retry migrate existing data',
  },
  CHECK_REMOTE_INDEXER_CONNECTION: {
    key: 'CHECK_REMOTE_INDEXER_CONNECTION',
    label: 'Check connection',
  },
  START_REMOTE_REINDEX_MIGRATION: {
    key: 'START_REMOTE_REINDEX_MIGRATION',
    label: 'Start migration',
  },
};
export default MIGRATION_STEP;
