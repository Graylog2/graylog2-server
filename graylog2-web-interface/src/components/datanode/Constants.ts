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
    description: 'Welcome',
  },
  ROLLING_UPGRADE_MIGRATION_WELCOME: {
    key: 'ROLLING_UPGRADE_MIGRATION_WELCOME',
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
    description: 'Remove old connection string from configuration file',
  },
  MIGRATE: {
    key: 'MIGRATE',
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
  PROVISION_ROLLING_UPGRADE_NODES_WITH_CERTIFICATES: {
    key: 'PROVISION_ROLLING_UPGRADE_NODES_WITH_CERTIFICATES',
    description: 'Provision upgrade nodes with certificates',
  },
  JOURNAL_SIZE_DOWNTIME_WARNING: {
    key: 'JOURNAL_SIZE_DOWNTIME_WARNING',
    description: 'Journal size Downtime warning',
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
  MIGRATION_STATE.ROLLING_UPGRADE_MIGRATION_WELCOME.key,
  MIGRATION_STATE.DIRECTORY_COMPATIBILITY_CHECK_PAGE.key,
  MIGRATION_STATE.PROVISION_ROLLING_UPGRADE_NODES_WITH_CERTIFICATES.key,
  MIGRATION_STATE.JOURNAL_SIZE_DOWNTIME_WARNING.key,
  'MIGRATE',
  MIGRATION_STATE.MANUALLY_REMOVE_OLD_CONNECTION_STRING_FROM_CONFIG.key,
  MIGRATION_STATE.FINISHED.key,
];
export const REMOTE_REINDEXING_MIGRATION_STEPS = [
  MIGRATION_STATE.REMOTE_REINDEX_WELCOME.key,
  MIGRATION_STATE.PROVISION_DATANODE_CERTIFICATES.key,
  MIGRATION_STATE.EXISTING_DATA_MIGRATION_QUESTION.key,
  MIGRATION_STATE.MIGRATE_WITH_DOWNTIME_QUESTION.key,
  MIGRATION_STATE.ASK_TO_SHUTDOWN_OLD_CLUSTER.key,
  MIGRATION_STATE.MANUALLY_REMOVE_OLD_CONNECTION_STRING_FROM_CONFIG.key,
  MIGRATION_STATE.FINISHED.key,
];

export default MIGRATION_STEP;
