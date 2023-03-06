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

import * as Immutable from 'immutable';

import type { EventType } from 'hooks/useEventById';
import type { EventDefinition } from 'logic/alerts/types';
import type { EventDefinitionAggregation } from 'hooks/useEventDefinition';
import QueryGenerator from 'views/logic/queries/QueryGenerator';
import Search from 'views/logic/search/Search';
import View from 'views/logic/views/View';
import ViewState from 'views/logic/views/ViewState';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import Series from 'views/logic/aggregationbuilder/Series';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import Direction from 'views/logic/aggregationbuilder/Direction';
import { allMessagesTable, resultHistogram } from 'views/logic/Widgets';

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

const eventData = mockEventData.event;
const query = QueryGenerator(eventData.replay_info.streams, undefined, {
  type: 'absolute',
  from: eventData?.replay_info?.timerange_start,
  to: eventData?.replay_info?.timerange_end,
}, {
  type: 'elasticsearch',
  query_string: eventData?.replay_info?.query || '',
});
const search = Search.create().toBuilder().queries([query]).build();

// const titles =
const histogram = resultHistogram('mc-widget-id');
const messageTable = allMessagesTable('allm-widget-id', []);

export const mockedView = View.create()
  .toBuilder()
  .newId()
  .type(View.Type.Search)
  .state({
    [query.id]: ViewState.create()
      .toBuilder()
      .titles({
        widget: {
          'mc-widget-id': 'Message Count',
          'allm-widget-id': 'All Messages',
          'field1-widget-id': 'count(field1) > 500',
          'field2-widget-id': 'count(field2) < 8000',
          'summary-widget-id': 'Summary:  count(field1) > 500 count(field2) < 8000',
        },
      })
      .widgets(Immutable.List([
        AggregationWidget.builder()
          .id('field1-widget-id')
          .config(
            AggregationWidgetConfig.builder()
              .columnPivots([])
              .rowPivots([Pivot.create(['field1', 'field2'], 'values', { limit: 15 })])
              .series([Series.forFunction('count(field1)')])
              .sort([new SortConfig(SortConfig.SERIES_TYPE, 'count(field1)', Direction.Descending)])
              .visualization('table')
              .rollup(true)
              .build(),
          )
          .build(),
        AggregationWidget.builder()
          .id('field1-widget-id')
          .config(
            AggregationWidgetConfig.builder()
              .columnPivots([])
              .rowPivots([Pivot.create(['field2', 'field1'], 'values', { limit: 15 })])
              .series([Series.forFunction('count(field2)')])
              .sort([new SortConfig(SortConfig.SERIES_TYPE, 'count(field2)', Direction.Ascending)])
              .visualization('table')
              .rollup(true)
              .build(),
          )
          .build(),
        histogram,
        messageTable,
        AggregationWidget.builder()
          .id('summary-widget-id')
          .config(
            AggregationWidgetConfig.builder()
              .columnPivots([])
              .rowPivots([Pivot.create(['field1', 'field2'], 'values', { limit: 15 })])
              .series([Series.forFunction('count(field1)'), Series.forFunction('count(field2)')])
              .sort([])
              .visualization('table')
              .rollup(true)
              .build(),
          )
          .build(),
      ]))
      .widgetPositions({
        'mc-widget-id': new WidgetPosition(1, 10, 2, Infinity),
        'allm-widget-id': new WidgetPosition(1, 12, 6, Infinity),
        'field1-widget-id': new WidgetPosition(1, 7, 3, Infinity),
        'field2-widget-id': new WidgetPosition(7, 7, 3, Infinity),
        'summary-widget-id': new WidgetPosition(1, 1, 3, Infinity),
      })
      .build(),
  })
  .search(search)
  .build();
