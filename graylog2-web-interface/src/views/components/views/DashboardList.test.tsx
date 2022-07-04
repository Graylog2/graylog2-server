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

import DashboardList from './DashboardList';

jest.mock('routing/Routes', () => ({ pluginRoute: () => () => '/route' }));
jest.mock('hooks/useUserDateTime');

const createPaginatedDashboards = (count = 1) => {
  const dashboards: Array<View> = [];

  if (count > 0) {
    // eslint-disable-next-line no-plusplus
    for (let i = 0; i < count; i++) {
      const simpleView = (): View => View.builder()
        .type('DASHBOARD')
        .id(`search-id-${i}`)
        .title(`search-title-${i}`)
        .description('desc')
        .owner('Bob')
        .createdAt(new Date())
        .requires({})
        .search(Search.builder().id('search.id').build())
        .build();
      dashboards.push(simpleView());
    }
  }

  return {
    pagination: {
      total: count,
      page: count > 0 ? count : 1,
      perPage: 5,
      count,
    },
    list: dashboards,
  };
};

describe('render the DashboardList', () => {
  it('should render empty', async () => {
    const dashboards = createPaginatedDashboards(0);

    render(
      <DashboardList dashboards={dashboards.list}
                     pagination={dashboards.pagination}
                     handleSearch={() => {}}
                     handleDashboardDelete={() => {}} />);

    await screen.findByText('There are no dashboards present/matching the filter!');
  });

  it('should render list', async () => {
    const dashboards = createPaginatedDashboards(3);

    render(<DashboardList dashboards={dashboards.list}
                          pagination={dashboards.pagination}
                          handleSearch={() => {}}
                          handleDashboardDelete={() => {}} />);

    await screen.findByText('search-title-0');
    await screen.findByText('search-title-1');
    await screen.findByText('search-title-2');
  });
});
