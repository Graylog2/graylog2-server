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

import type { ParameterJson } from 'views/logic/parameters/Parameter';

type Provider = {
  type: string,
  template: string,
  require_values: boolean,
}

type FieldSpec = {
  [key: string]: {
    data_type: string,
    providers: Array<Provider>,
  }
};

type Notification = {
  notification_id: string,
  notification_parameters: string
}

export type Scheduler = {
  data: {
    type: string
    timerange_from: number,
    timerange_to: number,
  },
  next_time: string,
  triggered_at: string,
  queued_notifications: number,
  is_scheduled: boolean,
  status: string
};

export type SearchFilter = {
  id: string,
  type: string,
  title: string,
  queryString: string,
  disabled: boolean,
  negation: boolean,
  frontendId?: string,
  description?: string,
};

export type EventDefinition = {
  _scope: string,
  id: string,
  title: string,
  description: string,
  priority: number,
  alert: boolean,
  state?: 'ENABLED' | 'DISABLED',
  config: {
    type: string,
    query: string,
    query_parameters: ParameterJson[],
    filters: SearchFilter[],
    streams: string[],
    group_by: string[],
    series: Array<{field: string, id: string, type: string}>,
    conditions: {
      expression: string | null | {},
    },
    search_within_ms: number,
    execute_every_ms: number,
  },
  field_spec: FieldSpec,
  key_spec: string[],
  notification_settings: {
    grace_period_ms: number,
    backlog_size: number,
  },
  notifications: Array<Notification>,
  remediation_steps?: string,
  storage: Array<{
    type: string,
    streams: number[] | string[],
  }>,
  updated_at: string | null,
  matched_at?: string,
  scheduler?: Scheduler,
}
// export type EventDefinition = {
//   id: string,
//   config?: {
//     type: string,
//     execute_every_ms?: number,
//     search_within_ms?: number,
//     event_limit?: number,
//     sigma_rule_id?: string,
//     streams?: Array<string>
//     filters?: Array<SearchFilter>,
//   },
//   title: string,
//   description?: string,
//   matched_at?: string,
//   priority?: number,
//   key_spec?: Array<string>
//   field_spec?: FieldSpec,
//   notification_settings?: {
//     backlog_size: number,
//     grace_period_ms: number,
//   }
//   notifications?: Array<Notification>,
//   _scope?: string,
//   scheduler?: Scheduler,
//   state?: 'ENABLED' | 'DISABLED',
//   remediation_steps?: string,
// };
