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

import { RefreshActions } from 'views/stores/RefreshStore';
import View from 'views/logic/views/View';
import OriginalShowDashboardInBigDisplayMode from 'views/pages/ShowDashboardInBigDisplayMode';
import TestStoreProvider from 'views/test/TestStoreProvider';
import { loadViewsPlugin, unloadViewsPlugin } from 'views/test/testViewsPlugin';
import mockAction from 'helpers/mocking/MockAction';
import { createSearch } from 'fixtures/searches';

const mockView = createSearch({ queryId: 'somequery' }).toBuilder()
  .type(View.Type.Dashboard)
  .id('view-id')
  .title('view title')
  .build();

jest.mock('views/stores/RefreshStore', () => ({
  RefreshActions: {
    setInterval: jest.fn(),
    enable: jest.fn(),
    disable: jest.fn(),
  },
}));

jest.mock('views/pages/ShowViewPage', () => ({ children }: React.PropsWithChildren<{}>) => children);
jest.mock('routing/withLocation', () => (x) => x);
jest.mock('routing/withParams', () => (x) => x);

const mockLocation = {
  query: {
    interval: '30', refresh: '10',
  },
};

type SUTProps = {
  location: typeof mockLocation;
};

const ShowDashboardInBigDisplayMode = (props: SUTProps) => (
  <TestStoreProvider view={mockView} initialQuery="somequery">
    <OriginalShowDashboardInBigDisplayMode {...props} />
  </TestStoreProvider>
);

describe('ShowDashboardInBigDisplayMode', () => {
  beforeAll(loadViewsPlugin);

  afterAll(unloadViewsPlugin);

  beforeEach(() => {
    jest.clearAllMocks();
    RefreshActions.disable = mockAction(jest.fn());
  });

  it('set refresh interval correctly based on location query', async () => {
    render(<ShowDashboardInBigDisplayMode location={mockLocation} />);

    await waitFor(() => expect(RefreshActions.setInterval).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(RefreshActions.setInterval).toHaveBeenCalledWith(10000));
  });

  it('enable refresh actions', async () => {
    render(<ShowDashboardInBigDisplayMode location={mockLocation} />);

    await waitFor(() => expect(RefreshActions.enable).toHaveBeenCalledTimes(1));
  });

  it('set new refresh interval when location query refresh param changes', async () => {
    const { rerender } = render(<ShowDashboardInBigDisplayMode location={mockLocation} />);

    rerender(<ShowDashboardInBigDisplayMode location={{ query: { ...mockLocation.query, refresh: '20' } }} />);

    await waitFor(() => expect(RefreshActions.setInterval).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(RefreshActions.setInterval).toHaveBeenCalledWith(20000));
  });

  it('not change RefreshActions when query refresh param did not changed', async () => {
    const { rerender } = render(<ShowDashboardInBigDisplayMode location={mockLocation} />);

    rerender(<ShowDashboardInBigDisplayMode location={mockLocation} />);

    await waitFor(() => expect(RefreshActions.setInterval).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(RefreshActions.enable).toHaveBeenCalledTimes(1));
  });

  it('disable refresh actions on unmount', async () => {
    const { unmount } = render(<ShowDashboardInBigDisplayMode location={mockLocation} />);

    unmount();
    await waitFor(() => expect(RefreshActions.disable).toHaveBeenCalledTimes(1));
  });

  it('should display view title', async () => {
    const { findByText } = render(<ShowDashboardInBigDisplayMode location={mockLocation} />);

    await findByText('view title');
  });
});
