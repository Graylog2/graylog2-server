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
import * as Immutable from 'immutable';

import type Widget from 'views/logic/widgets/Widget';
import View from 'views/logic/views/View';
import type Query from 'views/logic/queries/Query';
import { ConcatPositions } from 'views/logic/views/GenerateNextPosition';
import type WidgetPosition from 'views/logic/widgets/WidgetPosition';
import TitleTypes from 'views/stores/TitleTypes';

import UpdateSearchForWidgets from './UpdateSearchForWidgets';
import FindWidgetAndQueryIdInView from './FindWidgetAndQueryIdInView';

type QueryId = string;

const _newPositionsMap = (oldPosition, widgetPositions, widget): Immutable.Map<string, WidgetPosition> => {
  const newWidgetPositions: Immutable.Map<string, WidgetPosition> = oldPosition
    ? ConcatPositions(Immutable.Map({ [widget.id]: oldPosition.toBuilder().row(1).col(1).build() }), Immutable.Map(widgetPositions))
    : Immutable.Map(widgetPositions);

  return newWidgetPositions;
};

const _newTitlesMap = (titlesMap, widget, title) => {
  if (!title) {
    return titlesMap;
  }

  const widgetTitles = titlesMap.get(TitleTypes.Widget, Map());
  const newWidgetTitles = widgetTitles.set(widget.id, title);

  return titlesMap.set(TitleTypes.Widget, newWidgetTitles);
};

const _addWidgetToDashboard = (widget: Widget, dashboard: View, oldPosition: WidgetPosition, title: string | undefined | null): View => {
  const dashboardQueryId = dashboard.search.queries.first().id;
  const viewState = dashboard.state.get(dashboardQueryId);
  const widgets = viewState.widgets.push(widget);

  const { widgetPositions } = viewState;
  const newWidgetPositions: Map<string, WidgetPosition> = _newPositionsMap(oldPosition, widgetPositions, widget);

  const titlesMap = viewState.titles;
  const newTitlesMap = _newTitlesMap(titlesMap, widget, title);

  const newViewState = viewState.toBuilder()
    .widgets(widgets)
    .titles(newTitlesMap)
    .widgetPositions(newWidgetPositions)
    .build();

  return dashboard.toBuilder()
    .state(dashboard.state.set(dashboardQueryId, newViewState))
    .build();
};

const CopyWidgetToDashboard = (widgetId: string, search: View, dashboard: View): View | undefined | null => {
  if (dashboard.type !== View.Type.Dashboard) {
    return undefined;
  }

  const copyHooks = PluginStore.exports('views.hooks.copyWidgetToDashboard');

  const queryMap: Map<QueryId, Query> = Map(search.search.queries.map((q) => [q.id, q]));
  const match: [Widget, QueryId] | undefined | null = FindWidgetAndQueryIdInView(widgetId, search);

  if (match) {
    const [widget, queryId] = match;
    const { timerange, query, filter = Map(), filters } = queryMap.get(queryId);
    const { widgetPositions } = search.state.get(queryId);
    const oldPositions = widgetPositions[widgetId];
    const title = search.state.get(queryId).titles.get(TitleTypes.Widget).get(widgetId);

    const streams = (filter ? filter.get('filters', List.of()) : List.of())
      .filter((value) => Map.isMap(value) && value.get('type') === 'stream')
      .map((value) => value.get('id'))
      .toList()
      .toArray();

    const dashboardWidget = widget.toBuilder()
      .newId()
      .timerange(timerange)
      .query(query)
      .streams(streams)
      .filters(filters)
      .build();

    const updatedView = UpdateSearchForWidgets(_addWidgetToDashboard(dashboardWidget, dashboard, oldPositions, title));

    return copyHooks.reduce((previousDashboard, copyHook) => copyHook(search, previousDashboard), updatedView);
  }

  return undefined;
};

export default CopyWidgetToDashboard;
