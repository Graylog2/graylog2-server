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
import type * as Immutable from 'immutable';

import type WidgetPosition from 'views/logic/widgets/WidgetPosition';
import type { AppDispatch } from 'stores/useAppDispatch';
import type { GetState, WidgetPositions } from 'views/types';
import { selectWidgetPositions } from 'views/logic/slices/widgetSelectors';
import { selectActiveQuery, selectActiveViewState, selectWidgets, selectWidget } from 'views/logic/slices/viewSelectors';
import { updateViewState } from 'views/logic/slices/viewSlice';
import type Widget from 'views/logic/widgets/Widget';
import type WidgetConfig from 'views/logic/widgets/WidgetConfig';
import generateId from 'logic/generateId';
import { setTitle } from 'views/logic/slices/titlesActions';

export const updateWidgetPositions = (newWidgetPositions: WidgetPositions) => (dispatch: AppDispatch, getState: GetState) => {
  const activeQuery = selectActiveQuery(getState());
  const activeViewState = selectActiveViewState(getState());
  const newViewState = activeViewState.toBuilder()
    .widgetPositions(newWidgetPositions)
    .build();

  return dispatch(updateViewState(activeQuery, newViewState));
};

export const updateWidgetPosition = (id: string, newWidgetPosition: WidgetPosition) => (dispatch: AppDispatch, getState: GetState) => {
  const widgetPositions = selectWidgetPositions(getState());
  const newWidgetPositions = { ...widgetPositions, [id]: newWidgetPosition };

  return dispatch(updateWidgetPositions(newWidgetPositions));
};

export const updateWidgets = (newWidgets: Immutable.List<Widget>) => (dispatch: AppDispatch, getState: GetState) => {
  const activeQuery = selectActiveQuery(getState());
  const activeViewState = selectActiveViewState(getState());
  const newViewState = activeViewState.toBuilder()
    .widgets(newWidgets)
    .build();

  return dispatch(updateViewState(activeQuery, newViewState));
};

export const addWidget = (widget: Widget) => (dispatch: AppDispatch, getState: GetState) => {
  if (widget.id === undefined) {
    throw new Error('Unable to add widget without id to query.');
  }

  const widgets = selectWidgets(getState());
  const newWidgets = widgets.push(widget);

  return dispatch(updateWidgets(newWidgets));
};

export const updateWidget = (id: string, updatedWidget: Widget) => (dispatch: AppDispatch, getState: GetState) => {
  const widgets = selectWidgets(getState());
  const newWidgets = widgets.map((widget) => (widget.id === id ? updatedWidget : widget)).toList();

  return dispatch(updateWidgets(newWidgets));
};

export const updateWidgetConfig = (id: string, newWidgetConfig: WidgetConfig) => (dispatch: AppDispatch, getState: GetState) => {
  const widget = selectWidget(id)(getState());
  const newWidget = widget.toBuilder()
    .config(newWidgetConfig)
    .build();

  return dispatch(updateWidget(id, newWidget));
};

export const duplicateWidget = (widgetId: string, widgetTitle: string) => async (dispatch: AppDispatch, getState: GetState) => {
  const widget = selectWidget(widgetId)(getState());

  if (!widget) {
    throw new Error(`Unable to duplicate widget with id "${widgetId}", it is not found.`);
  }

  const activeQuery = selectActiveQuery(getState());

  const duplicatedWidget = widget.duplicate(generateId());

  return dispatch(addWidget(duplicatedWidget))
    .then(() => dispatch(setTitle(activeQuery, 'widget', duplicatedWidget.id, `${widgetTitle} (copy)`)));
};
