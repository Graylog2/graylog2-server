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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import { defaultUser } from 'defaultMockValues';

import { asMock } from 'helpers/mocking';
import useCurrentUser from 'hooks/useCurrentUser';
import type { Sort } from 'stores/PaginationTypes';

import PaginatedEntityTable from './PaginatedEntityTable';

jest.mock('hooks/useCurrentUser');
jest.mock('logic/telemetry/useSendTelemetry', () => () => jest.fn());

type TestEntity = {
  id: string;
  title: string;
  description: string;
};

const mockFetchEntitiesWithData = jest.fn().mockResolvedValue({
  list: [
    { id: '1', title: 'Entity 1', description: 'Description 1' },
    { id: '2', title: 'Entity 2', description: 'Description 2' },
  ],
  pagination: { total: 2, page: 1, perPage: 20 },
  attributes: [
    { id: 'title', title: 'Title', type: 'STRING' as const, sortable: true },
    { id: 'description', title: 'Description', type: 'STRING' as const, sortable: true },
  ],
});

const mockFetchEntitiesEmpty = jest.fn().mockResolvedValue({
  list: [],
  pagination: { total: 0, page: 1, perPage: 20 },
  attributes: [
    { id: 'title', title: 'Title', type: 'STRING' as const, sortable: true },
    { id: 'description', title: 'Description', type: 'STRING' as const, sortable: true },
  ],
});

const DEFAULT_LAYOUT = {
  entityTableId: 'test-entity-table',
  defaultPageSize: 20,
  defaultSort: { attributeId: 'title', direction: 'asc' } as Sort,
  defaultDisplayedAttributes: ['title', 'description'],
  defaultColumnOrder: ['title', 'description'],
};

describe('<PaginatedEntityTable />', () => {
  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(defaultUser);
    jest.clearAllMocks();
  });

  const defaultProps = {
    humanName: 'test entities',
    tableLayout: DEFAULT_LAYOUT,
    entityAttributesAreCamelCase: false,
    columnRenderers: {},
    keyFn: (options) => ['test-key', options],
  };

  it('should show search bar when there are entities', async () => {
    render(
      <PaginatedEntityTable<TestEntity>
        {...defaultProps}
        fetchEntities={mockFetchEntitiesWithData}
        withoutURLParams
      />,
    );

    await waitFor(() => {
      expect(screen.getByPlaceholderText('Search for test entities')).toBeInTheDocument();
    });

    expect(screen.getByText('Entity 1')).toBeInTheDocument();
    expect(screen.getByText('Entity 2')).toBeInTheDocument();
  });

  it('should hide search bar when there are no entities and hideSearchWhenEmpty is true (default)', async () => {
    render(
      <PaginatedEntityTable<TestEntity>
        {...defaultProps}
        fetchEntities={mockFetchEntitiesEmpty}
        withoutURLParams
      />,
    );

    await waitFor(() => {
      expect(screen.getByText('No test entities have been found.')).toBeInTheDocument();
    });

    expect(screen.queryByPlaceholderText('Search for test entities')).not.toBeInTheDocument();
  });

  it('should show search bar when there are no entities and hideSearchWhenEmpty is false', async () => {
    render(
      <PaginatedEntityTable<TestEntity>
        {...defaultProps}
        fetchEntities={mockFetchEntitiesEmpty}
        hideSearchWhenEmpty={false}
        withoutURLParams
      />,
    );

    await waitFor(() => {
      expect(screen.getByText('No test entities have been found.')).toBeInTheDocument();
    });

    expect(screen.getByPlaceholderText('Search for test entities')).toBeInTheDocument();
  });

  it('should not show search bar when externalSearch is provided', async () => {
    render(
      <PaginatedEntityTable<TestEntity>
        {...defaultProps}
        fetchEntities={mockFetchEntitiesWithData}
        externalSearch={{ query: 'test', onSearch: jest.fn(), onReset: jest.fn() }}
        withoutURLParams
      />,
    );

    await waitFor(() => {
      expect(screen.getByText('Entity 1')).toBeInTheDocument();
    });

    expect(screen.queryByPlaceholderText('Search for test entities')).not.toBeInTheDocument();
  });

  it('should use custom search placeholder', async () => {
    render(
      <PaginatedEntityTable<TestEntity>
        {...defaultProps}
        fetchEntities={mockFetchEntitiesWithData}
        searchPlaceholder="Custom search placeholder"
        withoutURLParams
      />,
    );

    await waitFor(() => {
      expect(screen.getByPlaceholderText('Custom search placeholder')).toBeInTheDocument();
    });
  });
});
