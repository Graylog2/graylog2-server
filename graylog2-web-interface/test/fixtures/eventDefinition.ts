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

// eslint-disable-next-line import/prefer-default-export
export const simpleEventDefinition = {
  alert: false,
  config: {
    conditions: { expression: null },
    execute_every_ms: 60000,
    group_by: [],
    query: '',
    query_parameters: [],
    search_within_ms: 60000,
    series: [],
    streams: ['stream-id-1'],
    type: 'aggregation-v1',
  },
  description: '',
  field_spec: {},
  id: 'event-definition-1-id',
  key_spec: [],
  notification_settings: {
    grace_period_ms: 0,
    backlog_size: 0,
  },
  notifications: [],
  priority: 2,
  storage: [{
    streams: ['stream-id-2'],
    type: 'persist-to-streams-v1',
  }],
  title: 'Event Definition 1',
};
