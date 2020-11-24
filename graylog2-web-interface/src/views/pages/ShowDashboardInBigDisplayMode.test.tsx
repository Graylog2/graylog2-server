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
import { StoreMock as MockStore } from 'helpers/mocking';

import { RefreshActions } from 'views/stores/RefreshStore';
import View from 'views/logic/views/View';
import _ShowDashboardInBigDisplayMode from 'views/pages/ShowDashboardInBigDisplayMode';

const mockView = View.builder()
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

jest.mock('views/stores/ViewStore', () => ({
  ViewStore: MockStore(
    ['listen', () => jest.fn()],
    ['getInitialState', () => ({ activeQuery: 'somequery', view: mockView })],
  ),
}));

jest.mock('views/stores/SearchExecutionStateStore', () => ({
  SearchExecutionStateStore: MockStore(
    ['listen', () => jest.fn()],
    ['getInitialState', () => ({ activeQuery: 'somequery', view: mockView })],
  ),
}));

jest.mock('views/pages/ShowViewPage', () => () => null);
jest.mock('routing/withLocation', () => (x) => x);
jest.mock('routing/withParams', () => (x) => x);

const mockLocation = {
  query: {
    interval: '30', refresh: '10',
  },
};

type SUTProps = {
  params: { viewId: string };
  location: typeof mockLocation;
};

const ShowDashboardInBigDisplayMode = _ShowDashboardInBigDisplayMode as React.ComponentType<SUTProps>;

describe('ShowDashboardInBigDisplayMode', () => {
  beforeEach(() => {
    RefreshActions.disable = jest.fn();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('set refresh interval correctly based on location query', async () => {
    render(<ShowDashboardInBigDisplayMode params={{ viewId: mockView.id }}
                                          location={mockLocation} />);

    await waitFor(() => expect(RefreshActions.setInterval).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(RefreshActions.setInterval).toHaveBeenCalledWith(10000));
  });

  it('enable refresh actions', async () => {
    render(<ShowDashboardInBigDisplayMode params={{ viewId: mockView.id }}
                                          location={mockLocation} />);

    await waitFor(() => expect(RefreshActions.enable).toHaveBeenCalledTimes(1));
  });

  it('set new refresh interval when location query refresh param changes', async () => {
    const { rerender } = render(<ShowDashboardInBigDisplayMode params={{ viewId: mockView.id }}
                                                               location={mockLocation} />);

    rerender(<ShowDashboardInBigDisplayMode params={{ viewId: mockView.id }}
                                            location={{ query: { ...mockLocation.query, refresh: '20' } }} />);

    await waitFor(() => expect(RefreshActions.setInterval).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(RefreshActions.setInterval).toHaveBeenCalledWith(20000));
  });

  it('not change RefreshActions when query refresh param did not changed', async () => {
    const { rerender } = render(<ShowDashboardInBigDisplayMode params={{ viewId: mockView.id }}
                                                               location={mockLocation} />);

    rerender(<ShowDashboardInBigDisplayMode params={{ viewId: mockView.id }}
                                            location={mockLocation} />);

    await waitFor(() => expect(RefreshActions.setInterval).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(RefreshActions.enable).toHaveBeenCalledTimes(1));
  });

  it('disable refresh actions on unmount', async () => {
    const { unmount } = render(<ShowDashboardInBigDisplayMode params={{ viewId: mockView.id }}
                                                              location={mockLocation} />);

    unmount();
    await waitFor(() => expect(RefreshActions.disable).toHaveBeenCalledTimes(1));
  });

  it('should display view title', async () => {
    const { findByText } = render(<ShowDashboardInBigDisplayMode params={{ viewId: mockView.id }}
                                                                 location={mockLocation} />);

    await findByText('view title');
  });
});
