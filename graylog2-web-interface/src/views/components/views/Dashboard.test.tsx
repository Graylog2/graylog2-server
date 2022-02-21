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

import View from 'views/logic/views/View';
import Search from 'views/logic/search/Search';
import { StoreMock as MockStore } from 'helpers/mocking';
import CurrentUserProvider from 'contexts/CurrentUserProvider';

import Dashboard from './Dashboard';

jest.mock('stores/users/CurrentUserStore', () => ({
  CurrentUserStore: MockStore(
    'get',
    ['getInitialState', () => ({
      currentUser: {
        id: 'user-betty-id',
        full_name: 'Betty Holberton',
        username: 'betty',
        permissions: ['*'],
      },
    })],
  ),
}));

jest.mock('routing/Routes', () => ({ pluginRoute: () => () => '/route' }));
jest.mock('hooks/useUserDateTime');

const createPaginatedDashboards = () => {
  const simpleView = (): View => View.builder()
    .type('DASHBOARD')
    .id('search-id-0')
    .title('search-title-0')
    .description('desc')
    .owner('Bob')
    .summary('sum')
    .createdAt(new Date('2022-01-01 02:00:00.000'))
    .requires({})
    .search(Search.builder().id('search.id').build())
    .build();

  return (simpleView());
};

describe('Dashboard', () => {
  const dashboard = createPaginatedDashboards();

  describe('render the Dashboard', () => {
    it('should render dashboard props', async () => {
      render(
        <Dashboard id={dashboard.id}
                   createdAt={dashboard.createdAt}
                   requires={dashboard.requires}
                   owner={dashboard.owner}
                   description={dashboard.description}
                   title={dashboard.title}
                   summary={dashboard.summary}>
          <div />
        </Dashboard>);

      await screen.findByText('search-title-0');
      await screen.findByText('desc');
      await screen.findByText('sum');
      await screen.findByText('2022-01-01 01:00:00');
    });

    it('should render text Last saved if current user the sane as owner', async () => {
      render(
        <CurrentUserProvider>
          <Dashboard id={dashboard.id}
                     createdAt={dashboard.createdAt}
                     requires={dashboard.requires}
                     owner="betty"
                     description={dashboard.description}
                     title={dashboard.title}
                     summary={dashboard.summary}>
            <div />
          </Dashboard>
        </CurrentUserProvider>,
      );

      await screen.findByText('Last saved');
    });

    it('should render text Shared by user if current user not the same as owner', async () => {
      render(
        <CurrentUserProvider>
          <Dashboard id={dashboard.id}
                     createdAt={dashboard.createdAt}
                     requires={dashboard.requires}
                     owner="bob"
                     description={dashboard.description}
                     title={dashboard.title}
                     summary={dashboard.summary}>
            <div />
          </Dashboard>
        </CurrentUserProvider>,
      );

      await screen.findByText('Shared by bob, last saved');
    });
  });
});
