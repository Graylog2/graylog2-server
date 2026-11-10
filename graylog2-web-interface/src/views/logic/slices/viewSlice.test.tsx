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
import * as React from 'react';
import { useStore } from 'react-redux';
import { renderHook } from 'wrappedTestingLibrary/hooks';
import { act } from 'wrappedTestingLibrary';

import { asMock } from 'helpers/mocking';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';
import createSearch from 'views/logic/slices/createSearch';
import NewQueryActionHandler from 'views/logic/NewQueryActionHandler';
import Query from 'views/logic/queries/Query';
import ViewState from 'views/logic/views/ViewState';
import type Search from 'views/logic/search/Search';
import type { RootState } from 'views/types';
import type { ViewsDispatch } from 'views/stores/useViewsDispatch';
import { selectActiveQuery, selectViewStates } from 'views/logic/slices/viewSelectors';

import { createQuery } from './viewSlice';

jest.mock('views/logic/slices/createSearch');
jest.mock('views/logic/NewQueryActionHandler');
jest.mock('views/logic/slices/searchExecutionSlice', () => ({
  ...jest.requireActual('views/logic/slices/searchExecutionSlice'),
  execute: jest.fn(() => async () => {}),
}));

const wrapper = ({ children }: React.PropsWithChildren) => <TestStoreProvider>{children}</TestStoreProvider>;

describe('viewSlice', () => {
  useViewsPlugin();

  beforeEach(() => {
    let counter = 0;

    asMock(NewQueryActionHandler).mockImplementation(async () => {
      counter += 1;

      return [Query.builder().id(`new-query-${counter}`).build(), ViewState.create()] as const;
    });
  });

  it('keeps the active query consistent when pages are created concurrently', async () => {
    const pendingSearches: Array<() => void> = [];
    asMock(createSearch).mockImplementation(
      (search: Search) =>
        new Promise((resolve) => {
          pendingSearches.push(() => resolve(search));
        }),
    );

    const { result } = renderHook(() => useStore<RootState>(), { wrapper });
    const store = result.current;
    const dispatch = store.dispatch as ViewsDispatch;
    const inconsistentStates: Array<string> = [];
    store.subscribe(() => {
      const state = store.getState();
      const activeQuery = selectActiveQuery(state);

      if (!selectViewStates(state).has(activeQuery)) {
        inconsistentStates.push(activeQuery);
      }
    });

    let first: Promise<unknown>;
    let second: Promise<unknown>;

    await act(async () => {
      first = dispatch(createQuery());
      second = dispatch(createQuery());
    });

    await act(async () => {
      while (pendingSearches.length > 0) {
        pendingSearches.shift()();
        // eslint-disable-next-line no-await-in-loop
        await new Promise((resolve) => {
          setTimeout(resolve, 0);
        });
      }

      await Promise.all([first, second]);
    });

    expect(inconsistentStates).toEqual([]);
    expect(selectViewStates(store.getState()).keySeq().toArray()).toEqual([
      'query-id-1',
      'new-query-1',
      'new-query-2',
    ]);
  });
});
