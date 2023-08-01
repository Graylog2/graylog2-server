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

import type { AppDispatch } from 'stores/useAppDispatch';
import type { RootState } from 'views/types';
import { selectRootUndoRedo } from 'views/logic/slices/undoRedoSelectors';

const REVISIONS_MAX_SIZE = 10;

type RevisionItemType = 'view';

export type RevisionItem = { type: RevisionItemType, state: any }

export type UndoRedoState = {
  revisions: Array<RevisionItem>,
  currentRevision: number
}

const undoRedoSlice = createSlice({
  name: 'undoRedo',
  initialState: {
    revisions: [],
    currentRevision: 0,
  },
  reducers: {
    setRevisions: (state, action) => ({
      ...state,
      revisions: action.payload.revisions,
      currentRevision: action.payload.currentRevision,
    }),
    setCurrentRevision: (state, action) => ({
      ...state,
      currentRevision: action.payload,
    }),
  },
});
export const undoRedoSliceReducer = undoRedoSlice.reducer;
export const { setRevisions, setCurrentRevision } = undoRedoSlice.actions;

export const pushIntoRevisions = (revisionItem: RevisionItem, setAsLastRevision: boolean = true) => async (dispatch: AppDispatch, getState: () => RootState) => {
  const { revisions, currentRevision } = selectRootUndoRedo(getState());
  const isLast = currentRevision === revisions.length;
  // if we are in the middle of the buffer, we have to remove all items after current;
  const cutRevisions = isLast ? revisions : revisions.slice(0, currentRevision);
  // if we reach max size of the buffer we have to remove first item;
  const newRevisions: Array<RevisionItem> = (cutRevisions.length < REVISIONS_MAX_SIZE) ? [...cutRevisions, revisionItem] : [...cutRevisions.slice(1), revisionItem];
  const newRevision = setAsLastRevision ? newRevisions.length : currentRevision;
  dispatch(setRevisions({ revisions: newRevisions, currentRevision: newRevision }));
};
