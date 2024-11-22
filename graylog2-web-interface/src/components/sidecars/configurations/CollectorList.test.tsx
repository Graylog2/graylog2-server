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
import userEvent from '@testing-library/user-event';

import type { Collector } from 'components/sidecars/types';

import CollectorList from './CollectorList';

const collectors: Array<Collector> = [
  {
    id: '659bce1c00e5295c3218f4b4',
    name: 'auditbeat',
    service_type: 'exec',
    node_operating_system: 'linux',
  },
  {
    id: '659bb9b5a5d6ba4b611c7170',
    name: 'filebeat',
    service_type: 'exec',
    node_operating_system: 'linux',
  },
  {
    id: '659bb9b5a5d6ba4b611c7171',
    name: 'filebeat',
    service_type: 'exec',
    node_operating_system: 'darwin',
  },
  {
    id: '659bb9b6a5d6ba4b611c7172',
    name: 'filebeat',
    service_type: 'exec',
    node_operating_system: 'freebsd',
  },
  {
    id: '659bb9b6a5d6ba4b611c7176',
    name: 'filebeat',
    service_type: 'svc',
    node_operating_system: 'windows',
  },
  {
    id: '659bb9b6a5d6ba4b611c7174',
    name: 'nxlog',
    service_type: 'exec',
    node_operating_system: 'linux',
  },
  {
    id: '659bb9b6a5d6ba4b611c7175',
    name: 'nxlog',
    service_type: 'svc',
    node_operating_system: 'windows',
  },
  {
    id: '659bb9b6a5d6ba4b611c7173',
    name: 'winlogbeat',
    service_type: 'svc',
    node_operating_system: 'windows',
  },
];

const SUT = (props: Partial<React.ComponentProps<typeof CollectorList>>) => (
  <CollectorList collectors={collectors}
                 onClone={jest.fn()}
                 onDelete={jest.fn()}
                 onPageChange={jest.fn()}
                 onQueryChange={jest.fn()}
                 validateCollector={jest.fn()}
                 pagination={{ page: 1, pageSize: 10, total: 1 }}
                 query=""
                 total={collectors.length}
                 {...props} />
);

describe('CollectorList', () => {
  it('renders list of collectors', async () => {
    render(<SUT />);

    await screen.findByText('auditbeat');
    await screen.findAllByText('filebeat');
    await screen.findAllByText('nxlog');
    await screen.findAllByText('winlogbeat');
  });

  it('triggers `onQueryChange` when query is changed', async () => {
    const onQueryChange = jest.fn();
    render(<SUT onQueryChange={onQueryChange} />);

    const queryInput = await screen.findByPlaceholderText('Find collectors');
    userEvent.type(queryInput, 'newquery{enter}');

    await waitFor(() => {
      expect(onQueryChange).toHaveBeenCalledWith('newquery', expect.any(Function));
    });
  });

  it('triggers `onPageChange` when page size is changed', async () => {
    const onPageChange = jest.fn();
    render(<SUT onPageChange={onPageChange} />);

    const pageDropdown = await screen.findByRole('button', { name: /configure page size/i });
    userEvent.click(pageDropdown);

    userEvent.click(await screen.findByRole('menuitem', { name: '25' }));

    await waitFor(() => {
      expect(onPageChange).toHaveBeenCalledWith(1, 25);
    });
  });
});
