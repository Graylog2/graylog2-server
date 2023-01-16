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
import type WidgetPosition from 'views/logic/widgets/WidgetPosition';
import type { AppDispatch } from 'stores/useAppDispatch';
import type { GetState, WidgetPositions } from 'views/types';
import { selectWidgetPositions } from 'views/logic/slices/widgetSelectors';
import { selectActiveQuery, selectActiveViewState } from 'views/logic/slices/viewSelectors';
import { updateViewState } from 'views/logic/slices/viewSlice';

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
