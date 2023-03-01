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

import type { EventType } from 'hooks/useEventById';

export const mockEventData = { event: { id: 'event-id-1', event_definition_id: 'event-definition-id-1' } as EventType };

export const mockEventDefinition = {
  _scope: 'DEFAULT',
  id: '111',
  title: 'Test',
  description: 'Test description',
  updated_at: '2023-02-21T13:28:09.296Z',
  priority: 2,
  alert: true,
  config: {
    type: 'aggregation-v1',
    query: 'http_method: GET',
    query_parameters: [],
    streams: [
      '0001',
    ],
    group_by: [
      'field1',
      'field2',
    ],
    series: [
      {
        id: 'count-field1',
        function: 'count',
        field: 'field1',
      },
      {
        id: 'count-field2',
        function: 'count',
        field: 'field2',
      },
    ],
    conditions: {
      expression: {
        expr: '||',
        left: {
          expr: '>',
          left: {
            expr: 'number-ref',
            ref: 'count-field1',
          },
          right: {
            expr: 'number',
            value: 500.0,
          },
        },
        right: {
          expr: '<',
          left: {
            expr: 'number-ref',
            ref: 'count-field2',
          },
          right: {
            expr: 'number',
            value: 8000.0,
          },
        },
      },
    },
    search_within_ms: 60000,
    execute_every_ms: 60000,
  },
  field_spec: {},
  key_spec: [],
  notification_settings: {
    grace_period_ms: 60000,
    backlog_size: 0,
  },
  notifications: [
    {
      notification_id: '2222',
      notification_parameters: null,
    },
  ],
  storage: [
    {
      type: 'persist-to-streams-v1',
      streams: [
        '0002',
      ],
    },
  ],
};
export const mockedMappedAggregation = [
  {
    expr: '>',
    value: 500,
    function: 'count',
    fnSeries: 'count(field1)',
    field: 'field1',
  },
  {
    expr: '<',
    value: 8000,
    function: 'count',
    fnSeries: 'count(field2)',
    field: 'field2',
  },
];
