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
import uuid from 'uuid/v4';

import type { QueryId } from 'views/logic/queries/Query';
import type { WidgetId } from 'views/logic/views/types';
import type { TitlesMap } from 'views/stores/TitleTypes';

import View from './View';
import FindWidgetAndQueryIdInView from './FindWidgetAndQueryIdInView';
import UpdateSearchForWidgets from './UpdateSearchForWidgets';
import GenerateNextPosition from './GenerateNextPosition';

import Widget from '../widgets/Widget';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';

const _removeWidgetTitle = (titlesMap: TitlesMap, widgetId: WidgetId): TitlesMap => {
  const widgetTitles = titlesMap.get('widget');

  if (!widgetTitles) {
    return titlesMap;
  }

  const newWidgetTitles = widgetTitles.remove(widgetId);

  return titlesMap.set('widget', newWidgetTitles);
};

const _removeWidgetFromTab = (widgetId: WidgetId, queryId: QueryId, dashboard: View): View => {
  const viewState = dashboard.state.get(queryId);
  const widgetIndex = viewState.widgets.findIndex((widget) => widget.id === widgetId);
  const { widgetPositions, titles } = viewState;
  const newTitles = _removeWidgetTitle(titles, widgetId);

  delete widgetPositions[widgetId];
  const { widgetMapping } = viewState;
  const newWidgetMapping = widgetMapping.remove(widgetId);
  const newViewState = viewState.toBuilder()
    .widgets(viewState.widgets.delete(widgetIndex))
    .widgetMapping(newWidgetMapping)
    .titles(newTitles)
    .widgetPositions(widgetPositions)
    .build();

  return dashboard.toBuilder()
    .state(dashboard.state.set(queryId, newViewState))
    .build();
};

const _setWidgetTitle = (titlesMap: TitlesMap, widgetID: WidgetId, newTitle: string | undefined | null): TitlesMap => {
  if (!newTitle) {
    return titlesMap;
  }

  const widgetTitlesMap = titlesMap.get('widget', Immutable.Map());
  const newWidgetTitleMap = widgetTitlesMap.set(widgetID, newTitle);

  return titlesMap.set('widget', newWidgetTitleMap);
};

const _addWidgetToTab = (widget: Widget, targetQueryId: QueryId, dashboard: View, widgetTitle: string | undefined | null, oldPosition: WidgetPosition): View => {
  const viewState = dashboard.state.get(targetQueryId);
  const newWidget = widget.toBuilder().id(uuid()).build();
  const newWidgets = viewState.widgets.push(newWidget);
  const { widgetPositions } = viewState;
  const newWidgetPositions = GenerateNextPosition(Immutable.Map(widgetPositions),
    newWidgets.toArray(), Immutable.Map({ [newWidget.id]: oldPosition }));
  const newTitleMap = _setWidgetTitle(viewState.titles, newWidget.id, widgetTitle);
  const newViewState = viewState.toBuilder()
    .widgets(newWidgets)
    .titles(newTitleMap)
    .widgetPositions(newWidgetPositions)
    .build();

  return dashboard.toBuilder()
    .state(dashboard.state.set(targetQueryId, newViewState))
    .build();
};

const _getWidgetTitle = (widgetId: WidgetId, queryId: QueryId, view: View): string | undefined | null => {
  return view.state.get(queryId).titles.getIn(['widget', widgetId]);
};

const MoveWidgetToTab = (widgetId: WidgetId, targetQueryId: QueryId, dashboard: View, copy: boolean = false): View | undefined | null => {
  if (dashboard.type !== View.Type.Dashboard) {
    throw new Error(`Unexpected type ${dashboard.type} expected ${View.Type.Dashboard}`);
  }

  const match: [Widget, QueryId] | undefined | null = FindWidgetAndQueryIdInView(widgetId, dashboard);

  if (match) {
    const [widget, queryId] = match;
    const widgetTitle = _getWidgetTitle(widgetId, queryId, dashboard);
    const { widgetPositions } = dashboard.state.get(queryId);
    const oldPosition = widgetPositions[widgetId];

    const tempDashboard = copy ? dashboard : _removeWidgetFromTab(widgetId, queryId, dashboard);
    const newWidget = copy ? widget.toBuilder().newId().build() : widget;

    return UpdateSearchForWidgets(_addWidgetToTab(newWidget, targetQueryId, tempDashboard, widgetTitle, oldPosition));
  }

  return undefined;
};

export default MoveWidgetToTab;
