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
import { ATTRIBUTE_STATUS } from 'components/common/EntityDataTable/Constants';

import EntityDataTable from './EntityDataTable';

jest.mock('hooks/useCurrentUser');
jest.mock('logic/telemetry/useSendTelemetry', () => () => jest.fn());

declare module 'graylog-web-plugin/plugin' {
  interface EntityActions {
    status: 'read';
  }
}

describe('<EntityDataTable />', () => {
  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(defaultUser);
  });

  const columnSchemas: Array<ColumnSchema> = [
    { id: 'title', title: 'Title', type: 'STRING', sortable: true },
    { id: 'description', title: 'Description', type: 'STRING', sortable: true, sliceable: true },
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
    title: { status: ATTRIBUTE_STATUS.show },
    description: { status: ATTRIBUTE_STATUS.show },
    status: { status: ATTRIBUTE_STATUS.show },
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

  const defaultProps = {
    defaultDisplayedColumns,
    defaultColumnOrder: defaultDisplayedColumns,
    layoutPreferences: { attributes: columnPreferences },
    enableSlicing: true,
    entities: data,
    onLayoutPreferencesChange: () => Promise.resolve(),
    onSortChange: () => {},
    entityAttributesAreCamelCase: true,
    columnSchemas,
    onResetLayoutPreferences: () => Promise.resolve(),
    onChangeSlicing: () => {},
  };

  it('should render selected columns and table headers', async () => {
    render(<EntityDataTable {...defaultProps} />);

    await screen.findByRole('columnheader', { name: /title/i });
    await screen.findByRole('columnheader', { name: /status/i });

    await screen.findByText('Entity title');
    await screen.findByText('enabled');

    expect(screen.queryByRole('columnheader', { name: /stream/i })).not.toBeInTheDocument();
    expect(screen.queryByText('Row Stream')).not.toBeInTheDocument();
  });

  it('should render default cell renderer', async () => {
    render(<EntityDataTable {...defaultProps} />);

    await screen.findByRole('columnheader', { name: /description/i });
    await screen.findByText('Entity description');
  });

  it('should render custom cell and header renderer', async () => {
    render(
      <EntityDataTable
        {...defaultProps}
        columnRenderers={{
          attributes: {
            title: {
              renderCell: (title: string) => `The title: ${title}`,
              renderHeader: (title) => `Custom ${title} Header`,
            },
          },
        }}
      />,
    );

    await screen.findByRole('columnheader', { name: /custom title header/i });
    await screen.findByText('The title: Entity title');
  });

  it('should merge attribute and type column renderers renderer', async () => {
    render(
      <EntityDataTable
        {...defaultProps}
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
      />,
    );

    await screen.findByRole('columnheader', { name: /custom header for type - title/i });
    await screen.findByText('Custom Cell For Attribute - Entity title');

    expect(screen.queryByText('Custom Cell For Type - Entity title')).not.toBeInTheDocument();
  });

  it('should render row actions', async () => {
    render(
      <EntityDataTable<{ id: string; title: string }>
        {...defaultProps}
        entityActions={(entity) => `Custom actions for ${entity.title}`}
      />,
    );

    await screen.findByText('Custom actions for Entity title');
  });

  it('keeps the trailing actions column even without row actions', async () => {
    render(<EntityDataTable {...defaultProps} />);

    const headers = await screen.findAllByRole('columnheader');

    const visibleAttributeHeaders = 3; // title, description, status

    expect(headers).toHaveLength(visibleAttributeHeaders + 1);
  });

  it('should not render column if user does not have required permissions', () => {
    asMock(useCurrentUser).mockReturnValue(defaultUser.toBuilder().permissions(Immutable.List()).build());

    render(<EntityDataTable {...defaultProps} />);

    expect(screen.queryByRole('columnheader', { name: /status/i })).not.toBeInTheDocument();
    expect(screen.queryByText('enabled')).not.toBeInTheDocument();
  });

  it('should display active sort', async () => {
    render(
      <EntityDataTable
        {...defaultProps}
        activeSort={{
          attributeId: 'description',
          direction: 'asc',
        }}
      />,
    );

    await screen.findByTitle(/sort description descending/i);
  });

  it('should sort based on column', async () => {
    const onSortChange = jest.fn();

    render(<EntityDataTable {...defaultProps} onSortChange={onSortChange} />);

    userEvent.click(await screen.findByRole('button', { name: /toggle description actions/i }));
    userEvent.click(await screen.findByRole('menuitem', { name: /sort ascending/i }));

    await waitFor(() => expect(onSortChange).toHaveBeenCalledTimes(1));

    expect(onSortChange).toHaveBeenCalledWith({ attributeId: 'description', direction: 'asc' });
  });

  it('should slice by column using header action', async () => {
    const onChangeSlicing = jest.fn(() => {});

    render(<EntityDataTable {...defaultProps} columnSchemas={columnSchemas} onChangeSlicing={onChangeSlicing} />);

    userEvent.click(await screen.findByRole('button', { name: /toggle description actions/i }));
    userEvent.click(await screen.findByRole('menuitem', { name: /slice by values/i }));

    expect(onChangeSlicing).toHaveBeenCalledWith('description');
  });

  it('should remove slicing using header action', async () => {
    const onChangeSlicing = jest.fn(() => {});

    render(
      <EntityDataTable
        {...defaultProps}
        columnSchemas={columnSchemas}
        onChangeSlicing={onChangeSlicing}
        activeSliceCol="description"
      />,
    );

    userEvent.click(await screen.findByRole('button', { name: /toggle description actions/i }));
    userEvent.click(await screen.findByRole('menuitem', { name: /no slicing/i }));

    expect(onChangeSlicing).toHaveBeenCalledWith(undefined, undefined);
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

    render(<EntityDataTable {...defaultProps} bulkSelection={{ actions: <BulkActions /> }} />);

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

    render(<EntityDataTable {...defaultProps} bulkSelection={{ actions: <div /> }} />);

    const rowCheckboxes = await screen.findAllByRole('checkbox', { name: /select entity/i });

    expect(rowCheckboxes[0]).not.toBeChecked();

    const selectAllCheckbox = await screen.findByRole('checkbox', { name: /select all visible entities/i });
    userEvent.click(selectAllCheckbox);

    expect(rowCheckboxes[0]).toBeChecked();

    await screen.findByText('1 item selected');

    userEvent.click(selectAllCheckbox);

    expect(rowCheckboxes[0]).not.toBeChecked();
  });

  it('user preferences should include all currently visible columns on preferences update', async () => {
    const onLayoutPreferencesChange = jest.fn();

    render(
      <EntityDataTable
        {...defaultProps}
        layoutPreferences={{}}
        defaultDisplayedColumns={['description', 'status', 'title']}
        defaultColumnOrder={['description', 'status', 'title']}
        onLayoutPreferencesChange={onLayoutPreferencesChange}
      />,
    );

    await screen.findByRole('columnheader', { name: /title/i });
    await screen.findByRole('columnheader', { name: /status/i });
    await screen.findByRole('columnheader', { name: /description/i });

    userEvent.click(await screen.findByRole('button', { name: /configure visible columns/i }));
    userEvent.click(await screen.findByRole('menuitem', { name: /hide title/i }));

    expect(onLayoutPreferencesChange).toHaveBeenCalledWith({
      attributes: {
        description: { status: ATTRIBUTE_STATUS.show },
        status: { status: ATTRIBUTE_STATUS.show },
        title: { status: 'hide' },
      },
    });
  });

  it('if there are user preferences, only selected columns should be displayed', async () => {
    const onLayoutPreferencesChange = jest.fn();

    render(
      <EntityDataTable
        {...defaultProps}
        layoutPreferences={{
          attributes: {
            description: { status: ATTRIBUTE_STATUS.show },
            status: { status: ATTRIBUTE_STATUS.show },
          },
        }}
        defaultDisplayedColumns={['description', 'status', 'title']}
        defaultColumnOrder={['description', 'status', 'title']}
        onLayoutPreferencesChange={onLayoutPreferencesChange}
      />,
    );

    userEvent.click(await screen.findByRole('button', { name: /configure visible columns/i }));
    await screen.findByRole('menuitem', { name: /show title/i });

    expect(
      screen.queryByRole('columnheader', {
        name: /title/i,
      }),
    ).not.toBeInTheDocument();
  });

  it('should reset layout preferences via reset all columns action', async () => {
    const onResetLayoutPreferences = jest.fn().mockResolvedValue(undefined);

    const initialLayoutPreferences = {
      attributes: {
        title: { status: ATTRIBUTE_STATUS.hide },
        description: { status: ATTRIBUTE_STATUS.show },
        status: { status: ATTRIBUTE_STATUS.show },
      },
      order: ['status', 'description', 'title'],
    };

    render(
      <EntityDataTable
        {...defaultProps}
        defaultDisplayedColumns={['title', 'description', 'status']}
        defaultColumnOrder={['title', 'description', 'status']}
        layoutPreferences={initialLayoutPreferences}
        onResetLayoutPreferences={onResetLayoutPreferences}
      />,
    );

    userEvent.click(await screen.findByRole('button', { name: /configure visible columns/i }));
    userEvent.click(await screen.findByRole('menuitem', { name: /reset all columns/i }));

    await waitFor(() => expect(onResetLayoutPreferences).toHaveBeenCalledTimes(1));
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
        {...defaultProps}
        layoutPreferences={{ attributes: { ...columnPreferences, 'created_at': { status: ATTRIBUTE_STATUS.show } } }}
        entities={dataWithCamelCaseAttributes}
        columnRenderers={{
          attributes: {
            created_at: {
              renderCell: (createdAt: string) => `Custom Cell For Created At - ${createdAt}`,
            },
          },
        }}
      />,
    );

    await screen.findByText('Custom Cell For Created At - 2021-01-01');
  });
});
