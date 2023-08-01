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

import type { AppDispatch } from 'stores/useAppDispatch';
import type { RootState, ViewState } from 'views/types';
import { selectRootUndoRedo, selectUndoRedoAvailability } from 'views/logic/slices/undoRedoSelectors';
import { isViewWidgetsEqualForSearch, selectQuery, updateView } from 'views/logic/slices/viewSlice';
import { setCurrentRevision } from 'views/logic/slices/undoRedoSlice';
import { selectRootView } from 'views/logic/slices/viewSelectors';
import type View from 'views/logic/views/View';
import isEqualForSearch from 'views/stores/isEqualForSearch';
import type Search from 'views/logic/search/Search';

const isViewSearchEqual = (first: Search, second: Search) => isEqualForSearch(first, second);
const viewHandler = (state: ViewState, { hasToPushRevision, dispatch, currentView }: {
  hasToPushRevision: boolean,
  dispatch: AppDispatch,
  currentView: View,
}): Promise<unknown> => dispatch(selectQuery(state.activeQuery)).then(() => {
  const shouldRecreateSearch = !isViewWidgetsEqualForSearch(state.view, currentView) || !isViewSearchEqual(state.view.search, currentView.search);

  return dispatch(updateView(state.view, shouldRecreateSearch, { hasToPushRevision }));
});

export const undo = () => async (dispatch: AppDispatch, getState: () => RootState) => {
  const rootState = getState();
  const { revisions, currentRevision } = selectRootUndoRedo(rootState);
  const { isUndoAvailable } = selectUndoRedoAvailability(rootState);
  const { view: currentView } = selectRootView(rootState);
  const hasToPushRevision = currentRevision === revisions.length;

  if (isUndoAvailable) {
    const newRevision = currentRevision - 1;
    const { state } = revisions[newRevision];
    viewHandler(state, { hasToPushRevision, dispatch, currentView }).then(() => dispatch(setCurrentRevision(newRevision)));
  }
};

export const redo = () => async (dispatch: AppDispatch, getState: () => RootState) => {
  const rootState = getState();
  const { revisions, currentRevision } = selectRootUndoRedo(rootState);
  const { view: currentView } = selectRootView(rootState);
  const { isRedoAvailable } = selectUndoRedoAvailability(rootState);

  if (isRedoAvailable) {
    const newRevision = currentRevision + 1;

    const { state } = revisions[newRevision];
    viewHandler(state, { dispatch, hasToPushRevision: false, currentView }).then(() => dispatch(setCurrentRevision(newRevision)));
  }
};
