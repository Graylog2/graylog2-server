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
import type { DBConnectorGenericInputCreateRequest, DBConnectorInputCreateRequest } from './types';

import type { FormDataType } from '../common/utils/types';

export const toDBConnectorInputCreateRequest = ({
  hostname,
  port,
  databaseName,
  username,
  password,
  dbConnectorName,
  pollingInterval,
  dbType,
  stateFieldType,
  timezone,
  stateField,
  tableName,
  enableThrottling,
  mongoCollectionName,
  overrideSource,
}: FormDataType): DBConnectorInputCreateRequest => ({
  name: dbConnectorName.value,
  hostname:hostname.value,
  port:port.value,
  database_name:databaseName.value,
  username:username.value,
  password:password.value,
  db_type:dbType.value,
  polling_interval: pollingInterval.value,
  polling_time_unit: 'MINUTES',
  state_field_type: stateFieldType.value,
  timezone: timezone.value,
  state_field: stateField.value,
  table_name: tableName.value,
  enable_throttling: !!enableThrottling.value,
  mongo_collection_name: mongoCollectionName.value,
  override_source: overrideSource?.value,
});

export const toGenericInputCreateRequest = ({
  hostname,
  port,
  databaseName,
  username,
  password,
  dbConnectorName,
  dbType,
  pollingInterval,
  mongoCollectionName,
  stateFieldType,
  timezone,
  stateField,
  tableName,
  enableThrottling,
  overrideSource,
}: FormDataType): DBConnectorGenericInputCreateRequest => ({
  type: 'org.graylog.integrations.dbconnector.DBConnectorInput',
  title: dbConnectorName.value,
  global: false,
  configuration: {
    hostname:hostname.value,
    port:port.value,
    database_name:databaseName.value,
    username:username.value,
    password:password.value,
    mongo_collection_name: mongoCollectionName.value,
    db_type:dbType.value,
    polling_interval: pollingInterval.value,
    polling_time_unit: 'MINUTES',
    state_field_type: stateFieldType.value,
    timezone: timezone.value,
    state_field: stateField.value,
    table_name: tableName.value,
    throttling_allowed: !!enableThrottling?.value,
    override_source: overrideSource?.value,

  },
});