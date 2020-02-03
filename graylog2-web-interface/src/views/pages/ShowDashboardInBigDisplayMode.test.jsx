// @flow strict
import * as React from 'react';
import { render, cleanup, wait, waitForElement } from 'wrappedTestingLibrary';

import { StoreMock as MockStore } from 'helpers/mocking';
import { RefreshActions } from 'views/stores/RefreshStore';
import View from 'views/logic/views/View';

import ShowDashboardInBigDisplayMode from 'views/pages/ShowDashboardInBigDisplayMode';

const mockView = View.builder()
  .type(View.Type.Dashboard)
  .id('view-id')
  .title('view title')
  .build();


jest.mock('react-router', () => ({ withRouter: x => x }));
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

describe('ShowDashboardInBigDisplayMode should', () => {
  const mockLocation = {
    query: {
      interval: '30', refresh: '10',
    },
  };

  afterEach(() => {
    cleanup();
    jest.clearAllMocks();
  });

  it('set refresh interval correctly based on location query', async () => {
    render(<ShowDashboardInBigDisplayMode route={{}}
                                          params={{ viewId: mockView.id }}
                                          location={mockLocation} />);
    await wait(() => expect(RefreshActions.setInterval).toHaveBeenCalledTimes(1));
    await wait(() => expect(RefreshActions.setInterval).toHaveBeenCalledWith(10000));
  });

  it('enable refresh actions', async () => {
    render(<ShowDashboardInBigDisplayMode route={{}}
                                          params={{ viewId: mockView.id }}
                                          location={mockLocation} />);
    await wait(() => expect(RefreshActions.enable).toHaveBeenCalledTimes(1));
  });

  it('set new refresh interval when location query refresh param changes', async () => {
    const { rerender } = render(<ShowDashboardInBigDisplayMode route={{}}
                                                               params={{ viewId: mockView.id }}
                                                               location={mockLocation} />);
    rerender(<ShowDashboardInBigDisplayMode route={{}}
                                            params={{ viewId: mockView.id }}
                                            location={{ query: { ...mockLocation.query, refresh: '20' } }} />);

    await wait(() => expect(RefreshActions.setInterval).toHaveBeenCalledTimes(2));
    await wait(() => expect(RefreshActions.setInterval).toHaveBeenCalledWith(20000));
  });

  it('not change RefreshActions when query refresh param did not changed', async () => {
    const { rerender } = render(<ShowDashboardInBigDisplayMode route={{}}
                                                               params={{ viewId: mockView.id }}
                                                               location={mockLocation} />);
    rerender(<ShowDashboardInBigDisplayMode route={{}}
                                            params={{ viewId: mockView.id }}
                                            location={mockLocation} />);

    await wait(() => expect(RefreshActions.setInterval).toHaveBeenCalledTimes(1));
    await wait(() => expect(RefreshActions.enable).toHaveBeenCalledTimes(1));
  });

  it('disable refresh actions on unmount', async () => {
    const { unmount } = render(<ShowDashboardInBigDisplayMode route={{}}
                                                              params={{ viewId: mockView.id }}
                                                              location={mockLocation} />);
    unmount();
    await wait(() => expect(RefreshActions.disable).toHaveBeenCalledTimes(1));
  });

  it('should display view title', async () => {
    const { getByText } = render(<ShowDashboardInBigDisplayMode route={{}}
                                                                params={{ viewId: mockView.id }}
                                                                location={mockLocation} />);

    await waitForElement(() => getByText('view title'));
  });
});
