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
import { render, waitFor } from 'wrappedTestingLibrary';
import type { Location } from 'react-router-dom';

import useLocation from 'routing/useLocation';
import asMock from 'helpers/mocking/AsMock';
import Search from 'views/logic/search/Search';
import View from 'views/logic/views/View';
import useQuery from 'routing/useQuery';
import useProcessHooksForView from 'views/logic/views/UseProcessHooksForView';
import StreamsContext from 'contexts/StreamsContext';
import { loadViewsPlugin, unloadViewsPlugin } from 'views/test/testViewsPlugin';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import useCreateSearch from 'views/hooks/useCreateSearch';

import NewDashboardPage from './NewDashboardPage';

jest.mock('views/components/Search', () => () => <span>Extended search page</span>);

jest.mock('routing/useQuery');
jest.mock('routing/useLocation');
jest.mock('views/logic/views/UseProcessHooksForView');
jest.mock('views/hooks/useCreateSearch');

const SimpleNewDashboardPage = () => (
  <StreamsContext.Provider value={[{ id: 'deadbeef', title: 'Teststream' }]}>
    <NewDashboardPage />
  </StreamsContext.Provider>
);

describe('NewDashboardPage', () => {
  const mockLocation = {
    pathname: '',
    search: '',
    state: {},
    hash: '',
  } as Location;

  beforeAll(loadViewsPlugin);

  afterAll(unloadViewsPlugin);

  beforeEach(() => {
    asMock(useLocation).mockReturnValue(mockLocation);
    asMock(useQuery).mockReturnValue({});
    asMock(useProcessHooksForView).mockReturnValue({ status: 'loaded', view: View.create(), executionState: SearchExecutionState.empty() });
    asMock(useCreateSearch).mockImplementation(async (view: Promise<View>) => view);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('shows loading spinner before rendering page', async () => {
    asMock(useProcessHooksForView).mockReturnValue({ status: 'loading' });

    const { findByText } = render(<SimpleNewDashboardPage />);

    expect(await findByText('Loading...')).not.toBeNull();
  });

  it('should create new view with type dashboard on mount', async () => {
    render(<SimpleNewDashboardPage />);

    await waitFor(() => expect(useCreateSearch).toHaveBeenCalled());

    await expect(asMock(useCreateSearch).mock.calls[0][0]).resolves.toEqual(expect.objectContaining({ type: View.Type.Dashboard }));
  });

  it('should render transform search view to dashboard view, if view is defined in location state', async () => {
    const view = View.create().toBuilder()
      .type(View.Type.Search)
      .title('My Search')
      .search(Search.builder().build())
      .createdAt(new Date('2019-10-16T14:38:44.681Z'))
      .build();

    asMock(useLocation).mockReturnValue({ ...mockLocation, state: { view } });

    const { findByText } = render(<SimpleNewDashboardPage />);

    await findByText('Extended search page');

    await waitFor(() => expect(useCreateSearch).toHaveBeenCalled());

    await expect(asMock(useCreateSearch).mock.calls[0][0]).resolves.toEqual(expect.objectContaining({ title: 'My Search', type: View.Type.Dashboard }));
  });

  it('should process hooks with provided location query when transforming search view to dashboard view', async () => {
    const view = View.create().toBuilder().type(View.Type.Search).search(Search.builder().build())
      .createdAt(new Date('2019-10-16T14:38:44.681Z'))
      .build();

    asMock(useLocation).mockReturnValue({ ...mockLocation, state: { view } });

    asMock(useQuery).mockReturnValue({
      q: '',
      rangetype: 'relative',
      relative: '300',
    });

    const { findByText } = render((
      <SimpleNewDashboardPage />
    ));

    await findByText('Extended search page');

    expect(useProcessHooksForView).toHaveBeenCalled();

    expect(useProcessHooksForView).toHaveBeenCalledWith(expect.anything(), SearchExecutionState.empty(), {
      q: '',
      rangetype: 'relative',
      relative: '300',
    });
  });
});
