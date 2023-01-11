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
import { updateQuery } from 'views/logic/slices/viewSlice';
import { updateGlobalOverride } from 'views/logic/slices/searchExecutionSlice';

import ExcludeFromQueryHandler from './ExcludeFromQueryHandler';

import GlobalOverride from '../search/GlobalOverride';
import Query from '../queries/Query';
import type { ViewType } from '../views/View';
import View from '../views/View';

const createQuery = (queryString: string) => Query.builder()
  .id('queryId')
  .query({ type: 'elasticsearch', query_string: queryString })
  .build();

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

  it('adds exclusion term to query', () => {
    const query = createQuery('');
    const view = createViewWithQuery(query);
    const state = { ...mockRootState, view: { view } } as RootState;
    const dispatch = mockDispatch(state);

    dispatch(ExcludeFromQueryHandler({
      queryId: 'queryId',
      field: 'something',
      value: 'other',
    }));

    expect(dispatch).toHaveBeenCalledWith(
      updateQuery(['queryId', query.toBuilder().query({ type: 'elasticsearch', query_string: 'NOT something:other' }).build()]),
    );
  });

  it('replaces `*` query completely', () => {
    const query = createQuery('*');
    const view = createViewWithQuery(query);
    const state = { ...mockRootState, view: { view } } as RootState;
    const dispatch = mockDispatch(state);

    dispatch(ExcludeFromQueryHandler({
      queryId: 'queryId',
      field: 'foo',
      value: 'bar',
    }));

    expect(dispatch).toHaveBeenCalledWith(
      updateQuery(['queryId', query.toBuilder().query({ type: 'elasticsearch', query_string: 'NOT foo:bar' }).build()]),
    );
  });

  it('appends negated term to existing query', () => {
    const query = createQuery('answer:42');
    const view = createViewWithQuery(query);
    const state = { ...mockRootState, view: { view } } as RootState;
    const dispatch = mockDispatch(state);

    dispatch(ExcludeFromQueryHandler({
      queryId: 'queryId',
      field: 'do',
      value: 'panic',
    }));

    expect(dispatch).toHaveBeenCalledWith(
      updateQuery(['queryId', query.toBuilder().query({ type: 'elasticsearch', query_string: 'answer:42 AND NOT do:panic' }).build()]),
    );
  });

  it('appends _exists_ fragment for proper field in case of missing bucket in input', () => {
    const query = createQuery('answer:42');
    const view = createViewWithQuery(query);
    const state = { ...mockRootState, view: { view } } as RootState;
    const dispatch = mockDispatch(state);

    dispatch(ExcludeFromQueryHandler({
      queryId: 'queryId',
      field: 'do',
      value: MISSING_BUCKET_NAME,
    }));

    expect(dispatch).toHaveBeenCalledWith(
      updateQuery(['queryId', query.toBuilder().query({ type: 'elasticsearch', query_string: 'answer:42 AND _exists_:do' }).build()]),
    );
  });

  it('escapes special characters in field value', () => {
    const query = createQuery('*');
    const view = createViewWithQuery(query);
    const state = { ...mockRootState, view: { view } } as RootState;
    const dispatch = mockDispatch(state);

    dispatch(ExcludeFromQueryHandler({
      queryId: 'queryId',
      field: 'something',
      value: 'foo && || : \\ / + - ! ( ) { } [ ] ^ " ~ * ? bar',
    }));

    expect(dispatch).toHaveBeenCalledWith(
      updateQuery(['queryId', query.toBuilder().query({ type: 'elasticsearch', query_string: 'NOT something:"foo && || : \\\\ / + - ! ( ) { } [ ] ^ \\" ~ * ? bar"' }).build()]),
    );
  });

  describe('for dashboards', () => {
    it('retrieves query string from global override', () => {
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

      dispatch(ExcludeFromQueryHandler({
        queryId: 'queryId',
        field: 'do',
        value: 'panic',
      }));

      expect(dispatch).toHaveBeenCalledWith(
        updateGlobalOverride(
          GlobalOverride.create(undefined, { type: 'elasticsearch', query_string: 'something AND NOT do:panic' }),
        ),
      );
    });
  });
});
