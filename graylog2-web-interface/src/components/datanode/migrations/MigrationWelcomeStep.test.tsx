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

import useDataNodes from 'components/datanode/hooks/useDataNodes';
import type { DataNode, MigrationState } from 'components/datanode/Types';
import { asMock } from 'helpers/mocking';

import MigrationWelcomeStep from './MigrationWelcomeStep';

jest.mock('components/datanode/hooks/useDataNodes', () =>
  jest.fn(() => ({
    data: {
      attributes: [],
      list: [],
      pagination: {
        query: '',
        page: 1,
        per_page: 0,
        total: 0,
        count: 0,
      },
    },
    refetch: () => {},
    isLoading: false,
    error: null,
  })),
);

jest.mock('components/datanode/hooks/useIsElasticsearch', () => jest.fn(() => false));
jest.mock('components/datanode/hooks/useMigrationState', () =>
  jest.fn(() => ({
    currentStep: {
      state: 'MIGRATION_WELCOME_PAGE',
      next_steps: ['SHOW_CA_CREATION'],
    },
    isLoading: false,
  })),
);

const currentStep = {
  state: 'MIGRATION_WELCOME_PAGE',
  next_steps: ['SHOW_CA_CREATION'],
  error_message: null,
  response: null,
} as MigrationState;

const dataNode: DataNode = {
  hostname: 'data-node',
  id: 'data-node-id',
  is_leader: false,
  is_master: false,
  last_seen: '2026-09-22T00:00:00.000Z',
  node_id: 'data-node-id',
  short_node_id: 'data-node',
  transport_address: '127.0.0.1:9300',
  type: 'DATA',
  status: 'CONNECTED',
  cert_valid_until: null,
  datanode_version: '7.2.0',
  version_compatible: true,
  cluster_address: 'http://localhost:9200',
  rest_api_address: 'http://localhost:8999',
  action_queue: '',
};

describe('MigrationWelcomeStep', () => {
  it('disables the next button when no Data Nodes are available', async () => {
    render(<MigrationWelcomeStep onTriggerStep={async () => currentStep} currentStep={currentStep} />);

    expect(await screen.findByRole('button', { name: 'Next' })).toBeDisabled();
  });

  it('enables the next button when a Data Node is available', async () => {
    asMock(useDataNodes).mockReturnValue({
      data: {
        attributes: [],
        list: [dataNode],
        pagination: {
          query: '',
          page: 1,
          per_page: 0,
          total: 1,
          count: 1,
        },
      },
      refetch: jest.fn(),
      isLoading: false,
      error: null,
    });

    render(<MigrationWelcomeStep onTriggerStep={async () => currentStep} currentStep={currentStep} />);

    expect(await screen.findByRole('button', { name: 'Next' })).toBeEnabled();
  });
});
