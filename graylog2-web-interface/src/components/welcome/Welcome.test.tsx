import React from 'react';
import { render, screen } from 'wrappedTestingLibrary';

import Welcome from 'components/welcome/Welcome';
import { asMock } from 'helpers/mocking';
import { useLastOpened, usePinnedItems, useRecentActivity } from 'components/welcome/hooks';

jest.mock('routing/Routes', () => ({ pluginRoute: () => () => '/route' }));

jest.mock('components/welcome/hooks', () => ({
  useLastOpened: jest.fn(() => ({
    data: {
      lastOpened: [{ id: '1', title: 'Title 1', type: 'dashboard' }, {
        id: '2',
        title: 'Title 2',
        type: 'search',
      }],
      per_page: 5,
      page: 1,
      count: 0,
      total: 0,
    },
    isFetching: false,
  })),
  usePinnedItems: jest.fn(() => ({
    data: {
      pinnedItems: [{ id: '4', title: 'Title 4', type: 'dashboard' }, {
        id: '3',
        title: 'Title 3',
        type: 'dashboard',
      }],
      per_page: 5,
      page: 1,
      count: 0,
      total: 0,
    },
    isFetching: false,
  })),
  useRecentActivity: jest.fn(() => ({
    data: {
      recentActivity: [
        {
          id: '5',
          activityType: 'share',
          itemType: 'dashboard',
          itemId: '5',
          title: 'Title 5',
          timestamp: '2022-01-01',
        },
        {
          id: '6',
          activityType: 'deleted',
          itemType: 'search',
          itemId: '6',
          title: 'Title 6',
          timestamp: '2022-01-03',
        },
      ],
      per_page: 5,
      page: 1,
      count: 0,
      total: 0,
    },
    isFetching: false,
  })),
}));

describe('Welcome', () => {
  describe('Last opened list', () => {
    it('Show items', async () => {
      render(<Welcome />);
      const list = await screen.findByTestId('last-opened-list');

      expect(list).toHaveTextContent('Title 1');
      expect(list).toHaveTextContent('Title 2');
    });

    it('Show no items', async () => {
      asMock(useLastOpened).mockImplementation(() => ({ data: { lastOpened: [], page: 1, count: 0, total: 0, per_page: 5 }, isFetching: false }));
      render(<Welcome />);
      await screen.findByText('There are no last opened items');
    });
  });

  describe('Pinned items list', () => {
    it('Show items', async () => {
      render(<Welcome />);
      const list = await screen.findByTestId('pinned-items-list');

      expect(list).toHaveTextContent('Title 3');
      expect(list).toHaveTextContent('Title 4');
    });

    it('Show no items', async () => {
      asMock(usePinnedItems).mockImplementation(() => ({ data: { pinnedItems: [], page: 1, count: 0, total: 0, per_page: 5 }, isFetching: false }));
      render(<Welcome />);
      await screen.findByText('There are no pinned items');
    });
  });

  describe('Recent activity list', () => {
    it('Show items', async () => {
      render(<Welcome />);
      const list = await screen.findByTestId('recent-activity-table');

      expect(list).toHaveTextContent('Title 5');
      expect(list).toHaveTextContent('Title 6');
    });

    it('Show no items', async () => {
      asMock(useRecentActivity).mockImplementation(() => ({ data: { recentActivity: [], page: 1, count: 0, total: 0, per_page: 5 }, isFetching: false }));
      render(<Welcome />);
      await screen.findByText('There is no recent activity');
    });
  });
});
