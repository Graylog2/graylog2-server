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
import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import { QueryParamProvider } from 'use-query-params';
import { ReactRouter6Adapter } from 'use-query-params/adapters/react-router-6';

import DashboardsPage from './DashboardsPage';

jest.mock('components/common/PaginatedEntityTable/useFetchEntities', () => () => ({
  data: {
    pagination: {
      total: 0,
      page: 1,
      perPage: 5,
      count: 0,
    },
    list: [],
  },
  refetch: () => {},
}));

jest.mock('routing/Routes', () => ({
  pluginRoute: jest.fn(),
}));

describe('DashboardsPage', () => {
  it('should render header and list', async () => {
    render(
      <QueryParamProvider adapter={ReactRouter6Adapter}>
        <DashboardsPage />
      </QueryParamProvider>);

    await screen.findByRole('heading', { name: /dashboards/i });
    await screen.findByText('No dashboards have been found.');
  });
});
