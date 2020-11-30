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
// @flow strict
import * as Immutable from 'immutable';

import View from 'views/logic/views/View';
import Query from 'views/logic/queries/Query';
import Search from 'views/logic/search/Search';
import { QueriesActions } from 'views/stores/QueriesStore';

import bindSearchParamsFromQuery from './BindSearchParamsFromQuery';

const MOCK_VIEW_QUERY_ID = 'query-id';

jest.mock('views/stores/QueriesStore', () => ({
  QueriesActions: {
    update: jest.fn(() => Promise.resolve()),
  },
}));

describe('BindSearchParamsFromQuery should', () => {
  const query = Query.builder().id(MOCK_VIEW_QUERY_ID).build();
  const search = Search.create()
    .toBuilder()
    .queries([query])
    .build();
  const view = View.create()
    .toBuilder()
    .type(View.Type.Search)
    .search(search)
    .build();
  const defaultInput = {
    query: {},
    view,
    retry: () => Promise.resolve(),
  };

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('not update query when provided view is not a search', async () => {
    const input = {
      ...defaultInput,
      view: view.toBuilder().type(View.Type.Dashboard).build(),
    };

    await bindSearchParamsFromQuery(input);

    expect(QueriesActions.update).not.toHaveBeenCalled();
  });

  it('update query string with provided query param', async () => {
    const input = {
      ...defaultInput,
      query: { q: 'gl2_source_input:source-input-id' },
    };

    await bindSearchParamsFromQuery(input);

    expect(QueriesActions.update)
      .toHaveBeenCalledWith(
        MOCK_VIEW_QUERY_ID,
        expect.objectContaining({ query: { query_string: 'gl2_source_input:source-input-id', type: 'elasticsearch' } }),
      );
  });

  it('not update query string when no query param is provided', async () => {
    await bindSearchParamsFromQuery(defaultInput);

    expect(QueriesActions.update).not.toHaveBeenCalled();
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

    await bindSearchParamsFromQuery(input);

    expect(QueriesActions.update)
      .toHaveBeenCalledWith(
        MOCK_VIEW_QUERY_ID,
        expect.objectContaining({ timerange: expectedTimerange }),
      );
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

    await bindSearchParamsFromQuery(input);

    expect(QueriesActions.update)
      .toHaveBeenCalledWith(
        MOCK_VIEW_QUERY_ID,
        expect.objectContaining({ timerange: expectedTimerange }),
      );
  });

  it('update query timerange when provided query range is keyword', async () => {
    const input = {
      ...defaultInput,
      query: { rangetype: 'keyword', keyword: 'Last five days' },
    };
    const expectedTimerange = {
      type: input.query.rangetype, keyword: input.query.keyword,
    };

    await bindSearchParamsFromQuery(input);

    expect(QueriesActions.update)
      .toHaveBeenCalledWith(
        MOCK_VIEW_QUERY_ID,
        expect.objectContaining({ timerange: expectedTimerange }),
      );
  });

  it('update streams of new search when comma-separated streams parameter was supplied', async () => {
    const input = {
      ...defaultInput,
      query: { streams: 'stream1, stream2,  stream3 ' },
    };

    await bindSearchParamsFromQuery(input);

    const expectedFilter = Immutable.Map({
      type: 'or',
      filters: [
        Immutable.Map({ type: 'stream', id: 'stream1' }),
        Immutable.Map({ type: 'stream', id: 'stream2' }),
        Immutable.Map({ type: 'stream', id: 'stream3' }),
      ],
    });

    expect(QueriesActions.update)
      .toHaveBeenCalledWith(
        MOCK_VIEW_QUERY_ID,
        expect.objectContaining({ filter: expectedFilter }),
      );
  });

  it('do not update streams of new search when streams parameter was supplied but is empty', async () => {
    const input = {
      ...defaultInput,
      query: { streams: '' },
    };

    await bindSearchParamsFromQuery(input);

    expect(QueriesActions.update)
      .not.toHaveBeenCalled();
  });
});
