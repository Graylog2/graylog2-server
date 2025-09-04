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
import { Map } from 'immutable';

import type Widget from 'views/logic/widgets/Widget';
import type View from 'views/logic/views/View';
import type WidgetPosition from 'views/logic/widgets/WidgetPosition';
import GenerateNextPosition from 'views/logic/views/GenerateNextPosition';
import SetWidgetTitle from 'views/logic/views/SetWidgetTitle';
import type { QueryId } from 'views/logic/queries/Query';

const AddWidgetToDashboardTab = (
  widget: Widget,
  targetQueryId: QueryId | undefined,
  dashboard: View,
  oldPosition: WidgetPosition,
  title: string | undefined | null,
): View => {
  const dashboardQueryId = targetQueryId ?? dashboard.search.queries.first().id;
  const viewState = dashboard.state.get(dashboardQueryId);
  const widgets = viewState.widgets.push(widget);

  const { widgetPositions } = viewState;
  const newWidgetPositions: Map<string, WidgetPosition> = GenerateNextPosition(
    Map(widgetPositions),
    widgets.toArray(),
    oldPosition?.height,
    oldPosition?.width,
  );

  const titlesMap = viewState.titles;
  const newTitlesMap = SetWidgetTitle(titlesMap, widget, title);

  const newViewState = viewState
    .toBuilder()
    .widgets(widgets)
    .titles(newTitlesMap)
    .widgetPositions(newWidgetPositions)
    .build();

  return dashboard.toBuilder().state(dashboard.state.set(dashboardQueryId, newViewState)).build();
};

export default AddWidgetToDashboardTab;
