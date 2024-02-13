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
    description: 'Check opensearch compatibility with datanode',
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
    description: 'Certificate autority',
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
    description: 'Welcome to Rolling upgrade migration',
  },
  PROVISION_DATANODE_CERTIFICATES: {
    key: 'PROVISION_DATANODE_CERTIFICATES',
    description: 'Provision data node with certificates',
  },
  EXISTING_DATA_MIGRATION_QUESTION: {
    key: 'EXISTING_DATA_MIGRATION_QUESTION',
    description: 'Migrate existing data',
  },
  MIGRATE_WITH_DOWNTIME_QUESTION: {
    key: 'MIGRATE_WITH_DOWNTIME_QUESTION',
    description: 'Migrate with downtime',
  },
  ASK_TO_SHUTDOWN_OLD_CLUSTER: {
    key: 'ASK_TO_SHUTDOWN_OLD_CLUSTER',
    description: 'Shut down old cluster',
  },
  MANUALLY_REMOVE_OLD_CONNECTION_STRING_FROM_CONFIG: {
    key: 'MANUALLY_REMOVE_OLD_CONNECTION_STRING_FROM_CONFIG',
    description: 'Remove connection string',
  },
  MESSAGE_PROCESSING_STOP_REPLACE_CLUSTER_AND_MP_RESTART: {
    key: 'MESSAGE_PROCESSING_STOP_REPLACE_CLUSTER_AND_MP_RESTART',
    description: 'Message processing and datanode actions',
  },
  REMOTE_REINDEX_WELCOME: {
    key: 'REMOTE_REINDEX_WELCOME',
    description: 'Remote reindexing migration',
  },
  DIRECTORY_COMPATIBILITY_CHECK_PAGE: {
    key: 'DIRECTORY_COMPATIBILITY_CHECK_PAGE',
    description: 'Directory compatibility check',
  },
  DIRECTORY_COMPATIBILITY_CHECK_PAGE2: {
    key: 'DIRECTORY_COMPATIBILITY_CHECK_PAGE2',
    description: 'Directory compatibility check',
  },
  PROVISION_ROLLING_UPGRADE_NODES_WITH_CERTIFICATES: {
    key: 'PROVISION_ROLLING_UPGRADE_NODES_WITH_CERTIFICATES',
    description: 'Provision upgrade nodes with certificates',
  },
  PROVISION_ROLLING_UPGRADE_NODES_RUNNING: {
    key: 'PROVISION_ROLLING_UPGRADE_NODES_RUNNING',
    description: 'Provisionning data nodes with certificate.',
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

export const ROLLING_UPGRADE_MIGRATION_STEPS = [
  MIGRATION_STATE.ROLLING_UPGRADE_MIGRATION_WELCOME_PAGE.key,
  MIGRATION_STATE.DIRECTORY_COMPATIBILITY_CHECK_PAGE2.key,
  MIGRATION_STATE.PROVISION_ROLLING_UPGRADE_NODES_WITH_CERTIFICATES.key,
  MIGRATION_STATE.PROVISION_ROLLING_UPGRADE_NODES_RUNNING.key,
  MIGRATION_STATE.JOURNAL_SIZE_DOWNTIME_WARNING.key,
  MIGRATION_STATE.MESSAGE_PROCESSING_STOP_REPLACE_CLUSTER_AND_MP_RESTART.key,
];
export const REMOTE_REINDEXING_MIGRATION_STEPS = [
  MIGRATION_STATE.REMOTE_REINDEX_WELCOME.key,
  MIGRATION_STATE.PROVISION_DATANODE_CERTIFICATES.key,
  MIGRATION_STATE.EXISTING_DATA_MIGRATION_QUESTION.key,
  MIGRATION_STATE.MIGRATE_WITH_DOWNTIME_QUESTION.key,
];

export const MIGRATION_WIZARD_STEPS = [
  MIGRATION_STATE.NEW.key,
  MIGRATION_STATE.MIGRATION_WELCOME_PAGE.key,
  MIGRATION_STATE.DIRECTORY_COMPATIBILITY_CHECK_PAGE.key,
  MIGRATION_STATE.CA_CREATION_PAGE.key,
  MIGRATION_STATE.RENEWAL_POLICY_CREATION_PAGE.key,
  MIGRATION_STATE.MIGRATION_SELECTION_PAGE.key,
  MIGRATION_STATE.ASK_TO_SHUTDOWN_OLD_CLUSTER.key,
  MIGRATION_STATE.MANUALLY_REMOVE_OLD_CONNECTION_STRING_FROM_CONFIG.key,
  MIGRATION_STATE.FINISHED.key,
];

export const MIGRATION_ACTIONS = {
  SHOW_DIRECTORY_COMPATIBILITY_CHECK: {
    key: 'SHOW_DIRECTORY_COMPATIBILITY_CHECK',
    label: 'Directory compatibility check',
  },
  SKIP_DIRECTORY_COMPATIBILITY_CHECK: {
    key: 'SKIP_DIRECTORY_COMPATIBILITY_CHECK',
    label: 'Skip directory compatibility check',
  },
  SHOW_RENEWAL_POLICY_CREATION: {
    key: 'SHOW_RENEWAL_POLICY_CREATION',
    label: 'Configure certificat renewal policy',
  },
  SHOW_MIGRATION_SELECTION: {
    key: 'SHOW_MIGRATION_SELECTION',
    label: 'Go to migration steps',
  },
};
export default MIGRATION_STEP;
