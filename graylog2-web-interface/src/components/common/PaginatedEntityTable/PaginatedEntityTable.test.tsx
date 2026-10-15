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

import asMock from 'helpers/mocking/AsMock';
import useFetchEntities from 'components/common/PaginatedEntityTable/useFetchEntities';

import PaginatedEntityTable from './PaginatedEntityTable';
import type { MiddleSectionProps } from './PaginatedEntityTable';

jest.mock('components/common/PaginatedEntityTable/useFetchEntities', () => jest.fn());
jest.mock('components/common/EntityDataTable/hooks/useUserLayoutPreferences');

type TestEntity = { id: string; title: string };

const tableLayout = {
  entityTableId: 'test-table',
  defaultSort: { attributeId: 'title', direction: 'asc' as const },
  defaultDisplayedAttributes: ['title'],
  defaultPageSize: 10,
  defaultColumnOrder: ['title'],
};

const MiddleSection = () => <div>Middle section</div>;

const renderTable = (middleSection: React.ComponentType<MiddleSectionProps> | undefined = undefined) =>
  render(
    <PaginatedEntityTable<TestEntity>
      humanName="things"
      tableLayout={tableLayout}
      fetchEntities={() => Promise.resolve({ list: [], pagination: { total: 0 }, attributes: [] })}
      keyFn={(params) => ['test', params]}
      columnRenderers={{}}
      entityAttributesAreCamelCase={false}
      middleSection={middleSection}
      withoutURLParams
    />,
  );

const isBefore = (first: Element, second: Element) =>
  Boolean(first.compareDocumentPosition(second) & Node.DOCUMENT_POSITION_FOLLOWING);

describe('PaginatedEntityTable', () => {
  it('renders an error message when fetching entities fails', async () => {
    asMock(useFetchEntities).mockReturnValue({
      isInitialLoading: false,
      refetch: jest.fn(),
      data: undefined,
      isError: true,
      error: new Error('Request failed with status 500'),
    });

    renderTable();

    await screen.findByText(/Fetching things failed/);
    await screen.findByText(/Request failed with status 500/);
  });

  it('keeps the search form visible when fetching entities fails, so filters can still be modified', async () => {
    asMock(useFetchEntities).mockReturnValue({
      isInitialLoading: false,
      refetch: jest.fn(),
      data: undefined,
      isError: true,
      error: new Error('Request failed with status 500'),
    });

    renderTable();

    await screen.findByPlaceholderText('Search for things');
  });

  it('renders the table when fetching entities succeeds', async () => {
    asMock(useFetchEntities).mockReturnValue({
      isInitialLoading: false,
      refetch: jest.fn(),
      data: {
        list: [],
        pagination: { total: 0 },
        attributes: [],
      },
      isError: false,
      error: undefined,
    });

    renderTable();

    await screen.findByText(/No things have been found/);
  });

  it('renders the middle section above the search form, so the search form sits directly above the table', async () => {
    asMock(useFetchEntities).mockReturnValue({
      isInitialLoading: false,
      refetch: jest.fn(),
      data: {
        list: [],
        pagination: { total: 0 },
        attributes: [],
      },
      isError: false,
      error: undefined,
    });

    renderTable(MiddleSection);

    const middleSection = await screen.findByText('Middle section');
    const searchInput = await screen.findByPlaceholderText('Search for things');
    const tableContent = await screen.findByText(/No things have been found/);

    expect(isBefore(middleSection, searchInput)).toBe(true);
    expect(isBefore(searchInput, tableContent)).toBe(true);
  });
});
