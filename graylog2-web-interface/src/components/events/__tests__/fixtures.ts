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
import type { EventDefinition } from 'components/event-definitions/event-definitions-types';

// eslint-disable-next-line import/prefer-default-export
export const eventDefinition: EventDefinition = {
  _scope: 'DEFAULT',
  id: '66d719128a7ffa68df52fd7f',
  title: 'Issue 20294',
  description: '',
  updated_at: '2024-11-26T10:55:08.671Z',
  matched_at: '2025-01-30T09:52:23.262Z',
  priority: 3,
  alert: false,
  config: {
    _is_scheduled: true,
    type: 'aggregation-v1',
    query: '',
    query_parameters: [],
    filters: [
      {
        type: 'inlineQueryString',
        title: 'Index Actions',
        id: 'd1057daf-f0e0-4159-ae00-e7e340629bb3',
        description: '',
        queryString: 'action:index',
        negation: false,
        disabled: false,
      },
    ],
    streams: [],
    stream_categories: [],
    group_by: [],
    series: [
      {
        type: 'count',
        id: 'count-',
        field: null,
      },
    ],
    conditions: {
      expression: {
        expr: '>',
        left: {
          expr: 'number-ref',
          ref: 'count-',
        },
        right: {
          expr: 'number',
          value: 0,
        },
      },
    },
    search_within_ms: 60000,
    execute_every_ms: 60000,
    use_cron_scheduling: false,
    cron_expression: null,
    cron_timezone: null,
    event_limit: 100,
  },
  field_spec: {},
  key_spec: ['mightyalert', 'secondkey'],
  notification_settings: {
    grace_period_ms: 300000,
    backlog_size: 0,
  },
  notifications: [],
  storage: [
    {
      type: 'persist-to-streams-v1',
      streams: ['000000000000000000000002'],
    },
  ],
  scheduler: null,
  state: 'ENABLED',
};
