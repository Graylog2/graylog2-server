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
import GlobalOverride from 'views/logic/search/GlobalOverride';

import bindSearchParamsFromQuery from './BindSearchParamsFromQuery';

const MOCK_VIEW_QUERY_ID = 'query-id';

jest.mock('views/logic/slices/createSearch', () => (s: Search) => s);

describe('BindSearchParamsFromQuery should', () => {
  const query = Query.builder().id(MOCK_VIEW_QUERY_ID).query(createElasticsearchQueryString('')).build();
  const search = Search.create().toBuilder().queries([query]).build();
  const view = View.create().toBuilder().type(View.Type.Search).search(search).build();
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

  it('update dashboard globalOverride.query when q param is provided', async () => {
    const input = {
      ...defaultInput,
      view: view.toBuilder().type(View.Type.Dashboard).build(),
      query: { q: 'source:nginx' },
    };

    const [, newExecutionState] = await bindSearchParamsFromQuery(input);

    expect(newExecutionState.globalOverride?.query?.query_string).toBe('source:nginx');
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
    expect(newView.search.id).not.toBe(view.search.id);
  });

  it('apply URL query override without mutating the original saved query', async () => {
    const savedQuery = Query.builder()
      .id(MOCK_VIEW_QUERY_ID)
      .query(createElasticsearchQueryString('persisted:query'))
      .build();
    const savedSearch = Search.create().toBuilder().queries([savedQuery]).build();
    const savedView = view.toBuilder().search(savedSearch).build();
    const input = {
      ...defaultInput,
      view: savedView,
      query: { q: 'override:query' },
    };

    const [newView] = await bindSearchParamsFromQuery(input);

    expect(findMockQuery(newView).query.query_string).toBe('override:query');
    expect(findMockQuery(savedView).query.query_string).toBe('persisted:query');
    expect(newView.search.id).not.toBe(savedView.search.id);
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
      type: input.query.rangetype,
      keyword: input.query.keyword,
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

  it('not update executionState when dashboard receives no URL params', async () => {
    const input = {
      ...defaultInput,
      view: view.toBuilder().type(View.Type.Dashboard).build(),
    };

    const [, newExecutionState] = await bindSearchParamsFromQuery(input);

    expect(newExecutionState).toBe(defaultInput.executionState);
  });

  it('update dashboard globalOverride.timerange when relative range param is provided', async () => {
    const input = {
      ...defaultInput,
      view: view.toBuilder().type(View.Type.Dashboard).build(),
      query: { relative: '3600' },
    };

    const [, newExecutionState] = await bindSearchParamsFromQuery(input);

    expect(newExecutionState.globalOverride?.timerange).toEqual({ type: 'relative', range: 3600 });
  });

  it('update dashboard globalOverride.timerange when absolute range params are provided', async () => {
    const input = {
      ...defaultInput,
      view: view.toBuilder().type(View.Type.Dashboard).build(),
      query: { rangetype: 'absolute', from: '2024-01-01T00:00:00.000Z', to: '2024-01-02T00:00:00.000Z' },
    };

    const [, newExecutionState] = await bindSearchParamsFromQuery(input);

    expect(newExecutionState.globalOverride?.timerange).toEqual({
      type: 'absolute',
      from: '2024-01-01T00:00:00.000Z',
      to: '2024-01-02T00:00:00.000Z',
    });
  });

  it('update dashboard globalOverride.timerange when keyword range param is provided', async () => {
    const input = {
      ...defaultInput,
      view: view.toBuilder().type(View.Type.Dashboard).build(),
      query: { rangetype: 'keyword', keyword: 'Last five days' },
    };

    const [, newExecutionState] = await bindSearchParamsFromQuery(input);

    expect(newExecutionState.globalOverride?.timerange).toEqual({ type: 'keyword', keyword: 'Last five days' });
  });

  it('preserve existing override timerange when only q param is provided on dashboard', async () => {
    const existingOverride = GlobalOverride.create({ type: 'relative', range: 300 });
    const existingExecutionState = defaultInput.executionState.toBuilder().globalOverride(existingOverride).build();
    const input = {
      ...defaultInput,
      view: view.toBuilder().type(View.Type.Dashboard).build(),
      executionState: existingExecutionState,
      query: { q: 'source:nginx' },
    };

    const [, newExecutionState] = await bindSearchParamsFromQuery(input);

    expect(newExecutionState.globalOverride?.query?.query_string).toBe('source:nginx');
    expect(newExecutionState.globalOverride?.timerange).toEqual({ type: 'relative', range: 300 });
  });

  it('preserve existing override query when only timerange param is provided on dashboard', async () => {
    const existingOverride = GlobalOverride.builder()
      .query({ type: 'elasticsearch', query_string: 'persisted:override' })
      .build();
    const existingExecutionState = defaultInput.executionState.toBuilder().globalOverride(existingOverride).build();
    const input = {
      ...defaultInput,
      view: view.toBuilder().type(View.Type.Dashboard).build(),
      executionState: existingExecutionState,
      query: { relative: '900' },
    };

    const [, newExecutionState] = await bindSearchParamsFromQuery(input);

    expect(newExecutionState.globalOverride?.query?.query_string).toBe('persisted:override');
    expect(newExecutionState.globalOverride?.timerange).toEqual({ type: 'relative', range: 900 });
  });

  it('ignore streams param on dashboard view', async () => {
    const input = {
      ...defaultInput,
      view: view.toBuilder().type(View.Type.Dashboard).build(),
      query: { streams: 'stream1,stream2' },
    };

    const [, newExecutionState] = await bindSearchParamsFromQuery(input);

    expect(newExecutionState).toBe(defaultInput.executionState);
  });
});
