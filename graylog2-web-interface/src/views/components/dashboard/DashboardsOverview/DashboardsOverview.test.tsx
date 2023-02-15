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
import { asMock } from 'helpers/mocking';
import useDashboards from 'views/components/dashboard/hooks/useDashboards';
import useUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUserLayoutPreferences';
import { layoutPreferences } from 'fixtures/entityListLayoutPreferences';

import DashboardsOverview from './DashboardsOverview';

jest.mock('routing/Routes', () => ({ pluginRoute: () => () => '/route' }));
jest.mock('views/components/dashboard/hooks/useDashboards');
jest.mock('components/common/EntityDataTable/hooks/useUserLayoutPreferences');

jest.mock('views/stores/ViewManagementStore', () => ({
  ViewManagementActions: {
    delete: jest.fn(),
    update: {
      completed: {
        listen: () => jest.fn(),
      },
    },
  },
}));

const loadDashboardsResponse = (count = 1) => {
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
        .favorite(true)
        .build();
      dashboards.push(simpleView());
    }
  }

  return {
    data: {
      pagination: {
        total: count,
        page: count > 0 ? count : 1,
        perPage: 5,
        count,
      },
      list: dashboards,
      attributes: [
        {
          id: 'title',
          title: 'Title',
          sortable: true,
        },
        {
          id: 'description',
          title: 'Description',
          sortable: true,
        },
      ],
    },
    refetch: () => {},
    isFetching: false,
  };
};

describe('DashboardsOverview', () => {
  beforeEach(() => {
    asMock(useUserLayoutPreferences).mockReturnValue({ data: layoutPreferences, isLoading: false });
  });

  it('should render empty', async () => {
    asMock(useDashboards).mockReturnValue(loadDashboardsResponse(0));

    render(<DashboardsOverview />);

    await screen.findByText('No dashboards have been created yet.');
  });

  it('should render list', async () => {
    asMock(useDashboards).mockReturnValue(loadDashboardsResponse(3));

    render(<DashboardsOverview />);

    await screen.findByText('search-title-0');
    await screen.findByText('search-title-1');
    await screen.findByText('search-title-2');
  });
});
