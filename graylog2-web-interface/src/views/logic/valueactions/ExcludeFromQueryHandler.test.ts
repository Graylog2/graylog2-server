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

import { MISSING_BUCKET_NAME } from 'views/Constants';
import { createSearch } from 'fixtures/searches';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import type { RootState } from 'views/types';
import mockDispatch from 'views/test/mockDispatch';
import { updateQueryString } from 'views/logic/slices/viewSlice';

import ExcludeFromQueryHandler from './ExcludeFromQueryHandler';

import GlobalOverride from '../search/GlobalOverride';
import Query from '../queries/Query';
import type { ViewType } from '../views/View';
import View from '../views/View';

const createQuery = (queryString: string) => Query.builder()
  .id('queryId')
  .query({ type: 'elasticsearch', query_string: queryString })
  .build();

jest.mock('views/logic/slices/viewSlice', () => ({
  ...jest.requireActual('views/logic/slices/viewSlice'),
  updateQueryString: jest.fn(() => Promise.resolve()),
}));

describe('ExcludeFromQueryHandler', () => {
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

  it('adds exclusion term to query', async () => {
    const query = createQuery('');
    const view = createViewWithQuery(query);
    const state = { ...mockRootState, view: { view } } as RootState;
    const dispatch = mockDispatch(state);

    await dispatch(ExcludeFromQueryHandler({
      queryId: 'queryId',
      field: 'something',
      value: 'other',
    }));

    expect(updateQueryString).toHaveBeenCalledWith('queryId', 'NOT something:other');
  });

  it('replaces `*` query completely', async () => {
    const query = createQuery('*');
    const view = createViewWithQuery(query);
    const state = { ...mockRootState, view: { view } } as RootState;
    const dispatch = mockDispatch(state);

    await dispatch(ExcludeFromQueryHandler({
      queryId: 'queryId',
      field: 'foo',
      value: 'bar',
    }));

    expect(updateQueryString).toHaveBeenCalledWith('queryId', 'NOT foo:bar');
  });

  it('appends negated term to existing query', async () => {
    const query = createQuery('answer:42');
    const view = createViewWithQuery(query);
    const state = { ...mockRootState, view: { view } } as RootState;
    const dispatch = mockDispatch(state);

    await dispatch(ExcludeFromQueryHandler({
      queryId: 'queryId',
      field: 'do',
      value: 'panic',
    }));

    expect(updateQueryString).toHaveBeenCalledWith('queryId', 'answer:42 AND NOT do:panic');
  });

  it('appends _exists_ fragment for proper field in case of missing bucket in input', async () => {
    const query = createQuery('answer:42');
    const view = createViewWithQuery(query);
    const state = { ...mockRootState, view: { view } } as RootState;
    const dispatch = mockDispatch(state);

    await dispatch(ExcludeFromQueryHandler({
      queryId: 'queryId',
      field: 'do',
      value: MISSING_BUCKET_NAME,
    }));

    expect(updateQueryString).toHaveBeenCalledWith('queryId', 'answer:42 AND _exists_:do');
  });

  it('escapes special characters in field value', async () => {
    const query = createQuery('*');
    const view = createViewWithQuery(query);
    const state = { ...mockRootState, view: { view } } as RootState;
    const dispatch = mockDispatch(state);

    await dispatch(ExcludeFromQueryHandler({
      queryId: 'queryId',
      field: 'something',
      value: 'foo && || : \\ / + - ! ( ) { } [ ] ^ " ~ * ? bar',
    }));

    expect(updateQueryString).toHaveBeenCalledWith('queryId', 'NOT something:"foo && || : \\\\ / + - ! ( ) { } [ ] ^ \\" ~ * ? bar"');
  });

  describe('for dashboards', () => {
    it('retrieves query string from global override', async () => {
      const query = createQuery('answer:42');
      const view = createViewWithQuery(query, View.Type.Dashboard);
      const state = {
        view: { view },
        searchExecution: {
          executionState: SearchExecutionState.create(
            Immutable.Map(),
            GlobalOverride.create(undefined, { type: 'elasticsearch', query_string: 'something' }),
          ),
        },
      } as RootState;
      const dispatch = mockDispatch(state);

      await dispatch(ExcludeFromQueryHandler({
        queryId: 'queryId',
        field: 'do',
        value: 'panic',
      }));

      expect(updateQueryString).toHaveBeenCalledWith('queryId', 'something AND NOT do:panic');
    });
  });
});
