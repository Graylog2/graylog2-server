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

public enum MigrationState {
    NEW,
    MIGRATION_WELCOME_PAGE,
    CA_CREATION_PAGE,
    RENEWAL_POLICY_CREATION_PAGE,
    MIGRATION_SELECTION_PAGE,
    ROLLING_UPGRADE_MIGRATION_WELCOME_PAGE,
    REMOTE_REINDEX_WELCOME_PAGE,
    PROVISION_DATANODE_CERTIFICATES_PAGE,
    PROVISION_DATANODE_CERTIFICATES_RUNNING,
    EXISTING_DATA_MIGRATION_QUESTION_PAGE,
    MIGRATE_EXISTING_DATA,
    REMOTE_REINDEX_RUNNING,
    ASK_TO_SHUTDOWN_OLD_CLUSTER,
    DIRECTORY_COMPATIBILITY_CHECK_PAGE,
    PROVISION_ROLLING_UPGRADE_NODES_WITH_CERTIFICATES,
    PROVISION_ROLLING_UPGRADE_NODES_RUNNING,
    JOURNAL_SIZE_DOWNTIME_WARNING,
    MESSAGE_PROCESSING_STOP,
    RESTART_GRAYLOG,
    MESSAGE_PROCESSING_RESTART,
    FAILED,
    DIRECTORY_COMPATIBILITY_CHECK_PAGE2, FINISHED
}
