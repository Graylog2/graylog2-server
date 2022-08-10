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

import Query from 'views/logic/queries/Query';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import Direction from 'views/logic/aggregationbuilder/Direction';
import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import MessagesWidget from 'views/logic/widgets/MessagesWidget';
import type { ViewStateMap, ViewType } from 'views/logic/views/View';
import View from 'views/logic/views/View';
import ViewState from 'views/logic/views/ViewState';
import Search from 'views/logic/search/Search';
import type { TitleType } from 'views/stores/TitleTypes';

const searchType = {
  id: 'search-type-id-1',
  type: 'messages',
  streams: [],
  sort: [],
  filters: [],
  filter: '',
  name: null,
  query: null,
  timerange: null,
  limit: 150,
  decorators: [],
  offset: 0,
};
const queries = [
  Query.builder().id('query-id-1').searchTypes([searchType]).build(),
];
const currentSort = new SortConfig(SortConfig.PIVOT_TYPE, 'level', Direction.Descending);
const config = new MessagesWidgetConfig(['level', 'http_method'], true, false, [], [currentSort]);

export const messagesWidget = (id: string = 'widget-id-1') => MessagesWidget.builder()
  .id(id)
  .config(config)
  .build();
const states: ViewStateMap = Immutable.Map({
  'query-id-1': ViewState.create(),
});
const searchWithQueries = Search.builder()
  .id('search-id')
  .queries(queries)
  .build();
export const viewWithoutWidget = (viewType: ViewType) => View.create()
  .toBuilder()
  .id('deadbeef')
  .type(viewType)
  .search(searchWithQueries)
  .state(states)
  .build();
// Prepare view with one widget
export const stateWithOneWidget = (widget) => ViewState.builder()
  .widgets(Immutable.List([widget]))
  .widgetMapping(Immutable.Map({ 'widget-id-1': Immutable.Set(['search-type-id-1']) }))
  .titles(Immutable.Map<TitleType, Immutable.Map<string, string>>({ widget: Immutable.Map<string, string>({ 'widget-id-1': 'Widget 1' }) }))
  .build();
export const viewWithOneWidget = (viewType) => viewWithoutWidget(viewType)
  .toBuilder()
  .state(Immutable.Map({ 'query-id-1': stateWithOneWidget(messagesWidget()) }))
  .build();
// Prepare view with multiple widgets
const stateWithMultipleWidgets: ViewState = ViewState.builder()
  .widgets(Immutable.List([messagesWidget('widget-id-1'), messagesWidget('widget-id-2')]))
  .widgetMapping(Immutable.Map({ 'widget-id-1': Immutable.Set(['search-type-id-1']) }))
  .titles(Immutable.Map<TitleType, Immutable.Map<string, string>>({ widget: Immutable.Map({ 'widget-id-1': 'Widget 1', 'widget-id-2': 'Widget 2' }) }))
  .build();
export const viewWithMultipleWidgets = (viewType) => viewWithoutWidget(viewType)
  .toBuilder()
  .state(Immutable.Map({ 'query-id-1': stateWithMultipleWidgets }))
  .build();
