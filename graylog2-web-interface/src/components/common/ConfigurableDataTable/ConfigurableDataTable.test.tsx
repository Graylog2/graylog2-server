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

import ConfigurableDataTable from './ConfigurableDataTable';

jest.mock('hooks/useCurrentUser');

describe('<ConfigurableDataTable />', () => {
  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(defaultUser);
  });

  const availableAttributes = [
    { id: 'title', title: 'Title', sortable: true },
    { id: 'description', title: 'Description', sortable: true },
    { id: 'stream', title: 'Stream', sortable: true },
    { id: 'status', title: 'Status', sortable: true },
  ];

  const selectedAttributes = ['title', 'description', 'status'];
  const rows = [
    {
      id: 'row-id',
      title: 'Row title',
      description: 'Row description',
      stream: 'Row stream',
      status: 'enabled',
    },
  ];

  it('should render selected rows and table headers', async () => {
    render(<ConfigurableDataTable attributes={selectedAttributes}
                                  rows={rows}
                                  onSortChange={() => {}}
                                  availableAttributes={availableAttributes}
                                  total={1} />);

    await screen.findByRole('columnheader', { name: /title/i });
    await screen.findByRole('columnheader', { name: /status/i });

    await screen.findByText('Row title');
    await screen.findByText('enabled');

    expect(screen.queryByRole('columnheader', { name: /stream/i })).not.toBeInTheDocument();
    expect(screen.queryByText('Row Stream')).not.toBeInTheDocument();
  });

  it('should render default cell renderer', async () => {
    render(<ConfigurableDataTable attributes={selectedAttributes}
                                  rows={rows}
                                  onSortChange={() => {}}
                                  availableAttributes={availableAttributes}
                                  total={1} />);

    await screen.findByRole('columnheader', { name: /description/i });
    await screen.findByText('Row description');
  });

  it('should render custom cell and header renderer', async () => {
    render(<ConfigurableDataTable attributes={selectedAttributes}
                                  rows={rows}
                                  onSortChange={() => {}}
                                  customCells={{
                                    title: {
                                      renderCell: (listItem) => `The title: ${listItem.title}`,
                                    },
                                  }}
                                  customHeaders={{
                                    title: {
                                      renderHeader: (attribute) => `Custom ${attribute.title} Header`,
                                    },
                                  }}
                                  availableAttributes={availableAttributes}
                                  total={1} />);

    await screen.findByRole('columnheader', { name: /custom title header/i });
    await screen.findByText('The title: Row title');
  });

  it('should render row actions', async () => {
    render(<ConfigurableDataTable<{ id: string, title: string }> attributes={selectedAttributes}
                                                                 rows={rows}
                                                                 onSortChange={() => {}}
                                                                 rowActions={(row) => `Custom actions for ${row.title}`}
                                                                 availableAttributes={availableAttributes}
                                                                 total={1} />);

    await screen.findByText('Custom actions for Row title');
  });

  it('should not render column for attribute is user does not have required permissions', () => {
    asMock(useCurrentUser).mockReturnValue(defaultUser.toBuilder().permissions(Immutable.List()).build());

    render(<ConfigurableDataTable attributes={selectedAttributes}
                                  rows={rows}
                                  onSortChange={() => {}}
                                  attributePermissions={{
                                    status: {
                                      permissions: ['status:read'],
                                    },
                                  }}
                                  availableAttributes={availableAttributes}
                                  total={1} />);

    expect(screen.queryByRole('columnheader', { name: /status/i })).not.toBeInTheDocument();
    expect(screen.queryByText('enabled')).not.toBeInTheDocument();
  });

  it('should display active sort', async () => {
    render(<ConfigurableDataTable attributes={selectedAttributes}
                                  rows={rows}
                                  onSortChange={() => {}}
                                  activeSort={{
                                    attributeId: 'description',
                                    order: 'asc',
                                  }}
                                  availableAttributes={availableAttributes}
                                  total={1} />);

    await screen.findByTitle(/sort description descending/i);
  });

  it('should sort based on attribute', async () => {
    const onSortChange = jest.fn();

    render(<ConfigurableDataTable attributes={selectedAttributes}
                                  rows={rows}
                                  onSortChange={onSortChange}
                                  availableAttributes={availableAttributes}
                                  total={1} />);

    userEvent.click(await screen.findByTitle(/sort description ascending/i));

    await waitFor(() => expect(onSortChange).toHaveBeenCalledTimes(1));

    expect(onSortChange).toHaveBeenCalledWith({ attributeId: 'description', order: 'asc' });
  });

  it('should provide selected item ids for bulk actions', async () => {
    const renderBulkActions = jest.fn(() => <div>Custom bulk actions</div>);

    render(<ConfigurableDataTable attributes={selectedAttributes}
                                  rows={rows}
                                  onSortChange={() => {}}
                                  bulkActions={renderBulkActions}
                                  availableAttributes={availableAttributes}
                                  total={1} />);

    const rowCheckboxes = await screen.findAllByRole('checkbox', { name: /select row/i });
    userEvent.click(rowCheckboxes[0]);

    await screen.findByText('Custom bulk actions');

    await waitFor(() => expect(renderBulkActions).toHaveBeenCalledWith(['row-id'], expect.any(Function)));
  });

  it('should provide bulk actions with function to update selected items', async () => {
    const selectedItemInfo = '1 item selected';
    const renderBulkActions = (_selectedItemIds: Array<string>, setSelectedItemIds: (selectedItemIds: Array<string>) => void) => (
      <button onClick={() => setSelectedItemIds([])} type="button">Reset selection</button>
    );
    asMock(useCurrentUser).mockReturnValue(defaultUser.toBuilder().permissions(Immutable.List()).build());

    render(<ConfigurableDataTable attributes={selectedAttributes}
                                  rows={rows}
                                  onSortChange={() => {}}
                                  bulkActions={renderBulkActions}
                                  availableAttributes={availableAttributes}
                                  total={1} />);

    const rowCheckboxes = await screen.findAllByRole('checkbox', { name: /select row/i });
    userEvent.click(rowCheckboxes[0]);

    await screen.findByText(selectedItemInfo);
    const customBulkAction = await screen.findByRole('button', { name: /reset selection/i });

    userEvent.click(customBulkAction);

    expect(screen.queryByText(selectedItemInfo)).not.toBeInTheDocument();
    expect(rowCheckboxes[0]).not.toBeChecked();
  });

  it('should select all items', async () => {
    asMock(useCurrentUser).mockReturnValue(defaultUser.toBuilder().permissions(Immutable.List()).build());

    render(<ConfigurableDataTable attributes={selectedAttributes}
                                  rows={rows}
                                  onSortChange={() => {}}
                                  bulkActions={() => <div />}
                                  availableAttributes={availableAttributes}
                                  total={1} />);

    const rowCheckboxes = await screen.findAllByRole('checkbox', { name: /select row/i });

    expect(rowCheckboxes[0]).not.toBeChecked();

    const selectAllCheckbox = await screen.findByRole('checkbox', { name: /all visible rows/i });
    userEvent.click(selectAllCheckbox);

    expect(rowCheckboxes[0]).toBeChecked();

    await screen.findByText('1 item selected');

    userEvent.click(selectAllCheckbox);

    expect(rowCheckboxes[0]).not.toBeChecked();
  });
});
