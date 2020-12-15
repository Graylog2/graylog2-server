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
import { List, Map } from 'immutable';

import Widget from 'views/logic/widgets/Widget';
import type { QueryId } from 'views/logic/queries/Query';
import Query from 'views/logic/queries/Query';

import type { ViewStateMap } from './View';
import View from './View';
import ViewState from './ViewState';

const ViewTransformer = (searchView: View): View => {
  const queryMap: Map<QueryId, Query> = Map(searchView.search.queries.map((q) => [q.id, q]));
  const newViewStateMap: ViewStateMap = (searchView.state || Map()).map((viewState: ViewState, queryId: string) => {
    const { timerange, query, filter = Map() } = queryMap.get(queryId);

    const streams = (filter ? filter.get('filters', List()) : List())
      .filter((value) => Map.isMap(value) && value.get('type') === 'stream')
      .map((value) => value.get('id'))
      .toList()
      .toArray();

    const widgets: List<Widget> = viewState.widgets.map((widget: Widget) => {
      return widget.toBuilder()
        .timerange(timerange)
        .query(query)
        .streams(streams)
        .build();
    }).toList();

    return viewState.toBuilder()
      .widgets(widgets)
      .build();
  }).toMap();
  // Remove query string attached to the existing search query
  const newQueries = searchView.search.queries.map(
    (query) => query.toBuilder().query({ ...query.query, query_string: '' }).build(),
  ).toSet();
  const newSearch = searchView.search.toBuilder().queries(newQueries).build();

  return searchView.toBuilder()
    .newId()
    .type(View.Type.Dashboard)
    .state(newViewStateMap)
    .search(newSearch)
    .build();
};

export default ViewTransformer;
