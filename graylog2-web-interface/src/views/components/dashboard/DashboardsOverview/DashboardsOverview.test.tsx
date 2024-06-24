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
import { ReactRouter6Adapter } from 'use-query-params/adapters/react-router-6';
import { useQueryParam, QueryParamProvider } from 'use-query-params';

import View from 'views/logic/views/View';
import Search from 'views/logic/search/Search';
import { asMock } from 'helpers/mocking';
import useFetchEntities from 'components/common/PaginatedEntityTable/useFetchEntities';
import useUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUserLayoutPreferences';
import { layoutPreferences } from 'fixtures/entityListLayoutPreferences';

import DashboardsOverview from './DashboardsOverview';

jest.mock('routing/Routes', () => ({ pluginRoute: () => () => '/route' }));
jest.mock('components/common/PaginatedEntityTable/useFetchEntities');
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

jest.mock('use-query-params', () => ({
  ...jest.requireActual('use-query-params'),
  useQueryParam: jest.fn(),
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
    isInitialLoading: false,
  };
};

describe('DashboardsOverview', () => {
  const SUT = () => (
    <QueryParamProvider adapter={ReactRouter6Adapter}>
      <DashboardsOverview />
    </QueryParamProvider>
  );

  beforeEach(() => {
    asMock(useUserLayoutPreferences).mockReturnValue({ data: layoutPreferences, isInitialLoading: false });
    asMock(useFetchEntities).mockReturnValue(loadDashboardsResponse(0));
    asMock(useQueryParam).mockImplementation(() => ([undefined, () => {}]));
  });

  it('should render empty', async () => {
    asMock(useFetchEntities).mockReturnValue(loadDashboardsResponse(0));

    render(<SUT />);

    await screen.findByText('No dashboards have been found.');
  });

  it('should render list', async () => {
    asMock(useFetchEntities).mockReturnValue(loadDashboardsResponse(3));

    render(<SUT />);

    await screen.findByText('search-title-0');
    await screen.findByText('search-title-1');
    await screen.findByText('search-title-2');
  });

  it('should use search query from URL params', async () => {
    asMock(useQueryParam).mockImplementation((field: string) => {
      const value = field === 'query' ? 'example query' : undefined;

      return [value, () => {}];
    });

    render(<SUT />);

    const searchInput = await screen.findByPlaceholderText('Search for dashboards');

    expect(searchInput).toHaveValue('example query');
  });
});
