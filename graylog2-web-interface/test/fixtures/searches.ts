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

/* eslint-disable import/prefer-default-export */

import { Map } from 'immutable';

import Query from 'views/logic/queries/Query';
import Search from 'views/logic/search/Search';
import View from 'views/logic/views/View';
import ViewState from 'views/logic/views/ViewState';
import type _Widget from 'views/logic/widgets/Widget';
import type { WidgetPositions } from 'views/types';

export const createSearch = ({ searchId, queryId }: { searchId?: string, queryId?: string} = {}) => {
  const exampleSearchId = searchId ?? 'search-id-1';
  const exampleQueryId = queryId ?? 'query-id-1';

  const viewState = ViewState.builder()
    .titles(Map())
    .build();
  const query = Query.builder().id(exampleQueryId).build();
  const searchSearch = Search.builder().queries([query]).id(exampleSearchId).build();

  return View.builder()
    .search(searchSearch)
    .type(View.Type.Dashboard)
    .state(Map({ [exampleQueryId]: viewState }))
    .id(exampleSearchId)
    .title('Search 1')
    .build();
};

export const createViewWithWidgets = (widgets: Array<_Widget>, positions: WidgetPositions) => {
  const view = createSearch();
  const newViewState = view.state.get('query-id-1')
    .toBuilder()
    .widgets(widgets)
    .widgetPositions(positions)
    .build();

  return view
    .toBuilder()
    .state(Map({ 'query-id-1': newViewState }))
    .build();
};
