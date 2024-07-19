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
import { useQueryParam } from 'use-query-params';

import type { MigrationState, MigrationStateItem } from 'components/datanode/Types';
import { asMock } from 'helpers/mocking';

import RemoteReindexingMigration from './RemoteReindexingMigration';

import { MIGRATION_STATE } from '../Constants';

jest.mock('use-query-params', () => ({
  ...jest.requireActual('use-query-params'),
  useQueryParam: jest.fn(),
}));

jest.mock('routing/useLocation', () => jest.fn(() => ({ search: '' })));

jest.mock('components/datanode/hooks/useCompatibilityCheck', () => jest.fn(() => ({
  data: {
    opensearch_version: '2.10.0',
    info: {
      nodes: [{
        indices: [{
          index_id: 'prlnhUp_TvSof9U-K3FZ9A',
          shards: [{ documents_count: 10, name: 'S0', primary: true, min_lucene_version: '9.7.0' }],
          index_name: '.opendistro_security',
          creation_date: '2023-11-17T09:57:36.511',
          index_version_created: '2.10.0',
        }],
      }],
      opensearch_data_location: '/home/tdvorak/bin/datanode/data',
    },
    compatibility_errors: [],
  },
  isFetching: false,
  isInitialLoading: false,
  error: undefined,
})));

jest.mock('components/datanode/hooks/useDataNodes', () => jest.fn(() => ({
  data: {
    attributes: [],
    list: [{
      cert_valid_until: '2053-11-02T13:20:58',
      error_msg: null,
      hostname: 'datanode1',
      node_id: '3af165ef-87a9-467f-b7db-435f4748eb75',
      short_node_id: '3af165ef',
      status: 'CONNECTED' as any,
      transport_address: 'http://datanode1:9200',
      type: 'DATANODE',
      id: '1',
      is_leader: true,
      is_master: true,
      last_seen: '2053-11-02T13:20:58',
    }],
    pagination: {
      query: '',
      page: 1,
      per_page: 0,
      total: 0,
      count: 0,
    },
  },
  refetch: () => {
  },
  isInitialLoading: false,
  error: null,
})));

const renderStep = (_state: MigrationStateItem) => {
  const getCurrentStep = (state: MigrationStateItem) => ({
    state,
    next_steps: [],
    error_message: null,
    response: null,
  } as MigrationState);

  render(<RemoteReindexingMigration onTriggerStep={async () => ({} as MigrationState)}
                                    currentStep={getCurrentStep(_state)} />);
};

describe('RemoteReindexingMigration', () => {
  beforeEach(() => {
    asMock(useQueryParam).mockImplementation(() => ([undefined, () => {}]));
  });

  it('should render Welcome step', async () => {
    renderStep(MIGRATION_STATE.REMOTE_REINDEX_WELCOME_PAGE.key);

    await screen.findByRole('button', {
      name: /1. Remote reindexing migration/i,
    });

    await screen.findByRole('heading', { name: /Welcome/i });
  });

  it('should render CertificatesProvisioning running step', async () => {
    renderStep(MIGRATION_STATE.PROVISION_DATANODE_CERTIFICATES_RUNNING.key);

    await screen.findByRole('button', {
      name: /2. Provision the Data Node's certificate./i,
    });
  });

  it('should render ExistingDataMigrationQuestion step', async () => {
    renderStep(MIGRATION_STATE.EXISTING_DATA_MIGRATION_QUESTION_PAGE.key);

    await screen.findByRole('button', {
      name: /3. Migrate existing data question/i,
    });

    await screen.findByText(/Do you want to migrate your existing data?/);
    await screen.findByText(/line from your Graylog configuration file/);
  });

  it('should render MigrateExistingData step', async () => {
    renderStep(MIGRATION_STATE.MIGRATE_EXISTING_DATA.key);

    await screen.findByRole('button', {
      name: /4. Migrate existing data/i,
    });

    await screen.findByLabelText(/Hostname/);
    await screen.findByLabelText(/Username/);
    await screen.findByLabelText(/Password/);
    await screen.findByLabelText(/Allowlist/);
  });

  it('should render RemoteReindexRunning step', async () => {
    renderStep(MIGRATION_STATE.REMOTE_REINDEX_RUNNING.key);

    await screen.findByRole('button', {
      name: /5. Remote reindexing migration running/i,
    });

    await screen.findByText(/We are currently migrating your existing data asynchronically/);
  });

  it('should render ShutdownClusterStep step', async () => {
    renderStep(MIGRATION_STATE.ASK_TO_SHUTDOWN_OLD_CLUSTER.key);

    await screen.findByRole('button', {
      name: /6. Shut down old cluster/i,
    });

    await screen.findByText(/To finish please shut down your/);
  });

  it('should render RemoteReindexRunning step and show Logs', async () => {
    asMock(useQueryParam).mockImplementation((field: string) => {
      const value = field === 'show_logs' ? 'true' : undefined;

      return [value, () => {}];
    });

    renderStep(MIGRATION_STATE.REMOTE_REINDEX_RUNNING.key);

    await screen.findByText(/Remote Reindex Migration Logs/);
  });
});
