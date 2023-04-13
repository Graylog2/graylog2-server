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
import * as Immutable from 'immutable';

import FieldType from 'views/logic/fieldtypes/FieldType';
import Query from 'views/logic/queries/Query';
import { MISSING_BUCKET_NAME } from 'views/Constants';
import { createSearch } from 'fixtures/searches';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import mockDispatch from 'views/test/mockDispatch';
import { updateQueryString } from 'views/logic/slices/viewSlice';
import type { RootState } from 'views/types';

import AddToQueryHandler from './AddToQueryHandler';

import type { ViewType } from '../views/View';
import View from '../views/View';
import GlobalOverride from '../search/GlobalOverride';

const createQuery = (id: string, queryString: string = '') => Query.builder()
  .id(id)
  .query({ type: 'elasticsearch', query_string: queryString })
  .build();

jest.mock('views/logic/slices/viewSlice', () => ({
  ...jest.requireActual('views/logic/slices/viewSlice'),
  updateQueryString: jest.fn(() => Promise.resolve()),
}));

describe('AddToQueryHandler', () => {
  const defaultView = createSearch();

  const createViewWithQuery = (query: Query, type: ViewType = View.Type.Search) => {
    const { search } = defaultView;

    return defaultView
      .toBuilder()
      .type(type)
      .search(search.toBuilder().queries([query]).build())
      .build();
  };

  const mockRootState = {
    searchExecution: { executionState: SearchExecutionState.empty() },
  };

  it('formats date field for ES', async () => {
    const query = createQuery('queryId');
    const view = createViewWithQuery(query);
    const state = { ...mockRootState, view: { view } } as RootState;
    const dispatch = mockDispatch(state);

    await dispatch(AddToQueryHandler({
      queryId: 'queryId',
      field: 'timestamp',
      value: '2019-01-17T11:00:09.025Z',
      type: new FieldType('date', [], []),
    }));

    expect(updateQueryString).toHaveBeenCalledWith('queryId', 'timestamp:"2019-01-17T11:00:09.025Z"');
  });

  it('updates query string before adding predicate', async () => {
    const query = createQuery('anotherQueryId', 'foo:23');
    const view = createViewWithQuery(query);
    const state = { ...mockRootState, view: { view } } as RootState;
    const dispatch = mockDispatch(state);

    await dispatch(AddToQueryHandler({
      queryId: 'anotherQueryId',
      field: 'bar',
      value: 42,
      type: new FieldType('keyword', [], []),
    }));

    expect(updateQueryString).toHaveBeenCalledWith('anotherQueryId', 'foo:23 AND bar:42');
  });

  it('appends NOT _exists_ fragment for proper field in case of missing bucket in input', async () => {
    const query = createQuery('anotherQueryId', 'foo:23');
    const view = createViewWithQuery(query);
    const state = { ...mockRootState, view: { view } } as RootState;
    const dispatch = mockDispatch(state);

    await dispatch(AddToQueryHandler({
      queryId: 'anotherQueryId',
      field: 'bar',
      value: MISSING_BUCKET_NAME,
      type: new FieldType('keyword', [], []),
    }));

    expect(updateQueryString).toHaveBeenCalledWith('anotherQueryId', 'foo:23 AND NOT _exists_:bar');
  });

  describe('for dashboards', () => {
    const query = createQuery('queryId');
    const view = createViewWithQuery(query, View.Type.Dashboard);
    const mockDashboardRootState = {
      view: { view },
    };

    it('retrieves query string from global override', async () => {
      const state = {
        ...mockDashboardRootState,
        searchExecution: {
          executionState: SearchExecutionState.create(
            Immutable.Map(),
            GlobalOverride.create(undefined, { type: 'elasticsearch', query_string: 'something' }),
          ),
        },
      } as RootState;
      const dispatch = mockDispatch(state);

      await dispatch(AddToQueryHandler({
        queryId: 'queryId',
        field: 'bar',
        value: 42,
        type: new FieldType('keyword', [], []),
      }));

      expect(updateQueryString).toHaveBeenCalledWith('queryId', 'something AND bar:42');
    });

    it('creates query string when global override is undefined', async () => {
      const state = {
        ...mockDashboardRootState,
        searchExecution: {
          executionState: SearchExecutionState.create(
            Immutable.Map(),
            undefined,
          ),
        },
      } as RootState;
      const dispatch = mockDispatch(state);

      await dispatch(AddToQueryHandler({
        queryId: 'queryId',
        field: 'bar',
        value: 42,
        type: new FieldType('keyword', [], []),
      }));

      expect(updateQueryString).toHaveBeenCalledWith('queryId', 'bar:42');
    });
  });
});
