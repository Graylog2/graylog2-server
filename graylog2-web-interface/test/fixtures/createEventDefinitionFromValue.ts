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

import Pivot from 'views/logic/aggregationbuilder/Pivot';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Series from 'views/logic/aggregationbuilder/Series';
import { allMessagesTable } from 'views/logic/Widgets';
import ValueParameter from 'views/logic/parameters/ValueParameter';
import LookupTableParameter from 'views/logic/parameters/LookupTableParameter';
import type { FiltersType, SearchFilter } from 'views/types';
import View from 'views/logic/views/View';
import ViewState from 'views/logic/views/ViewState';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import Search from 'views/logic/search/Search';
import QueryGenerator from 'views/logic/queries/QueryGenerator';
import ParameterBinding from 'views/logic/parameters/ParameterBinding';
import type { ParameterBindings } from 'views/logic/search/SearchExecutionState';
import type { MappedData } from 'views/logic/valueactions/createEventDefinition/types';

export const valuePath = [
  { http_method: 'GET' },
  { action: 'index' },
];

export const pivots = [
  Pivot.createValues(['http_method', 'index']),
  Pivot.createValues(['resources', 'index']),
];

export const testAggregationWidget = AggregationWidget.builder()
  .id('summary-widget-id')
  .type('pivot')
  .config(
    AggregationWidgetConfig.builder()
      .columnPivots([Pivot.createValues(['field1', 'field2'])])
      .rowPivots([Pivot.createValues(['field3', 'field4'])])
      .series([Series.forFunction('count(field5)'), Series.forFunction('count(field6)')])
      .sort([])
      .visualization('table')
      .rollup(true)
      .build(),
  )
  .build();

export const testWidgetMetricValuePath = [
  { field3: 'value3' },
  { field4: 'value4' },
  { field1: 'value1' },
  { _exist_: 'field5' },
];

export const testWidgetValueValuePath = [
  { field3: 'value3' },
  { field4: 'value4' },
];

export const messageTable = allMessagesTable('allm-widget-id', []);

export const ltParamJSON = {
  binding: undefined,
  data_type: 'any',
  default_value: 'GET',
  description: '',
  key: 'lt',
  lookup_table: 'http_method',
  name: 'newParameter',
  optional: false,
  title: 'lt',
  type: 'lut-parameter-v1',
};

export const valueParameterJSON = {
  binding: { type: 'value', value: '' },
  data_type: 'any',
  default_value: 'GET',
  description: '',
  name: 'newParameter3',
  optional: false,
  title: 'New Parameter3',
  type: 'value-parameter-v1',
};

export const valueParameter = ValueParameter.fromJSON(valueParameterJSON);
export const ltParam = LookupTableParameter.fromJSON(ltParamJSON);
export const parameters = Immutable.Set([ltParam, valueParameter]);
export const parameterBindings: ParameterBindings = Immutable.Map([['newParameter3', ParameterBinding.create('newParameter3', 'GET')]]);
export const firstSimpleSearchFilter: SearchFilter = {
  type: 'inlineQueryString',
  queryString: 'http_method: GET or http_method: POST',
};

export const secondSimpleSearchFilter: SearchFilter = {
  type: 'inlineQueryString',
  queryString: 'action: show',
};

export const disabledSearchFilter: SearchFilter = {
  type: 'inlineQueryString',
  queryString: 'action: show',
  disabled: true,
};

export const negationSearchFilter: SearchFilter = {
  type: 'inlineQueryString',
  queryString: 'action: login',
  negation: true,
};
const query = QueryGenerator([], [], 'query-id', {
  type: 'relative',
  from: 300,

}, {
  type: 'elasticsearch',
  query_string: 'http_method:GET',
});
export const mockedFilter: Immutable.Map<string, any> = Immutable.Map([
  ['filters', [
    Immutable.Map([['type', 'stream'], ['id', 'streamId']]),
  ]],
]);

export const mockedSearchFilters: FiltersType = Immutable.List([{
  type: 'inlineQueryString',
  queryString: 'http_method:GET',
}]);
const searchOneAggregation = Search.create().toBuilder().id('search-id').queries([
  query
    .toBuilder()
    .filter(mockedFilter)
    .filters(mockedSearchFilters)
    .searchTypes(Array(3)
      .fill({
        filters: [],
        type: 'AGGREGATION',
        typeDefinition: {},
      }))
    .build()])
  .build();
export const mockedView = View.create()
  .toBuilder()
  .id('view-id')
  .type(View.Type.Search)
  .state({
    'query-id': ViewState.create()
      .toBuilder()
      .titles({
        widget: {
          'widget-id': 'widget-id title',
        },
      })
      .widgets([testAggregationWidget])
      .widgetMapping(Immutable.Map(
        ['widget-id'].map((item) => [item, Immutable.Set([undefined])]),
      ))
      .widgetPositions({
        'widget-id': new WidgetPosition(1, 1, 3, 6),
      })
      .build(),
  })
  .search(searchOneAggregation)
  .build();

export const mockedContexts = {
  widget: testAggregationWidget,
  view: mockedView,
  parameterBindings: parameterBindings,
  valuePath: valuePath,
  parameters: parameters,
  analysisDisabledFields: undefined,
  currentUser: undefined,
  message: undefined,
  isLocalNode: undefined,
};

export const mappedDataResult: MappedData = {
  searchWithinMs: 300000,
  lutParameters: [ltParamJSON],
  searchFilterQuery: '(http_method:GET)',
  queryWithReplacedParams: 'http_method:GET',
  streams: ['streamId'],
  searchFromValue: 'action:show',
};

export const modalDataResult = {
  aggCondition: 'count(action): 400',
  columnGroupBy: 'action, http_method',
  lutParameters: 'newParameter',
  queryWithReplacedParams: 'http_method:GET',
  rowGroupBy: 'action',
  searchFilterQuery: '(http_method:GET)',
  searchFromValue: 'action:show',
  searchWithinMs: 300000,
  streams: 'streamId-1-title, streamId-2-title',
};

export const urlConfigWithAgg = {
  agg_field: 'action',
  agg_function: 'count',
  agg_value: 400,
  group_by: [
    'action',
    'action',
    'http_method',
  ],
  loc_query_parameters: [
    {
      binding: undefined,
      data_type: 'any',
      default_value: 'GET',
      description: '',
      key: 'lt',
      lookup_table: 'http_method',
      name: 'newParameter',
      optional: false,
      title: 'lt',
      type: 'lut-parameter-v1',
    },
  ],
  query: '(http_method:GET) AND ((http_method:GET)) AND (action:show)',
  search_within_ms: 300000,
  streams: [
    'streamId-1',
    'streamId-2',
  ],
  type: 'aggregation-v1',
};

export const urlConfigWithFunctionAgg = {
  agg_function: 'count',
  agg_value: 400,
  group_by: [
    'action',
    'action',
    'http_method',
  ],
  loc_query_parameters: [
    {
      binding: undefined,
      data_type: 'any',
      default_value: 'GET',
      description: '',
      key: 'lt',
      lookup_table: 'http_method',
      name: 'newParameter',
      optional: false,
      title: 'lt',
      type: 'lut-parameter-v1',
    },
  ],
  query: '(http_method:GET) AND ((http_method:GET)) AND (action:show)',
  search_within_ms: 300000,
  streams: [
    'streamId-1',
    'streamId-2',
  ],
  type: 'aggregation-v1',
};

export const urlConfigWithoutAgg = {
  group_by: [],
  loc_query_parameters: [
    {
      binding: undefined,
      data_type: 'any',
      default_value: 'GET',
      description: '',
      key: 'lt',
      lookup_table: 'http_method',
      name: 'newParameter',
      optional: false,
      title: 'lt',
      type: 'lut-parameter-v1',
    },
  ],
  query: '(http_method:GET) AND ((http_method:GET)) AND (action:show)',
  search_within_ms: 300000,
  streams: [
    'streamId-1',
    'streamId-2',
  ],
  type: 'aggregation-v1',
};

export const urlConfigWithAggString = JSON.stringify(urlConfigWithAgg);
export const urlConfigWithFunctionAggString = JSON.stringify(urlConfigWithFunctionAgg);
export const urlConfigWithoutAggString = JSON.stringify(urlConfigWithoutAgg);
