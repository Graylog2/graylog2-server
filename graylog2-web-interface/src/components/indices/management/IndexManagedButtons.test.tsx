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
import { OrderedMap } from 'immutable';
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import asMock from 'helpers/mocking/AsMock';
import useTableFilterContext from 'components/common/PaginatedEntityTable/useTableFilterContext';

import IndexManagedButtons from './IndexManagedButtons';
import type { IndexSummary } from './types';
import useIndexOverview from './useIndexOverview';

jest.mock('./useIndexOverview');
jest.mock('components/common/PaginatedEntityTable/useTableFilterContext');

const index = (name: string, indexSetId: string | null) => ({ index: name, index_set_id: indexSetId }) as IndexSummary;

describe('IndexManagedButtons', () => {
  const onChangeFilters = jest.fn();
  const withFilters = (filters: OrderedMap<string, Array<string>>) =>
    asMock(useTableFilterContext).mockReturnValue({
      searchParams: {
        page: 1,
        pageSize: 20,
        query: '',
        sort: { attributeId: 'index', direction: 'asc' },
        filters,
      },
      setQuery: jest.fn(),
      onChangeFilters,
      onChangeSlicingFilter: jest.fn(),
      paginationState: { page: 1, pageSize: 20, resetPage: jest.fn(), setPagination: jest.fn() },
      resetFilters: jest.fn(),
    });

  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useIndexOverview).mockReturnValue({
      indices: [index('graylog_0', 'set-1'), index('graylog_1', 'set-1'), index('security-auditlog', null)],
      isLoading: false,
    });
  });

  it('counts the indices Graylog manages and the others', async () => {
    withFilters(OrderedMap({ health: ['red'] }));
    render(<IndexManagedButtons />);

    await screen.findByRole('button', { name: /^all\s*3/i });
    screen.getByRole('button', { name: /^managed\s*2/i });

    await userEvent.click(screen.getByRole('button', { name: /^unmanaged\s*1/i }));

    expect(onChangeFilters).toHaveBeenCalledWith(OrderedMap({ health: ['red'], managed: ['false'] }));
  });

  it('removes the filter for All', async () => {
    withFilters(OrderedMap({ managed: ['true'] }));
    render(<IndexManagedButtons />);

    await userEvent.click(await screen.findByRole('button', { name: /^all/i }));

    expect(onChangeFilters).toHaveBeenCalledWith(OrderedMap());
  });
});
