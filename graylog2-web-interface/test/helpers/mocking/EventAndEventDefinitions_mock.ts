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
import Immutable from 'immutable';

import type { Event } from 'components/events/events/types';
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
import FormattingSettings from 'views/logic/views/formatting/FormattingSettings';
import { StaticColor } from 'views/logic/views/formatting/highlighting/HighlightingColor';
import HighlightingRule from 'views/logic/views/formatting/highlighting/HighlightingRule';

const mock_color = StaticColor.create('#ffffff');
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
    priority: '2',
    fields: [{}],
    replay_info: {
      timerange_start: '2023-03-02T13:42:21.266Z',
      timerange_end: '2023-03-02T13:43:21.266Z',
      query: 'http_method: GET',
      streams: [
        '001',
      ],
    },
    group_by_fields: { field4: 'value4' },

  } as Event,
};

export const mockEventDefinitionTwoAggregations:EventDefinition = {
  _scope: 'DEFAULT',
  id: 'event-definition-id-1',
  title: 'Event Definition Title',
  description: 'Test description',
  updated_at: '2023-02-21T13:28:09.296Z',
  priority: 2,
  alert: true,
  config: {
    type: 'aggregation-v1',
    query: 'http_method: GET',
    query_parameters: [],
    streams: [
      '001',
    ],
    group_by: [
      'field1',
      'field2',
    ],
    series: [
      {
        id: 'count-field1',
        type: 'count',
        field: 'field1',
      },
      {
        id: 'count-field2',
        type: 'count',
        field: 'field2',
      },
    ],
    filters: [],
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
      notification_id: 'email_notification_id',
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

export const mockEventDefinitionOneAggregation = {
  ...mockEventDefinitionTwoAggregations,
  id: 'event-definition-id-1',
  config: {
    ...mockEventDefinitionTwoAggregations.config,
    series: [
      {
        id: 'count-field1',
        type: 'count',
        field: 'field1',
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
      },
    },
  },
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

export const mockedMappedAggregationNoField: Array<EventDefinitionAggregation> = [
  {
    expr: '>',
    value: 500,
    function: 'count',
    fnSeries: 'count()',
  },
];
const eventData = mockEventData.event;
const query = QueryGenerator(eventData.replay_info.streams, 'query-id', {
  type: 'absolute',
  from: eventData?.replay_info?.timerange_start,
  to: eventData?.replay_info?.timerange_end,
}, {
  type: 'elasticsearch',
  query_string: '(http_method: GET) AND (field4:value4)',
});

const histogram = resultHistogram('mc-widget-id');
const messageTable = allMessagesTable('allm-widget-id', []);

const field1Widget = AggregationWidget.builder()
  .id('field1-widget-id')
  .type('pivot')
  .config(
    AggregationWidgetConfig.builder()
      .columnPivots([])
      .rowPivots([Pivot.createValues(['field1', 'field2'])])
      .series([Series.forFunction('count(field1)')])
      .sort([new SortConfig(SortConfig.SERIES_TYPE, 'count(field1)', Direction.Descending)])
      .visualization('table')
      .rollup(true)
      .build(),
  )
  .build();

const noFieldWidget = AggregationWidget.builder()
  .id('field1-widget-id')
  .type('pivot')
  .config(
    AggregationWidgetConfig.builder()
      .columnPivots([])
      .rowPivots([])
      .series([Series.forFunction('count()')])
      .sort([new SortConfig(SortConfig.SERIES_TYPE, 'count()', Direction.Descending)])
      .visualization('table')
      .rollup(true)
      .build(),
  )
  .build();

const field2Widget = AggregationWidget.builder()
  .id('field2-widget-id')
  .type('pivot')
  .config(
    AggregationWidgetConfig.builder()
      .columnPivots([])
      .rowPivots([Pivot.createValues(['field2', 'field1'])])
      .series([Series.forFunction('count(field2)')])
      .sort([new SortConfig(SortConfig.SERIES_TYPE, 'count(field2)', Direction.Ascending)])
      .visualization('table')
      .rollup(true)
      .build(),
  )
  .build();

const summaryWidget = AggregationWidget.builder()
  .id('summary-widget-id')
  .type('pivot')
  .config(
    AggregationWidgetConfig.builder()
      .columnPivots([])
      .rowPivots([Pivot.createValues(['field1', 'field2'])])
      .series([Series.forFunction('count(field1)'), Series.forFunction('count(field2)')])
      .sort([])
      .visualization('table')
      .rollup(true)
      .build(),
  )
  .build();
const widgetsWithOneAggregation = [
  field1Widget,
  histogram,
  messageTable,
];

const widgetsWithOneAggregationNoField = [
  noFieldWidget,
  histogram,
  messageTable,
];
const widgetsWithTwoAggregations = [
  field1Widget,
  field2Widget,
  histogram,
  messageTable,
  summaryWidget,
];
const searchTwoAggregations = Search.create().toBuilder().id('search-id').queries([
  query
    .toBuilder()
    .searchTypes(Array(5).fill({
      filters: [],
      type: 'AGGREGATION',
      typeDefinition: {},
    })).build()])
  .build();
export const mockedViewWithTwoAggregations = View.create()
  .toBuilder()
  .id('view-id')
  .type(View.Type.Search)
  .state({
    'query-id': ViewState.create()
      .toBuilder()
      .titles({
        widget: {
          'field1-widget-id': 'count(field1) > 500',
          'field2-widget-id': 'count(field2) < 8000',
          'mc-widget-id': 'Message Count',
          'allm-widget-id': 'All Messages',
          'summary-widget-id': 'Summary:  count(field1) > 500 count(field2) < 8000',
        },
      })
      .widgetMapping(Immutable.Map(
        ['field1-widget-id', 'field2-widget-id', 'mc-widget-id', 'allm-widget-id', 'summary-widget-id'].map((item) => [item, Immutable.Set([undefined])]),
      ))
      .widgets(widgetsWithTwoAggregations)
      .widgetPositions({
        'field1-widget-id': new WidgetPosition(1, 4, 3, 6),
        'field2-widget-id': new WidgetPosition(7, 4, 3, 6),
        'mc-widget-id': new WidgetPosition(1, 10, 2, Infinity),
        'allm-widget-id': new WidgetPosition(1, 12, 6, Infinity),
        'summary-widget-id': new WidgetPosition(1, 1, 3, Infinity),
      })
      .formatting(FormattingSettings.create([
        HighlightingRule.create('count(field1)', 500, 'greater', mock_color),
        HighlightingRule.create('count(field2)', 8000, 'less', mock_color),
      ]))
      .build(),
  })
  .search(searchTwoAggregations)
  .build();

const searchOneAggregation = Search.create().toBuilder().id('search-id').queries([query.toBuilder().searchTypes(Array(3).fill({
  filters: [],
  type: 'AGGREGATION',
  typeDefinition: {},
})).build()])
  .build();
export const mockedViewWithOneAggregation = View.create()
  .toBuilder()
  .id('view-id')
  .type(View.Type.Search)
  .state({
    'query-id': ViewState.create()
      .toBuilder()
      .titles({
        widget: {
          'field1-widget-id': 'count(field1) > 500',
          'mc-widget-id': 'Message Count',
          'allm-widget-id': 'All Messages',
        },
      })
      .widgets(widgetsWithOneAggregation)
      .widgetMapping(Immutable.Map(
        ['field1-widget-id', 'mc-widget-id', 'allm-widget-id'].map((item) => [item, Immutable.Set([undefined])]),
      ))
      .widgetPositions({
        'field1-widget-id': new WidgetPosition(1, 1, 3, 6),
        'mc-widget-id': new WidgetPosition(1, 4, 2, Infinity),
        'allm-widget-id': new WidgetPosition(1, 6, 6, Infinity),
      })
      .formatting(FormattingSettings.create([
        HighlightingRule.create('count(field1)', 500, 'greater', mock_color),
      ]))
      .build(),
  })
  .search(searchOneAggregation)
  .build();

export const mockedViewWithOneAggregationNoField = View.create()
  .toBuilder()
  .id('view-id')
  .type(View.Type.Search)
  .state({
    'query-id': ViewState.create()
      .toBuilder()
      .titles({
        widget: {
          'field1-widget-id': 'count() > 500',
          'mc-widget-id': 'Message Count',
          'allm-widget-id': 'All Messages',
        },
      })
      .widgets(widgetsWithOneAggregationNoField)
      .widgetMapping(Immutable.Map(
        ['field1-widget-id', 'mc-widget-id', 'allm-widget-id'].map((item) => [item, Immutable.Set([undefined])]),
      ))
      .widgetPositions({
        'field1-widget-id': new WidgetPosition(1, 1, 3, 6),
        'mc-widget-id': new WidgetPosition(1, 4, 2, Infinity),
        'allm-widget-id': new WidgetPosition(1, 6, 6, Infinity),
      })
      .formatting(FormattingSettings.create([
        HighlightingRule.create('count()', 500, 'greater', mock_color),
      ]))
      .build(),
  })
  .search(searchOneAggregation)
  .build();

const queryED = QueryGenerator(eventData.replay_info.streams, 'query-id', {
  type: 'relative',
  range: 60,
}, {
  type: 'elasticsearch',
  query_string: mockEventDefinitionTwoAggregations?.config?.query || '',
});
export const mockedViewWithTwoAggregationsED = mockedViewWithTwoAggregations
  .toBuilder()
  .search(
    searchTwoAggregations.toBuilder().queries([queryED.toBuilder().searchTypes(
      Array(5).fill({
        filters: [],
        type: 'AGGREGATION',
        typeDefinition: {},
      }),
    ).build()]).build(),
  )
  .build();

export const mockedViewWithOneAggregationED = mockedViewWithOneAggregation
  .toBuilder()
  .search(
    searchOneAggregation.toBuilder().queries([queryED.toBuilder().searchTypes(
      Array(3).fill({
        filters: [],
        type: 'AGGREGATION',
        typeDefinition: {},
      }),
    ).build()]).build(),
  )
  .build();

export const mockEventDefinitionOneAggregationNoFields = {
  ...mockEventDefinitionTwoAggregations,
  id: 'event-definition-id-1',
  config: {
    ...mockEventDefinitionTwoAggregations.config,
    group_by: [],
    series: [],
    conditions: {
      expression: {
        expr: '||',
        left: {
          expr: '>',
          left: {
            expr: 'number-ref',
          },
          right: {
            expr: 'number',
            value: 500.0,
          },
        },
      },
    },
  },
};
