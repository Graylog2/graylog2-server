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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import { defaultUser } from 'defaultMockValues';
import Immutable from 'immutable';
import userEvent from '@testing-library/user-event';

import { asMock } from 'helpers/mocking';
import useCurrentUser from 'hooks/useCurrentUser';
import type { ColumnSchema } from 'components/common/EntityDataTable/types';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';

import EntityDataTable from './EntityDataTable';

jest.mock('hooks/useCurrentUser');

describe('<EntityDataTable />', () => {
  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(defaultUser);
  });

  const columnSchemas: Array<ColumnSchema> = [
    { id: 'title', title: 'Title', type: 'STRING', sortable: true },
    { id: 'description', title: 'Description', type: 'STRING', sortable: true },
    { id: 'stream', title: 'Stream', type: 'STRING', sortable: true },
    {
      id: 'status',
      title: 'Status',
      type: 'STRING',
      sortable: true,
      permissions: ['status:read'],
    },
    { id: 'created_at', title: 'Created At', type: 'STRING', sortable: true },
  ];

  const columnPreferences = {
    title: { status: 'show' },
    description: { status: 'show' },
    status: { status: 'show' },
  } as const;

  const defaultDisplayedColumns = ['title', 'description', 'summary', 'status'];

  const data = [
    {
      id: 'row-id',
      title: 'Entity title',
      description: 'Entity description',
      stream: 'Entity stream',
      status: 'enabled',
    },
  ];

  it('should render selected columns and table headers', async () => {
    render(
      <EntityDataTable
        defaultDisplayedColumns={defaultDisplayedColumns}
        columnPreferences={columnPreferences}
        entities={data}
        onColumnPreferencesChange={() => {}}
        onSortChange={() => {}}
        entityAttributesAreCamelCase
        columnSchemas={columnSchemas}
      />,
    );

    await screen.findByRole('columnheader', { name: /title/i });
    await screen.findByRole('columnheader', { name: /status/i });

    await screen.findByText('Entity title');
    await screen.findByText('enabled');

    expect(screen.queryByRole('columnheader', { name: /stream/i })).not.toBeInTheDocument();
    expect(screen.queryByText('Row Stream')).not.toBeInTheDocument();
  });

  it('should render default cell renderer', async () => {
    render(
      <EntityDataTable
        defaultDisplayedColumns={defaultDisplayedColumns}
        columnPreferences={columnPreferences}
        entities={data}
        onSortChange={() => {}}
        entityAttributesAreCamelCase
        onColumnPreferencesChange={() => {}}
        columnSchemas={columnSchemas}
      />,
    );

    await screen.findByRole('columnheader', { name: /description/i });
    await screen.findByText('Entity description');
  });

  it('should render custom cell and header renderer', async () => {
    render(
      <EntityDataTable
        columnPreferences={columnPreferences}
        defaultDisplayedColumns={defaultDisplayedColumns}
        entities={data}
        onSortChange={() => {}}
        entityAttributesAreCamelCase
        onColumnPreferencesChange={() => {}}
        columnRenderers={{
          attributes: {
            title: {
              renderCell: (title: string) => `The title: ${title}`,
              renderHeader: (title) => `Custom ${title} Header`,
            },
          },
        }}
        columnSchemas={columnSchemas}
      />,
    );

    await screen.findByRole('columnheader', { name: /custom title header/i });
    await screen.findByText('The title: Entity title');
  });

  it('should merge attribute and type column renderers renderer', async () => {
    render(
      <EntityDataTable
        columnPreferences={columnPreferences}
        defaultDisplayedColumns={defaultDisplayedColumns}
        entities={data}
        onSortChange={() => {}}
        entityAttributesAreCamelCase
        onColumnPreferencesChange={() => {}}
        columnRenderers={{
          attributes: {
            title: {
              renderCell: (title: string) => `Custom Cell For Attribute - ${title}`,
            },
          },
          types: {
            STRING: {
              renderCell: (title: string) => `Custom Cell For Type - ${title}`,
              renderHeader: (title: string) => `Custom Header For Type - ${title}`,
            },
          },
        }}
        columnSchemas={columnSchemas}
      />,
    );

    await screen.findByRole('columnheader', { name: /custom header for type - title/i });
    await screen.findByText('Custom Cell For Attribute - Entity title');

    expect(screen.queryByText('Custom Cell For Type - Entity title')).not.toBeInTheDocument();
  });

  it('should render row actions', async () => {
    render(
      <EntityDataTable<{ id: string; title: string }>
        columnPreferences={columnPreferences}
        defaultDisplayedColumns={defaultDisplayedColumns}
        entities={data}
        onSortChange={() => {}}
        entityAttributesAreCamelCase
        onColumnPreferencesChange={() => {}}
        entityActions={(entity) => `Custom actions for ${entity.title}`}
        columnSchemas={columnSchemas}
      />,
    );

    await screen.findByText('Custom actions for Entity title');
  });

  it('should not render column if user does not have required permissions', () => {
    asMock(useCurrentUser).mockReturnValue(defaultUser.toBuilder().permissions(Immutable.List()).build());

    render(
      <EntityDataTable
        columnPreferences={columnPreferences}
        defaultDisplayedColumns={defaultDisplayedColumns}
        entities={data}
        onSortChange={() => {}}
        entityAttributesAreCamelCase
        onColumnPreferencesChange={() => {}}
        columnSchemas={columnSchemas}
      />,
    );

    expect(screen.queryByRole('columnheader', { name: /status/i })).not.toBeInTheDocument();
    expect(screen.queryByText('enabled')).not.toBeInTheDocument();
  });

  it('should display active sort', async () => {
    render(
      <EntityDataTable
        columnPreferences={columnPreferences}
        defaultDisplayedColumns={defaultDisplayedColumns}
        entities={data}
        onSortChange={() => {}}
        entityAttributesAreCamelCase
        onColumnPreferencesChange={() => {}}
        activeSort={{
          attributeId: 'description',
          direction: 'asc',
        }}
        columnSchemas={columnSchemas}
      />,
    );

    await screen.findByTitle(/sort description descending/i);
  });

  it('should sort based on column', async () => {
    const onSortChange = jest.fn();

    render(
      <EntityDataTable
        columnPreferences={columnPreferences}
        defaultDisplayedColumns={defaultDisplayedColumns}
        entities={data}
        entityAttributesAreCamelCase
        onSortChange={onSortChange}
        onColumnPreferencesChange={() => {}}
        columnSchemas={columnSchemas}
      />,
    );

    userEvent.click(await screen.findByTitle(/sort description ascending/i));

    await waitFor(() => expect(onSortChange).toHaveBeenCalledTimes(1));

    expect(onSortChange).toHaveBeenCalledWith({ attributeId: 'description', direction: 'asc' });
  });

  it('bulk actions should update selected items', async () => {
    const selectedItemInfo = '1 item selected';

    const BulkActions = () => {
      const { setSelectedEntities } = useSelectedEntities();

      return (
        <button onClick={() => setSelectedEntities([])} type="button">
          Reset selection
        </button>
      );
    };

    asMock(useCurrentUser).mockReturnValue(defaultUser.toBuilder().permissions(Immutable.List()).build());

    render(
      <EntityDataTable
        columnPreferences={columnPreferences}
        defaultDisplayedColumns={defaultDisplayedColumns}
        entities={data}
        onSortChange={() => {}}
        entityAttributesAreCamelCase
        onColumnPreferencesChange={() => {}}
        bulkSelection={{ actions: <BulkActions /> }}
        columnSchemas={columnSchemas}
      />,
    );

    const rowCheckboxes = await screen.findAllByRole('checkbox', { name: /select entity/i });
    userEvent.click(rowCheckboxes[0]);

    await screen.findByText(selectedItemInfo);
    const customBulkAction = await screen.findByRole('button', { name: /reset selection/i });

    userEvent.click(customBulkAction);

    expect(screen.queryByText(selectedItemInfo)).not.toBeInTheDocument();
    expect(rowCheckboxes[0]).not.toBeChecked();
  });

  it('should select all items', async () => {
    asMock(useCurrentUser).mockReturnValue(defaultUser.toBuilder().permissions(Immutable.List()).build());

    render(
      <EntityDataTable
        columnPreferences={columnPreferences}
        defaultDisplayedColumns={defaultDisplayedColumns}
        entities={data}
        onSortChange={() => {}}
        entityAttributesAreCamelCase
        onColumnPreferencesChange={() => {}}
        bulkSelection={{ actions: <div /> }}
        columnSchemas={columnSchemas}
      />,
    );

    const rowCheckboxes = await screen.findAllByRole('checkbox', { name: /select entity/i });

    expect(rowCheckboxes[0]).not.toBeChecked();

    const selectAllCheckbox = await screen.findByRole('checkbox', { name: /select all visible entities/i });
    userEvent.click(selectAllCheckbox);

    expect(rowCheckboxes[0]).toBeChecked();

    await screen.findByText('1 item selected');

    userEvent.click(selectAllCheckbox);

    expect(rowCheckboxes[0]).not.toBeChecked();
  });

  it('should display default columns, which are not hidden via user column preferences and update visibility correctly', async () => {
    const onColumnPreferencesChange = jest.fn();

    render(
      <EntityDataTable
        columnPreferences={{
          description: { status: 'show' },
          status: { status: 'show' },
        }}
        defaultDisplayedColumns={['description', 'status', 'title']}
        entities={data}
        onSortChange={() => {}}
        entityAttributesAreCamelCase
        onColumnPreferencesChange={onColumnPreferencesChange}
        columnSchemas={columnSchemas}
      />,
    );

    userEvent.click(await screen.findByRole('button', { name: /configure visible columns/i }));
    userEvent.click(await screen.findByRole('menuitem', { name: /hide title/i }));

    expect(onColumnPreferencesChange).toHaveBeenCalledWith({
      'description': { 'status': 'show' },
      'status': { 'status': 'show' },
      'title': { 'status': 'hide' },
    });
  });

  it('should hande entities with camel case attributes', async () => {
    const dataWithCamelCaseAttributes = [
      {
        id: 'row-id',
        title: 'Entity title',
        description: 'Entity description',
        stream: 'Entity stream',
        status: 'enabled',
        createdAt: '2021-01-01',
      },
    ];

    render(
      <EntityDataTable
        columnPreferences={{ ...columnPreferences, 'created_at': { status: 'show' } }}
        defaultDisplayedColumns={defaultDisplayedColumns}
        entities={dataWithCamelCaseAttributes}
        onSortChange={() => {}}
        entityAttributesAreCamelCase
        onColumnPreferencesChange={() => {}}
        columnRenderers={{
          attributes: {
            created_at: {
              renderCell: (createdAt: string) => `Custom Cell For Created At - ${createdAt}`,
            },
          },
        }}
        columnSchemas={columnSchemas}
      />,
    );

    await screen.findByText('Custom Cell For Created At - 2021-01-01');
  });
});
