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

import React, { useEffect, useState } from 'react';

import useParams from 'routing/useParams';
import type { EventType } from 'hooks/useEventById';
import useEventById from 'hooks/useEventById';
import type { EventDefinitionAggregation } from 'hooks/useEventDefinition';
import useEventDefinition from 'hooks/useEventDefinition';
import { Spinner } from 'components/common';
import SearchPage from 'views/pages/SearchPage';
import { EventNotificationsActions } from 'stores/event-notifications/EventNotificationsStore';
import useCreateViewForEvent from 'views/logic/views/UseCreateViewForEvent';
import useAlertAndEventDefinitionData from 'hooks/useAlertAndEventDefinitionData';
import type { EventDefinition } from 'logic/alerts/types';

export const mockEventData = {
  event: {
    alert: true,
    id: 'event-id-1',
    event_definition_id: 'event-definition-id-1',
    event_definition_type: 'aggregation-v1',
    origin_context: null,
    timestamp: '2023-03-02T13:43:21.266Z',
    timestamp_processing: '2023-03-02T13:43:21.906Z',
    timerange_start: '2023-03-02T13:42:21.266Z',
    timerange_end: '2023-03-02T13:43:21.266Z',
    streams: [
      '002',
    ],
    source_streams: [
      '001',
    ],
    message: 'message',
    source: '',
    key_tuple: [],
    key: null,
    priority: 2,
    fields: {},
    replay_info: {
      timerange_start: '2023-03-02T13:42:21.266Z',
      timerange_end: '2023-03-02T13:43:21.266Z',
      query: 'http_method: GET',
      streams: [
        '001',
      ],
    },

  } as EventType,
};

export const mockEventDefinition:EventDefinition = {
  _scope: 'DEFAULT',
  id: 'event-definition-id-1',
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
export const mockedMappedAggregation: Array<EventDefinitionAggregation> = [
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

const EventView = () => {
  const { eventData, eventDefinition, aggregations } = useAlertAndEventDefinitionData();
  const view = useCreateViewForEvent({ eventData, eventDefinition, aggregations });

  return <SearchPage view={view} isNew />;
};

const EventReplaySearchPage = () => {
  const [isNotificationLoaded, setIsNotificationLoaded] = useState(false);
  const { alertId } = useParams<{ alertId?: string }>();
  const { data: eventData, isLoading: eventIsLoading, isFetched: eventIsFetched } = useEventById(alertId);
  const { isLoading: EDIsLoading, isFetched: EDIsFetched } = useEventDefinition(eventData?.event_definition_id);

  useEffect(() => {
    EventNotificationsActions.listAll().then(() => setIsNotificationLoaded(true));
  }, [setIsNotificationLoaded]);

  const isLoading = eventIsLoading || EDIsLoading || !eventIsFetched || !EDIsFetched || !isNotificationLoaded;

  return isLoading ? <Spinner /> : <EventView />;
};

export default EventReplaySearchPage;
