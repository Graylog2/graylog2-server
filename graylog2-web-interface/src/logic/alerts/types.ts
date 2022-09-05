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

export type EventDefinition = {
  _scope: string,
  id: string,
  title: string,
  description: string,
  priority: number,
  alert: boolean,
  config: {
    type: string,
    query: string,
    query_parameters: string[],
    streams: string[],
    group_by: string[],
    series: string[],
    conditions: {
      expression: string | null,
    },
    search_within_ms: number,
    execute_every_ms: number,
  },
  field_spec: {},
  key_spec: string[],
  notification_settings: {
    grace_period_ms: number,
    backlog_size: number,
  },
  notifications: string[],
  storage: [
    {
      type: string,
      streams: number[],
    }
  ]
}
