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

import useLocation from 'routing/useLocation';
import asMock from 'helpers/mocking/AsMock';
import Search from 'views/logic/search/Search';
import View from 'views/logic/views/View';
import useQuery from 'routing/useQuery';
import useProcessHooksForView from 'views/logic/views/UseProcessHooksForView';
import StreamsContext from 'contexts/StreamsContext';
import useLoadView from 'views/hooks/useLoadView';
import { createSearch } from 'fixtures/searches';
import useView from 'views/hooks/useView';
import ViewTransformer from 'views/logic/views/ViewTransformer';

import NewDashboardPage from './NewDashboardPage';

jest.mock('views/components/Search', () => () => <span>Extended search page</span>);

jest.mock('routing/useLocation');
jest.mock('routing/useQuery');
jest.mock('views/logic/views/UseProcessHooksForView');
jest.mock('views/hooks/useLoadView');
jest.mock('views/hooks/useView');
jest.mock('views/logic/views/ViewTransformer');

const SimpleNewDashboardPage = () => (
  <StreamsContext.Provider value={[{}]}>
    <NewDashboardPage />
  </StreamsContext.Provider>
);

describe('NewDashboardPage', () => {
  const mockLocation = {
    pathname: '',
    search: '',
    state: {},
    hash: '',
  };
  const defaultView = createSearch();

  beforeEach(() => {
    asMock(useLocation).mockReturnValue(mockLocation);
    asMock(useQuery).mockReturnValue({});
    asMock(useProcessHooksForView).mockReturnValue([true, undefined]);
    asMock(useView).mockReturnValue(defaultView);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('shows loading spinner before rendering page', async () => {
    asMock(useProcessHooksForView).mockReturnValue([false, undefined]);

    const { findByText } = render(<SimpleNewDashboardPage />);

    expect(await findByText('Loading...')).not.toBeNull();
  });

  it('should create new view with type dashboard on mount', async () => {
    render(<SimpleNewDashboardPage />);

    await waitFor(() => expect(useLoadView).toHaveBeenCalled());

    await expect(asMock(useLoadView).mock.calls[0][0]).resolves.toEqual(expect.objectContaining({ type: View.Type.Dashboard }));
  });

  it('should render transform search view to dashboard view, if view is defined in location state', async () => {
    const view = View.create().toBuilder()
      .type(View.Type.Search)
      .title('My Search')
      .search(Search.builder().build())
      .createdAt(new Date('2019-10-16T14:38:44.681Z'))
      .build();

    asMock(useLocation).mockReturnValue({ ...mockLocation, state: { view } });
    const dashboardView = view.toBuilder().newId().type(View.Type.Dashboard).build();
    asMock(ViewTransformer).mockReturnValue(dashboardView);
    asMock(useView).mockReturnValue(dashboardView);

    const { findByText } = render(<SimpleNewDashboardPage />);

    await findByText('Extended search page');

    await waitFor(() => expect(useLoadView).toHaveBeenCalled());

    await expect(asMock(useLoadView).mock.calls[0][0]).resolves.toEqual(expect.objectContaining({ title: 'My Search', type: View.Type.Dashboard }));
  });

  it('should process hooks with provided location query when transforming search view to dashboard view', async () => {
    const view = View.create().toBuilder().type(View.Type.Search).search(Search.builder().build())
      .createdAt(new Date('2019-10-16T14:38:44.681Z'))
      .build();

    const dashboardView = view.toBuilder().newId().type(View.Type.Dashboard).build();
    asMock(ViewTransformer).mockReturnValue(dashboardView);
    asMock(useView).mockReturnValue(dashboardView);

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

    expect(useProcessHooksForView).toHaveBeenCalledWith(expect.anything(), {
      q: '',
      rangetype: 'relative',
      relative: '300',
    });
  });
});
