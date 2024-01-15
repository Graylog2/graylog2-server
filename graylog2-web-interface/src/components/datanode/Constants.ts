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
  REPLACE_EXISTING_OS_ES_CLUSTER: {
    key: 'REPLACE_EXISTING_OS_ES_CLUSTER',
    description: 'Replace existing OS/ES cluster',
  },
  CREATE_NEW_DATANODE_CLUSTER: {
    key: 'CREATE_NEW_DATANODE_CLUSTER',
    description: 'Create new datanode cluster',
  },
  MIGRATION_FINISHED: {
    key: 'MIGRATION_FINISHED',
    description: 'Migration finished',
  },
} as const;

export default MIGRATION_STEP;
