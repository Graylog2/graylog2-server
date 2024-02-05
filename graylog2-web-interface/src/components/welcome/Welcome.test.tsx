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
import { defaultUser } from 'defaultMockValues';

import Welcome from 'components/welcome/Welcome';
import { asMock } from 'helpers/mocking';
import useLastOpened from 'components/welcome/hooks/useLastOpened';
import useFavoriteItems from 'components/welcome/hooks/useFavoriteItems';
import useRecentActivity from 'components/welcome/hooks/useRecentActivity';
import useCurrentUser from 'hooks/useCurrentUser';
import { carol, sam } from 'fixtures/users';

jest.mock('components/content-stream/ContentStreamSection', () => 'content-stream-section');

jest.mock('components/welcome/hooks/useLastOpened', () => jest.fn(() => ({
  data: {
    lastOpened: [{ grn: 'grn::::dashboard:1', title: 'Title 1' }, {
      grn: 'grn::::search:2',
      title: 'Title 2',
    }],
    per_page: 5,
    page: 1,
    count: 0,
    total: 0,
  },
  isFetching: false,
})));

jest.mock('components/welcome/hooks/useFavoriteItems', () => jest.fn(() => ({
  data: {
    favorites: [{ grn: 'grn::::dashboard:4', title: 'Title 4' }, {
      grn: 'grn::::dashboard:3',
      title: 'Title 3',
    }],
    per_page: 5,
    page: 1,
    count: 0,
    total: 0,
  },
  isFetching: false,
})));

jest.mock('components/welcome/hooks/useRecentActivity', () => jest.fn(() => ({
  data: {
    recentActivity: [
      {
        id: '5',
        activityType: 'share',
        itemGrn: 'grn::::dashboard:5',
        itemTitle: 'Title 5',
        timestamp: '2022-01-01',
      },
      {
        id: '6',
        activityType: 'deleted',
        itemGrn: 'grn::::search:6',
        itemTitle: 'Title 6',
        timestamp: '2022-01-03',
      },
    ],
    per_page: 5,
    page: 1,
    count: 0,
    total: 0,
  },
  isFetching: false,
})));

jest.mock('routing/Routes', () => ({
  pluginRoute: (x) => x,
  dashboard_show: (x) => `/route/DASHBOARDS_VIEWID/${x}`,
  getPluginRoute: (x) => () => x,
  SEARCH: '/search',
  SYSTEM: {
    USERS: {
      edit: () => '/edit-profile-page',
    },
  },
}));

jest.mock('hooks/useCurrentUser');

describe('Welcome', () => {
  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(defaultUser);
  });

  describe('Page header', () => {
    it('Shows link to edit profile in case user is not readOnly and has no defined starting page', async () => {
      render(<Welcome />);

      await screen.findByText('This is your personal start page, allowing easy access to the content most relevant for you.');
      const linkToEditProfile = await screen.findByText('edit profile');

      expect(linkToEditProfile).toHaveAttribute('href', '/edit-profile-page');
    });

    it('Shows appropriate message without link for readOnly users', async () => {
      asMock(useCurrentUser).mockReturnValue(sam);
      render(<Welcome />);
      await screen.findByText('This is your personal page, allowing easy access to the content most relevant for you.');
    });

    it('Shows appropriate message without link for users with defined starting page', async () => {
      asMock(useCurrentUser).mockReturnValue(carol);
      render(<Welcome />);
      await screen.findByText('This is your personal page, allowing easy access to the content most relevant for you.');
    });
  });

  describe('Last opened list', () => {
    it('Show items', async () => {
      render(<Welcome />);

      await screen.findByRole('link', {
        name: /Title 1/i,
      });

      await screen.findByRole('link', {
        name: /Title 2/i,
      });
    });

    it('Show no items', async () => {
      asMock(useLastOpened).mockImplementation(() => ({
        data: { lastOpened: [], page: 1, count: 0, total: 0, per_page: 5 },
        isFetching: false,
      }));

      render(<Welcome />);
      await screen.findByText(/You have not opened any searches\/dashboards yet/i);
    });
  });

  describe('Favorite items list', () => {
    it('Show items', async () => {
      render(<Welcome />);

      await screen.findByRole('link', {
        name: /Title 3/i,
      });

      await screen.findByRole('link', {
        name: /Title 4/i,
      });
    });

    it('Show no items', async () => {
      asMock(useFavoriteItems).mockImplementation(() => ({
        data: {
          favorites: [],
          page: 1,
          count: 0,
          total: 0,
          per_page: 5,
        },
        isFetching: false,
      }));

      render(<Welcome />);
      await screen.findByText(/You do not have any favorite items yet./i);
    });
  });

  describe('Recent activity list', () => {
    it('Show items', async () => {
      render(<Welcome />);

      await screen.findByRole('link', {
        name: /Title 5/i,
      });

      await screen.findByRole('link', {
        name: /Title 6/i,
      });
    });

    it('Show no items', async () => {
      asMock(useRecentActivity).mockImplementation(() => ({
        data: {
          recentActivity: [],
          page: 1,
          count: 0,
          total: 0,
          per_page: 5,
        },
        isFetching: false,
      }));

      render(<Welcome />);
      await screen.findByText(/There is no recent activity/i);
    });
  });
});
