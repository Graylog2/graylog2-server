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

import React from 'react';
import { render, screen } from 'wrappedTestingLibrary';

import Welcome from 'components/welcome/Welcome';
import { asMock } from 'helpers/mocking';
import { useLastOpened, useFavoriteItems, useRecentActivity } from 'components/welcome/hooks';

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
  useFavoriteItems: jest.fn(() => ({
    data: {
      favoriteItems: [{ id: '4', title: 'Title 4', type: 'dashboard' }, {
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

  describe('Favorite items list', () => {
    it('Show items', async () => {
      render(<Welcome />);
      const list = await screen.findByTestId('favorite-items-list');

      expect(list).toHaveTextContent('Title 3');
      expect(list).toHaveTextContent('Title 4');
    });

    it('Show no items', async () => {
      asMock(useFavoriteItems).mockImplementation(() => ({ data: { favoriteItems: [], page: 1, count: 0, total: 0, per_page: 5 }, isFetching: false }));
      render(<Welcome />);
      await screen.findByText('There are no favorite items');
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
