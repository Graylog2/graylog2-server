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
import { defaultUser } from 'defaultMockValues';
import Immutable from 'immutable';

import { asMock } from 'helpers/mocking';
import useCurrentUser from 'hooks/useCurrentUser';

import ConfigurableDataTable from './ConfigurableDataTable';

jest.mock('hooks/useCurrentUser');

describe('<ConfigurableDataTable />', () => {
  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(defaultUser);
  });

  const availableAttributes = [
    { id: 'title', title: 'Title' },
    { id: 'description', title: 'Description' },
    { id: 'stream', title: 'Stream' },
    { id: 'status', title: 'Status' },
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
                                  availableAttributes={availableAttributes} />);

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
                                  availableAttributes={availableAttributes} />);

    await screen.findByRole('columnheader', { name: /description/i });
    await screen.findByText('Row description');
  });

  it('should render custom cell and header renderer', async () => {
    render(<ConfigurableDataTable attributes={selectedAttributes}
                                  rows={rows}
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
                                  availableAttributes={availableAttributes} />);

    await screen.findByRole('columnheader', { name: /custom title header/i });
    await screen.findByText('The title: Row title');
  });

  it('should render row actions', async () => {
    render(<ConfigurableDataTable attributes={selectedAttributes}
                                  rows={rows}
                                  rowActions={(row) => `Custom actions for ${row.title}`}
                                  availableAttributes={availableAttributes} />);

    await screen.findByText('Custom actions for Row title');
  });

  it('should not render column for attribute is user does not have required permissions', async () => {
    asMock(useCurrentUser).mockReturnValue(defaultUser.toBuilder().permissions(Immutable.List()).build());

    render(<ConfigurableDataTable attributes={selectedAttributes}
                                  rows={rows}
                                  attributePermissions={{
                                    status: {
                                      permissions: ['status:read'],
                                    },
                                  }}
                                  availableAttributes={availableAttributes} />);

    expect(screen.queryByRole('columnheader', { name: /status/i })).not.toBeInTheDocument();
    expect(screen.queryByText('enabled')).not.toBeInTheDocument();
  });
});
