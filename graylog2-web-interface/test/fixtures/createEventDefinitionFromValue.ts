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

export const valuePath = [
  { http_method: 'GET' },
  { action: 'index' },
];

export const pivots = [
  Pivot.create(['http_method', 'index'], 'values', { limit: 15 }),
  Pivot.create(['resources', 'index'], 'values', { limit: 15 }),
];

export const testAggregationWidget = AggregationWidget.builder()
  .id('summary-widget-id')
  .type('pivot')
  .config(
    AggregationWidgetConfig.builder()
      .columnPivots([Pivot.create(['field1', 'field2'], 'values', { limit: 15 })])
      .rowPivots([Pivot.create(['field3', 'field4'], 'values', { limit: 15 })])
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
const query = QueryGenerator([], 'query-id', {
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
