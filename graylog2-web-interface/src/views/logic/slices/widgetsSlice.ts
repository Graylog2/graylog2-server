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
import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';

import type { ViewsDispatch } from 'views/stores/useViewsDispatch';
import type { GetState } from 'views/types';

export type WidgetsState = {
  newWidget: string | undefined;
};
const widgetsSlice = createSlice({
  name: 'widgets',
  initialState: {
    newWidget: undefined,
  },
  reducers: {
    setNewWidget: (state, action: PayloadAction<string | undefined>) => ({
      ...state,
      newWidget: action.payload,
    }),
  },
});
export const widgetsSliceReducer = widgetsSlice.reducer;
export const setNewWidget = (id: string) => (dispatch: ViewsDispatch) =>
  dispatch(widgetsSlice.actions.setNewWidget(id));

// Widgets stay flagged as "new" until the grid actually scrolls them into view (or finds them
// already visible) rather than after a fixed delay, since the widget-creation/edit round trip
// (including a full search re-execution) can easily take longer than any fixed guess.
export const clearNewWidget = (id: string) => (dispatch: ViewsDispatch, getState: GetState) => {
  if (getState().widgets.newWidget === id) {
    dispatch(widgetsSlice.actions.setNewWidget(undefined));
  }
};

export const selectNewWidget = (state: { widgets: WidgetsState }) => state.widgets.newWidget;
