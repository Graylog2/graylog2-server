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

import { alice } from 'fixtures/users';
import View from 'views/logic/views/View';
import Search from 'views/logic/search/Search';
import CurrentUserContext from 'contexts/CurrentUserContext';

import DashboardListItem from './DashboardListItem';

jest.mock('routing/Routes', () => ({ pluginRoute: () => () => '/route' }));
jest.mock('hooks/useUserDateTime');
const mockedUnixTime = 1577836800000; // 2020-01-01 00:00:00.000

jest.useFakeTimers()
  // @ts-expect-error
  .setSystemTime(mockedUnixTime);

describe('Render DashboardListItem', () => {
  const dashboard = View.builder()
    .type('DASHBOARD')
    .id('search-id-0')
    .title('search-title-0')
    .description('desc')
    .owner('Bob')
    .summary('sum')
    .createdAt(new Date())
    .requires({})
    .search(Search.builder().id('search.id').build())
    .build();

  it('should render dashboard props', async () => {
    render(
      <DashboardListItem id={dashboard.id}
                         createdAt={dashboard.createdAt}
                         requires={dashboard.requires}
                         owner={dashboard.owner}
                         description={dashboard.description}
                         title={dashboard.title}
                         summary={dashboard.summary}>
        <div />
      </DashboardListItem>);

    await screen.findByText('search-title-0');
    await screen.findByText('desc');
    await screen.findByText('sum');
    await screen.findByText('2020-01-01 00:00:00');
  });

  it('should render text "Last saved" if current user the same as an owner', async () => {
    render(
      <CurrentUserContext.Provider value={alice}>
        <DashboardListItem id={dashboard.id}
                           createdAt={dashboard.createdAt}
                           requires={dashboard.requires}
                           owner={alice.username}
                           description={dashboard.description}
                           title={dashboard.title}
                           summary={dashboard.summary}>
          <div />
        </DashboardListItem>
      </CurrentUserContext.Provider>,
    );

    await screen.findByText('Last saved');
  });

  it('should render text "Shared by user" if current user not the same as an owner', async () => {
    render(
      <CurrentUserContext.Provider value={alice}>
        <DashboardListItem id={dashboard.id}
                           createdAt={dashboard.createdAt}
                           requires={dashboard.requires}
                           owner="bob"
                           description={dashboard.description}
                           title={dashboard.title}
                           summary={dashboard.summary}>
          <div />
        </DashboardListItem>
      </CurrentUserContext.Provider>,
    );

    await screen.findByText('Shared by bob, last saved');
  });
});
