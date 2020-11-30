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
import * as React from 'react';
import { render, waitFor } from 'wrappedTestingLibrary';
import asMock from 'helpers/mocking/AsMock';
import { act } from 'react-dom/test-utils';

import { processHooks } from 'views/logic/views/ViewLoader';
import { ViewActions } from 'views/stores/ViewStore';
import Search from 'views/logic/search/Search';
import View from 'views/logic/views/View';

import NewDashboardPage from './NewDashboardPage';

jest.mock('views/pages/SearchPage', () => () => <div>Extended search page</div>);

jest.mock('components/common', () => ({
  IfPermitted: jest.fn(({ children }) => <>{children}</>),
}));

jest.mock('views/stores/ViewStore', () => ({
  ViewActions: {
    create: jest.fn(() => Promise.resolve({ view: undefined })),
    load: jest.fn(() => Promise.resolve()),
  },
}));

jest.mock('views/logic/views/ViewLoader', () => ({
  processHooks: jest.fn((promise, loadHooks, executeHooks, query, onSuccess) => Promise.resolve().then(onSuccess)),
}));

jest.mock('routing/withLocation', () => (x) => x);

describe('NewDashboardPage', () => {
  const SimpleNewDashboardPage = (props) => <NewDashboardPage location={{}} {...props} />;

  beforeAll(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('shows loading spinner before rendering page', async () => {
    const { findByText, getByText } = render(<SimpleNewDashboardPage />);

    act(() => jest.advanceTimersByTime(200));

    expect(getByText('Loading...')).not.toBeNull();

    await findByText('Extended search page');
  });

  it('should create new view with type dashboard on mount', async () => {
    render(<SimpleNewDashboardPage />);

    await waitFor(() => expect(ViewActions.create).toBeCalledTimes(1));
    await waitFor(() => expect(ViewActions.create).toHaveBeenCalledWith(View.Type.Dashboard));
  });

  it('should render transform search view to dashboard view, if view is defined in location state', async () => {
    const loadViewMock = asMock(ViewActions.load);
    const view = View.create().toBuilder().type(View.Type.Search).search(Search.builder().build())
      .createdAt(new Date('2019-10-16T14:38:44.681Z'))
      .build();

    const { findByText } = render((
      <SimpleNewDashboardPage location={{ state: { view } }} />
    ));

    await findByText('Extended search page');

    expect(loadViewMock).toHaveBeenCalled();
    expect(loadViewMock).toHaveBeenCalledWith(expect.objectContaining({ type: View.Type.Dashboard }), expect.anything());
  });

  it('should process hooks with provided location query when transforming search view to dashboard view', async () => {
    const processHooksAction = asMock(processHooks);
    const view = View.create().toBuilder().type(View.Type.Search).search(Search.builder().build())
      .createdAt(new Date('2019-10-16T14:38:44.681Z'))
      .build();

    const { findByText } = render((
      <SimpleNewDashboardPage location={{
        state: { view },
        query: {
          q: '',
          rangetype: 'relative',
          relative: '300',
        },
      }} />
    ));

    await findByText('Extended search page');

    expect(processHooksAction).toHaveBeenCalled();
    expect(processHooksAction).toHaveBeenCalledWith(expect.anything(), expect.anything(), expect.anything(), { q: '', rangetype: 'relative', relative: '300' }, expect.anything());
  });

  it('should not render transform search view to dashboard view if view search is in JSON format', async () => {
    const view = View.create().toBuilder().type(View.Type.Search).search(Search.builder().build())
      .createdAt(new Date('2019-10-16T14:38:44.681Z'))
      .build()
      .toJSON();

    const { findByText } = render((
      <SimpleNewDashboardPage location={{ state: { view } }} />
    ));

    await findByText('Extended search page');

    expect(ViewActions.load).toHaveBeenCalledTimes(0);
  });
});
