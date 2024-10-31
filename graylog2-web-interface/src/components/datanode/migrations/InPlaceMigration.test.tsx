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

import type { MigrationState, MigrationStateItem } from 'components/datanode/Types';

import InPlaceMigration from './InPlaceMigration';

import { MIGRATION_STATE } from '../Constants';

jest.mock('components/datanode/hooks/useCompatibilityCheck', () => jest.fn(() => ({
  data: {
    datanode1: {
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
  refetch: () => {},
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

  render(<InPlaceMigration onTriggerStep={async () => ({} as MigrationState)} currentStep={getCurrentStep(_state)} />);
};

describe('InPlaceMigration', () => {
  it('should render In-Place migration Welcome step', async () => {
    renderStep(MIGRATION_STATE.ROLLING_UPGRADE_MIGRATION_WELCOME_PAGE.key);

    await screen.findByRole('button', {
      name: /1. Welcome to In-Place migration/i,
    });

    await screen.findByRole('heading', { name: /Welcome/i });
  });

  it('should render CompatibilityCheckStep step', async () => {
    renderStep(MIGRATION_STATE.DIRECTORY_COMPATIBILITY_CHECK_PAGE.key);

    await screen.findByRole('button', {
      name: /2. Directory compatibility check/i,
    });
  });

  it('should render CertificatesProvisioning running step', async () => {
    renderStep(MIGRATION_STATE.PROVISION_ROLLING_UPGRADE_NODES_RUNNING.key);

    await screen.findByRole('button', {
      name: /3. Provision the Data Node's certificate./i,
    });
  });

  it('should render JournalDowntimeWarning step', async () => {
    renderStep(MIGRATION_STATE.JOURNAL_SIZE_DOWNTIME_WARNING.key);

    await screen.findByRole('button', {
      name: /4. Journal size downtime warning/i,
    });

    await screen.findByRole('heading', { name: /Journal downtime size warning/i });
  });

  it('should render StopMessageProcessing step', async () => {
    renderStep(MIGRATION_STATE.MESSAGE_PROCESSING_STOP.key);

    await screen.findByRole('button', {
      name: /5. Stop message processing/i,
    });

    await screen.findByRole('heading', { name: /Stop OpenSearch/i });
  });

  it('should render RestartGraylog step', async () => {
    renderStep(MIGRATION_STATE.RESTART_GRAYLOG.key);

    await screen.findByRole('button', {
      name: /6. Update configuration file and restart Graylog/i,
    });

    await screen.findByText(/please restart Graylog to finish the migration./);
  });
});
