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
import { updateViewNoBufferPush } from 'views/logic/slices/viewSlice';
import type { RootState, ViewState } from 'views/types';
import { selectRootUndoRedo, selectUndoRedoAvailability } from 'views/logic/slices/undoRedoSelectors';

const BUFFER_MAX_SIZE = 10;

type BufferItemType = 'view';

export type BufferItem = { type: BufferItemType, state: any }

export type UndoRedoState = {
  buffer: Array<BufferItem>,
  currentRevision: number
}

const handlers: { [name in BufferItemType]: any} = {
  view: (state: ViewState, hasToPushRevision: boolean = false) => updateViewNoBufferPush({ newView: state.view, recreateSearch: state.isDirty, hasToPushRevision }),
};

const undoRedoSlice = createSlice({
  name: 'undoRedo',
  initialState: {
    buffer: [],
    currentRevision: 0,
  },
  reducers: {
    setBuffer: (state, action) => ({
      ...state,
      buffer: action.payload,
    }),
    setCurrentRevision: (state, action) => ({
      ...state,
      currentRevision: action.payload,
    }),
  },
});
export const undoRedoSliceReducer = undoRedoSlice.reducer;
export const { setBuffer, setCurrentRevision } = undoRedoSlice.actions;

export const pushIntoBuffer = (bufferItem: BufferItem, setAsLastRevision: boolean = true) => async (dispatch: AppDispatch, getState: () => RootState) => {
  const { buffer, currentRevision } = selectRootUndoRedo(getState());
  const isLast = currentRevision === buffer.length;
  // if we are in the middle of the buffer, we have to remove all items after current;
  const cutBuffer = isLast ? buffer : buffer.slice(0, currentRevision);
  // if we reach max size of the buffer we have to remove first item;
  const newBuffer: Array<BufferItem> = (cutBuffer.length < BUFFER_MAX_SIZE) ? [...cutBuffer, bufferItem] : [...cutBuffer.slice(1), bufferItem];
  const newRevision = setAsLastRevision ? newBuffer.length : currentRevision;
  dispatch(setBuffer(newBuffer));
  dispatch(setCurrentRevision(newRevision));
};

export const undo = () => async (dispatch: AppDispatch, getState: () => RootState) => {
  const rootState = getState();
  const { buffer, currentRevision } = selectRootUndoRedo(rootState);
  const { isUndoAvailable } = selectUndoRedoAvailability(rootState);

  const hasToPushRevision = currentRevision === buffer.length;

  if (isUndoAvailable) {
    const newRevision = currentRevision - 1;
    const { type, state } = buffer[newRevision];
    const bufferHandler = handlers[type];

    dispatch(bufferHandler(state, hasToPushRevision)).then(() => dispatch(setCurrentRevision(newRevision)));
  }
};

export const redo = () => async (dispatch: AppDispatch, getState: () => RootState) => {
  const rootState = getState();
  const { buffer, currentRevision } = selectRootUndoRedo(rootState);
  const { isRedoAvailable } = selectUndoRedoAvailability(rootState);

  if (isRedoAvailable) {
    const newRevision = currentRevision + 1;

    const { type, state } = buffer[newRevision];
    const bufferHandler = handlers[type];
    dispatch(bufferHandler(state)).then(() => dispatch(setCurrentRevision(newRevision)));
  }
};
