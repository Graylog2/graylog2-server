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
// @flow strict
import { List, Map } from 'immutable';

import Widget from 'views/logic/widgets/Widget';
import View from 'views/logic/views/View';
import Query from 'views/logic/queries/Query';

import FindWidgetAndQueryIdInView from './FindWidgetAndQueryIdInView';
import UpdateSearchForWidgets from './UpdateSearchForWidgets';

type QueryId = string;

const _addWidgetToDashboard = (widget: Widget, dashboard: View): View => {
  const dashboardQueryId = dashboard.state.keySeq().first();
  const viewState = dashboard.state.get(dashboardQueryId);
  const newViewState = viewState.toBuilder()
    .widgets(viewState.widgets.push(widget))
    .build();

  return dashboard.toBuilder()
    .state(dashboard.state.set(dashboardQueryId, newViewState))
    .build();
};

const CopyWidgetToDashboard = (widgetId: string, search: View, dashboard: View): ?View => {
  if (dashboard.type !== View.Type.Dashboard) {
    return undefined;
  }

  const queryMap: Map<QueryId, Query> = Map(search.search.queries.map((q) => [q.id, q]));
  const match: ?[Widget, QueryId] = FindWidgetAndQueryIdInView(widgetId, search);

  if (match) {
    const [widget, queryId] = match;
    const { timerange, query, filter = Map() } = queryMap.get(queryId);

    const streams = (filter ? filter.get('filters', List.of()) : List.of())
      .filter((value) => Map.isMap(value) && value.get('type') === 'stream')
      .map((value) => value.get('id'))
      .toList()
      .toArray();

    const dashboardWidget = widget.toBuilder()
      .timerange(timerange)
      .query(query)
      .streams(streams)
      .build();

    return UpdateSearchForWidgets(_addWidgetToDashboard(dashboardWidget, dashboard));
  }

  return undefined;
};

export default CopyWidgetToDashboard;
