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
import { PluginStore } from 'graylog-web-plugin/plugin';

import View from 'views/logic/views/View';
import type Query from 'views/logic/queries/Query';
import AddWidgetToDashboardTab from 'views/logic/views/AddWidgetToDashboardTab';
import type { QueryId } from 'views/logic/queries/Query';
import GetWidgetTitle from 'views/logic/views/GetWidgetTitle';

import UpdateSearchForWidgets from './UpdateSearchForWidgets';
import FindWidgetAndQueryIdInView from './FindWidgetAndQueryIdInView';

const CopyWidgetToDashboard = (widgetId: string, source: View, destination: View): View | undefined | null => {
  if (destination.type !== View.Type.Dashboard) {
    throw new Error(`Unexpected type ${destination.type} expected ${View.Type.Dashboard}`);
  }

  const copyHooks = PluginStore.exports('views.hooks.copyWidgetToDashboard');

  const queryMap: Map<QueryId, Query> = Map(source.search.queries.map((q) => [q.id, q]));
  const match = FindWidgetAndQueryIdInView(widgetId, source);

  if (match) {
    const [widget, queryId] = match;
    const { timerange, query, filter = Map(), filters } = queryMap.get(queryId);
    const { widgetPositions } = source.state.get(queryId);
    const oldPositions = widgetPositions[widgetId];
    const title = GetWidgetTitle(widgetId, queryId, source);

    const streams = (filter ? filter.get('filters', List.of()) : List.of())
      .filter((value) => Map.isMap(value) && value.get('type') === 'stream')
      .map((value) => value.get('id'))
      .toList()
      .toArray();

    const dashboardWidget = widget
      .toBuilder()
      .newId()
      .timerange(timerange)
      .query(query)
      .streams(streams)
      .filters(filters)
      .build();

    const updatedView = UpdateSearchForWidgets(
      AddWidgetToDashboardTab(dashboardWidget, undefined, destination, oldPositions, title),
    );

    return copyHooks.reduce((previousDashboard, copyHook) => copyHook(source, previousDashboard), updatedView);
  }

  return undefined;
};

export default CopyWidgetToDashboard;
