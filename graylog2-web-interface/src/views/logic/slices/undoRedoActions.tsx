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
import { selectQuery, updateView } from 'views/logic/slices/viewSlice';
import { setCurrentRevision } from 'views/logic/slices/undoRedoSlice';

const viewHandler = (state: ViewState, { hasToPushRevision, dispatch }: {
  hasToPushRevision: boolean,
  dispatch: AppDispatch
}): Promise<unknown> => dispatch(selectQuery(state.activeQuery)).then(() => dispatch(updateView(state.view, state.isDirty, { hasToPushRevision })));

export const undo = () => async (dispatch: AppDispatch, getState: () => RootState) => {
  const rootState = getState();
  const { revisions, currentRevision } = selectRootUndoRedo(rootState);
  const { isUndoAvailable } = selectUndoRedoAvailability(rootState);

  const hasToPushRevision = currentRevision === revisions.length;

  if (isUndoAvailable) {
    const newRevision = currentRevision - 1;
    const { state } = revisions[newRevision];

    viewHandler(state, { hasToPushRevision, dispatch }).then(() => dispatch(setCurrentRevision(newRevision)));
  }
};

export const redo = () => async (dispatch: AppDispatch, getState: () => RootState) => {
  const rootState = getState();
  const { revisions, currentRevision } = selectRootUndoRedo(rootState);
  const { isRedoAvailable } = selectUndoRedoAvailability(rootState);

  if (isRedoAvailable) {
    const newRevision = currentRevision + 1;

    const { state } = revisions[newRevision];

    viewHandler(state, { dispatch, hasToPushRevision: false }).then(() => dispatch(setCurrentRevision(newRevision)));
  }
};
