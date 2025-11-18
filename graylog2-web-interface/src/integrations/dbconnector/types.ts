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
export type DBConnectorGenericInputCreateRequest = {
  type: 'org.graylog.integrations.dbconnector.DBConnectorInput',
  title: string,
  global: boolean,
  configuration: {
    db_type: string,
    polling_interval: number,
    polling_time_unit: 'MILLISECONDS' | 'MICROSECONDS' | 'HOURS' | 'SECONDS' | 'NANOSECONDS' | 'DAYS' | 'MINUTES',
    hostname: string,
    port: number,
    database_name: string,
    username: string,
    password: string,
    state_field_type: string,
    timezone: string,
    state_field: string,
    table_name: String,
    throttling_allowed: boolean,
    mongo_collection_name: string,
    override_source: string,
  },
};

export type DBConnectorInputCreateRequest = {
  name: string,
  db_type: string,
  polling_interval: number,
  polling_time_unit: 'MILLISECONDS' | 'MICROSECONDS' | 'HOURS' | 'SECONDS' | 'NANOSECONDS' | 'DAYS' | 'MINUTES',
  hostname: string,
  port: number,
  database_name: string,
  username: string,
  password: string,
  state_field_type: string,
  timezone: string,
  state_field: string,
  table_name: string,
  enable_throttling: boolean,
  mongo_collection_name: string,
  override_source: string,

};
