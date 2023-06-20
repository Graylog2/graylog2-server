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

import View from 'views/logic/views/View';
import Query, { createElasticsearchQueryString } from 'views/logic/queries/Query';
import Search from 'views/logic/search/Search';
import mockDispatch from 'views/test/mockDispatch';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';

import bindSearchParamsFromQuery from './BindSearchParamsFromQuery';

const MOCK_VIEW_QUERY_ID = 'query-id';

jest.mock('views/logic/slices/createSearch', () => (s: Search) => s);

describe('BindSearchParamsFromQuery should', () => {
  const query = Query.builder()
    .id(MOCK_VIEW_QUERY_ID)
    .query(createElasticsearchQueryString(''))
    .build();
  const search = Search.create()
    .toBuilder()
    .queries([query])
    .build();
  const view = View.create()
    .toBuilder()
    .type(View.Type.Search)
    .search(search)
    .build();
  const dispatch = mockDispatch();
  const defaultInput = {
    query: {},
    view,
    executionState: SearchExecutionState.empty(),
    retry: () => Promise.resolve(),
    dispatch,
  };

  afterEach(() => {
    jest.clearAllMocks();
  });

  const findMockQuery = (v: View) => v.search.queries.find((q) => q.id === MOCK_VIEW_QUERY_ID);

  it('not update query when provided view is not a search', async () => {
    const input = {
      ...defaultInput,
      view: view.toBuilder().type(View.Type.Dashboard).build(),
    };

    const [newView] = await bindSearchParamsFromQuery(input);

    expect(findMockQuery(newView).query.query_string).toBe('');
  });

  it('not update query when query is already up to date', async () => {
    const [newView] = await bindSearchParamsFromQuery(defaultInput);

    expect(findMockQuery(newView).query.query_string).toBe('');
  });

  it('update query string with provided query param', async () => {
    const input = {
      ...defaultInput,
      query: { q: 'gl2_source_input:source-input-id' },
    };

    const [newView] = await bindSearchParamsFromQuery(input);

    expect(findMockQuery(newView).query.query_string).toBe('gl2_source_input:source-input-id');
  });

  it('not update query string when no query param is provided', async () => {
    const [newView] = await bindSearchParamsFromQuery(defaultInput);

    expect(findMockQuery(newView).query.query_string).toBe('');
  });

  it('update query timerange when relative range value param is povided', async () => {
    const input = {
      ...defaultInput,
      query: { relative: '0' },
    };
    const expectedTimerange = {
      type: 'relative',
      range: 0,
    };

    const [newView] = await bindSearchParamsFromQuery(input);

    expect(findMockQuery(newView).timerange).toEqual(expectedTimerange);
  });

  it('update query timerange when provided query range param is absolute', async () => {
    const input = {
      ...defaultInput,
      query: { rangetype: 'absolute', from: '2010-01-00 00:00:00', to: '2010-10-00 00:00:00' },
    };
    const expectedTimerange = {
      type: input.query.rangetype,
      from: input.query.from,
      to: input.query.to,
    };

    const [newView] = await bindSearchParamsFromQuery(input);

    expect(findMockQuery(newView).timerange).toEqual(expectedTimerange);
  });

  it('update query timerange when provided query range is keyword', async () => {
    const input = {
      ...defaultInput,
      query: { rangetype: 'keyword', keyword: 'Last five days' },
    };
    const expectedTimerange = {
      type: input.query.rangetype, keyword: input.query.keyword,
    };

    const [newView] = await bindSearchParamsFromQuery(input);

    expect(findMockQuery(newView).timerange).toEqual(expectedTimerange);
  });

  it('update streams of new search when comma-separated streams parameter was supplied', async () => {
    const input = {
      ...defaultInput,
      query: { streams: 'stream1, stream2,  stream3 ' },
    };

    const [newView] = await bindSearchParamsFromQuery(input);

    const expectedFilter = Immutable.Map({
      type: 'or',
      filters: Immutable.List([
        Immutable.Map({ type: 'stream', id: 'stream1' }),
        Immutable.Map({ type: 'stream', id: 'stream2' }),
        Immutable.Map({ type: 'stream', id: 'stream3' }),
      ]),
    });

    expect(findMockQuery(newView).filter).toEqual(expectedFilter);
  });

  it('do not update streams of new search when streams parameter was supplied but is empty', async () => {
    const input = {
      ...defaultInput,
      query: { streams: '' },
    };

    const [newView] = await bindSearchParamsFromQuery(input);

    expect(newView).toEqual(view);
  });
});
