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
import { List } from 'immutable';
import { render, screen } from 'wrappedTestingLibrary';

import useParams from 'routing/useParams';
import asMock from 'helpers/mocking/AsMock';
import StreamsContext from 'contexts/StreamsContext';
import useQuery from 'routing/useQuery';
import useFetchView from 'views/hooks/useFetchView';
import View from 'views/logic/views/View';
import Search from 'views/logic/search/Search';
import useProcessHooksForView from 'views/logic/views/UseProcessHooksForView';
import { loadViewsPlugin, unloadViewsPlugin } from 'views/test/testViewsPlugin';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';

import ShowViewPage from './ShowViewPage';

jest.mock('actions/errors/ErrorsActions', () => ({
  report: jest.fn(),
}));

jest.mock('views/components/Search', () => 'extended-search-page');
jest.mock('views/hooks/useFetchView');
jest.mock('views/hooks/useCreateSearch');
jest.mock('views/logic/views/UseProcessHooksForView');

jest.mock('routing/useParams');
jest.mock('routing/useQuery');

describe('ShowViewPage', () => {
  const view = View.fromJSON({
    id: 'foo',
    type: 'DASHBOARD',
    title: 'Foo',
    summary: 'summary',
    description: 'Foo',
    search_id: 'foosearch',
    properties: List<any>(),
    state: {},
    created_at: '2022-01-01 00:00:00',
    owner: 'admin',
    requires: {},
    favorite: false,
  }).toBuilder()
    .search(Search.create().toBuilder().parameters([]).build())
    .build();
  const SimpleShowViewPage = () => (
    <StreamsContext.Provider value={[{ id: 'stream-id-1', title: 'Stream 1' }]}>
      <ShowViewPage />
    </StreamsContext.Provider>
  );

  beforeAll(loadViewsPlugin);

  afterAll(unloadViewsPlugin);

  beforeEach(() => {
    asMock(useQuery).mockReturnValue({});
    asMock(useParams).mockReturnValue({ viewId: 'foo' });
    asMock(useProcessHooksForView).mockReturnValue({ status: 'loaded', view, executionState: SearchExecutionState.empty() });
    asMock(useFetchView).mockResolvedValue(view);
  });

  it('renders Spinner while loading', async () => {
    asMock(useProcessHooksForView).mockReturnValue({ status: 'loading' });

    render(<SimpleShowViewPage />);

    await screen.findByText('Loading...');
  });

  it('loads view with id passed from props', () => {
    render(<SimpleShowViewPage />);

    expect(useFetchView).toHaveBeenCalledWith('foo');
  });

  it('fetches views again if view id prop changes', () => {
    const { rerender } = render(<SimpleShowViewPage />);

    expect(useFetchView).toHaveBeenCalledWith('foo');

    asMock(useParams).mockReturnValue({ viewId: 'bar' });

    rerender(<SimpleShowViewPage />);

    expect(useFetchView).toHaveBeenCalledWith('bar');
  });
});
