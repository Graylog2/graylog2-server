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
import type { RootState, ExtraArguments, SearchExecutionResult } from 'views/types';
import type { AppDispatch } from 'stores/useAppDispatch';
import { asMock } from 'helpers/mocking';
import { createSearch } from 'fixtures/searches';
import type View from 'views/logic/views/View';
import type { SearchExecutors } from 'views/logic/slices/searchExecutionSlice';
import SearchMetadata from 'views/logic/search/SearchMetadata';

const defaultState = { view: { view: createSearch(), activeQuery: 'query-id-1' } } as RootState;
const mockSearchExecutors: SearchExecutors = {
  resultMapper: (r) => r,
  parse: async () => SearchMetadata.empty(),
  execute: async () => ({}) as SearchExecutionResult,
};

const mockDispatch = (state: RootState = defaultState) => {
  const dispatch: AppDispatch = jest.fn();

  asMock(dispatch).mockImplementation((fn: (d: AppDispatch, getState: () => RootState, extraArgs: ExtraArguments) => unknown) => (typeof fn === 'function'
    ? fn(dispatch, () => state, { searchExecutors: mockSearchExecutors })
    : fn));

  return dispatch;
};

export const mockDispatchForView = (view: View, initialQuery: string = 'query-id-1') => {
  const state = { ...defaultState, view: { view, activeQuery: initialQuery } } as RootState;
  const dispatch: AppDispatch = jest.fn();

  asMock(dispatch).mockImplementation((fn: (d: AppDispatch, getState: () => RootState) => unknown) => (typeof fn === 'function'
    ? fn(dispatch, () => state)
    : fn));

  return dispatch;
};

export default mockDispatch;
