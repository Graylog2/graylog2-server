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
package org.graylog.plugins.views.storage.migration.state.machine;

public enum MigrationStep {
    SELECT_MIGRATION,
    SHOW_CA_CREATION,
    SHOW_RENEWAL_POLICY_CREATION,
    SHOW_MIGRATION_SELECTION,
    PROVISION_DATANODE_CERTIFICATES,
    SHOW_DATA_MIGRATION_QUESTION,
    SHOW_MIGRATE_EXISTING_DATA,
    CHECK_REMOTE_INDEXER_CONNECTION,
    START_REMOTE_REINDEX_MIGRATION,
    RETRY_MIGRATE_EXISTING_DATA,
    REQUEST_MIGRATION_STATUS,
    SHOW_ASK_TO_SHUTDOWN_OLD_CLUSTER,
    SHOW_PROVISION_ROLLING_UPGRADE_NODES_WITH_CERTIFICATES,
    SHOW_STOP_PROCESSING_PAGE,
    SELECT_ROLLING_UPGRADE_MIGRATION,
    SELECT_REMOTE_REINDEX_MIGRATION,
    DISCOVER_NEW_DATANODES,
    SKIP_EXISTING_DATA_MIGRATION,
    RUN_DIRECTORY_COMPATIBILITY_CHECK,
    CALCULATE_JOURNAL_SIZE,
    CONFIRM_OLD_CLUSTER_STOPPED,
    CONFIRM_OLD_CONNECTION_STRING_FROM_CONFIG_REMOVED_AND_GRAYLOG_RESTARTED,
    CONFIRM_OLD_CONNECTION_STRING_FROM_CONFIG_REMOVED,
    SHOW_ROLLING_UPGRADE_ASK_TO_SHUTDOWN_OLD_CLUSTER
}
