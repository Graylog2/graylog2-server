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
import type { QueryId } from 'views/logic/queries/Query';
import type { WidgetId } from 'views/logic/views/types';
import type { TitlesMap } from 'views/stores/TitleTypes';
import normalizeViewState from 'views/logic/views/normalizeViewState';
import AddWidgetToDashboardTab from 'views/logic/views/AddWidgetToDashboardTab';
import GetWidgetTitle from 'views/logic/views/GetWidgetTitle';

import View from './View';
import FindWidgetAndQueryIdInView from './FindWidgetAndQueryIdInView';
import UpdateSearchForWidgets from './UpdateSearchForWidgets';

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
  const newViewState = viewState
    .toBuilder()
    .widgets(viewState.widgets.delete(widgetIndex))
    .widgetMapping(newWidgetMapping)
    .titles(newTitles)
    .widgetPositions(widgetPositions)
    .build();

  return dashboard
    .toBuilder()
    .state(dashboard.state.set(queryId, normalizeViewState(newViewState)))
    .build();
};

const MoveWidgetToTab = (
  widgetId: WidgetId,
  targetQueryId: QueryId,
  dashboard: View,
  copy: boolean = false,
): View | undefined | null => {
  if (dashboard.type !== View.Type.Dashboard) {
    throw new Error(`Unexpected type ${dashboard.type} expected ${View.Type.Dashboard}`);
  }

  const match = FindWidgetAndQueryIdInView(widgetId, dashboard);

  if (match) {
    const [widget, queryId] = match;
    const widgetTitle = GetWidgetTitle(widgetId, queryId, dashboard);
    const { widgetPositions } = dashboard.state.get(queryId);
    const oldPosition = widgetPositions[widgetId];

    const tempDashboard = copy ? dashboard : _removeWidgetFromTab(widgetId, queryId, dashboard);
    const newWidget = copy ? widget.toBuilder().newId().build() : widget;

    return UpdateSearchForWidgets(
      AddWidgetToDashboardTab(newWidget, targetQueryId, tempDashboard, oldPosition, widgetTitle),
    );
  }

  return undefined;
};

export default MoveWidgetToTab;
