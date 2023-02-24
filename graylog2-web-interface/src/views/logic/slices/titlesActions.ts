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
import type { TitleType, TitlesMap } from 'views/stores/TitleTypes';
import type { AppDispatch } from 'stores/useAppDispatch';
import type { GetState } from 'views/types';
import { selectViewState } from 'views/logic/slices/viewSelectors';
import { updateViewState } from 'views/logic/slices/viewSlice';
import { selectTitles } from 'views/logic/slices/titlesSelectors';

export const updateTitles = (id: string, newTitles: TitlesMap) => async (dispatch: AppDispatch, getState: GetState) => {
  const viewState = selectViewState(id)(getState());
  const newViewState = viewState.toBuilder()
    .titles(newTitles)
    .build();

  return dispatch(updateViewState(id, newViewState));
};

export const setTitle = (queryId: string, type: TitleType, id: string, title: string) => async (dispatch: AppDispatch, getState: GetState) => {
  const viewState = selectViewState(queryId)(getState());
  const titles = selectTitles(queryId)(getState());
  const newTitles = titles.setIn([type, id], title);
  const newViewState = viewState.toBuilder()
    .titles(newTitles)
    .build();

  return dispatch(updateViewState(queryId, newViewState));
};
