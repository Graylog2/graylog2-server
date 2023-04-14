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

import fetch from 'logic/rest/FetchProvider';
import DataNodesOverview from 'preflight/components/DataNodesOverview';
import useDataNodes from 'preflight/hooks/useDataNodes';
import { asMock } from 'helpers/mocking';

jest.mock('preflight/hooks/useDataNodes');
jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));

const availableDataNodes = [
  {
    hostname: '192.168.0.10',
    id: 'data-node-id-1',
    is_leader: false,
    is_master: false,
    last_seen: '2020-01-10T10:40:00.000Z',
    node_id: 'node-id-complete-1',
    short_node_id: 'node-id-1',
    transport_address: 'http://localhost:9200',
    type: 'DATANODE',
  },
  {
    hostname: '192.168.0.11',
    id: 'data-node-id-2',
    is_leader: false,
    is_master: false,
    last_seen: '2020-01-10T10:40:00.000Z',
    node_id: 'node-id-complete-2',
    short_node_id: 'node-id-2',
    transport_address: 'http://localhost:9201',
    type: 'DATANODE',
  },
  {
    hostname: '192.168.0.12',
    id: 'data-node-id-3',
    is_leader: false,
    is_master: false,
    last_seen: '2020-01-10T10:40:00.000Z',
    node_id: 'node-id-complete-3',
    short_node_id: 'node-id-3',
    transport_address: 'http://localhost:9202',
    type: 'DATANODE',
  },
];

describe('DataNodesOverview', () => {
  beforeEach(() => {
    asMock(useDataNodes).mockReturnValue({
      data: availableDataNodes,
      isFetching: false,
      isInitialLoading: false,
      error: undefined,
    });
  });

  it('should list available data nodes', async () => {
    render(<DataNodesOverview />);

    await screen.findByText('node-id-3');
    await screen.findByText('http://localhost:9200');
  });

  it('should resume startup', async () => {
    render(<DataNodesOverview />);

    await screen.findByText('node-id-3');

    const resumeStartupButton = screen.getByRole('button', {
      name: /resume startup/i,
    });

    userEvent.click(resumeStartupButton);

    await waitFor(() => expect(fetch).toHaveBeenCalledWith('POST', expect.stringContaining('/api/status/finish-config'), undefined, false));
  });
});
