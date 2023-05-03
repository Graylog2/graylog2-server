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
import { renderPreflight, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import DataNodesOverview from 'preflight/components/Setup/DataNodesOverview';
import useDataNodes from 'preflight/hooks/useDataNodes';
import { asMock } from 'helpers/mocking';
import FetchError from 'logic/errors/FetchError';

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
  let oldConfirm;

  beforeEach(() => {
    asMock(useDataNodes).mockReturnValue({
      data: availableDataNodes,
      isFetching: false,
      isInitialLoading: false,
      error: undefined,
    });

    oldConfirm = window.confirm;
    window.confirm = jest.fn(() => true);
  });

  afterEach(() => {
    window.confirm = oldConfirm;
  });

  it('should list available data nodes', async () => {
    renderPreflight(<DataNodesOverview onResumeStartup={() => {}} />);

    await screen.findByText('node-id-3');
    await screen.findByText('http://localhost:9200');
  });

  it('should resume startup', async () => {
    const onResumeStartup = jest.fn();
    renderPreflight(<DataNodesOverview onResumeStartup={onResumeStartup} />);

    await screen.findByText('node-id-3');

    const resumeStartupButton = screen.getByRole('button', {
      name: /resume startup/i,
    });

    userEvent.click(resumeStartupButton);

    await waitFor(() => expect(onResumeStartup).toHaveBeenCalledTimes(1));
  });

  it('should display error message when there was an error fetching data nodes', async () => {
    asMock(useDataNodes).mockReturnValue({
      data: [],
      isFetching: false,
      isInitialLoading: false,
      error: new FetchError('The request error message', 500, { status: 500, body: { message: 'The request error message' } }),
    });

    renderPreflight(<DataNodesOverview onResumeStartup={() => {}} />);
    await screen.findByText(/There was an error fetching the data nodes:/);
    await screen.findByText(/There was an error fetching a resource: The request error message. Additional information: Not available/);
  });
});
