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
import { render, screen } from 'wrappedTestingLibrary';

import asMock from 'helpers/mocking/AsMock';
import useDataNodeUpgradeStatus from 'components/datanode/hooks/useDataNodeUpgradeStatus';
import useOpenSearchClusterStats from 'components/datanode/opensearch-upgrade/hooks/useOpenSearchClusterStats';
import { useCurrentRollingRestart } from 'components/datanode/opensearch-upgrade/hooks/useOpenSearchRollingRestart';
import type {
  RollingRestartJob,
  RollingRestartState,
} from 'components/datanode/opensearch-upgrade/rollingRestartTypes';

import DataNodeUpgradePage from './DataNodeUpgradePage';

jest.mock('components/datanode/hooks/useDataNodeUpgradeStatus', () => ({
  __esModule: true,
  default: jest.fn(),
  getNodeToUpgrade: jest.fn(() => null),
  saveNodeToUpgrade: jest.fn(),
  startShardReplication: jest.fn(),
  stopShardReplication: jest.fn(),
}));
jest.mock('components/datanode/opensearch-upgrade/hooks/useOpenSearchClusterStats');
jest.mock('components/datanode/opensearch-upgrade/hooks/useOpenSearchRollingRestart', () => ({
  __esModule: true,
  default: jest.fn(),
  useCurrentRollingRestart: jest.fn(),
}));
jest.mock('components/datanode/opensearch-upgrade/OpenSearchUpgradeSection', () => ({
  __esModule: true,
  default: () => <div>opensearch-upgrade-section-stub</div>,
}));
jest.mock('components/cluster-configuration/ClusterConfigurationPageNavigation', () => ({
  __esModule: true,
  default: () => <div>page-navigation-stub</div>,
}));
jest.mock('components/datanode/data-node-upgrade/ClusterHealthInfo', () => ({
  __esModule: true,
  default: () => <div>cluster-health-stub</div>,
}));

const mockUpgradeStatus = () =>
  asMock(useDataNodeUpgradeStatus).mockReturnValue({
    data: {
      outdated_nodes: [],
      up_to_date_nodes: [{ hostname: 'data-node-1' }],
      shard_replication_enabled: true,
      warnings: [],
    },
    isInitialLoading: false,
  } as unknown as ReturnType<typeof useDataNodeUpgradeStatus>);

const mockClusterStats = (overrides: Partial<ReturnType<typeof useOpenSearchClusterStats>> = {}) =>
  asMock(useOpenSearchClusterStats).mockReturnValue({
    currentVersion: '3.5.0',
    targetVersion: '3.5.0',
    nodes: [],
    numberOfDataNodes: 3,
    unavailableDataNodeCount: 0,
    isError: false,
    isFetching: false,
    isLoading: false,
    isUpgradeAvailable: false,
    refetch: jest.fn(),
    ...overrides,
  } as ReturnType<typeof useOpenSearchClusterStats>);

const mockRollingRestart = (data: RollingRestartJob | null = null, isLoading: boolean = false) =>
  asMock(useCurrentRollingRestart).mockReturnValue({
    data,
    isLoading,
    refetch: jest.fn(),
  } as unknown as ReturnType<typeof useCurrentRollingRestart>);

const rollingRestartJob = (smState: RollingRestartState): RollingRestartJob => ({
  job_definition_type: 'rolling-restart-v1',
  job_definition_id: 'job-definition-id',
  status: 'running',
  created_at: '2026-01-01T00:00:00.000Z',
  updated_at: '2026-01-01T00:00:00.000Z',
  next_time: null,
  data: {
    type: 'rolling-restart-v1',
    sm_state: smState,
    nodes: [],
    current_node_index: -1,
    abort_requested: false,
    triggered_by: 'admin',
    waiting_green_since: '2026-01-01T00:00:00.000Z',
  },
});

describe('DataNodeUpgradePage', () => {
  beforeEach(() => {
    mockUpgradeStatus();
    mockClusterStats();
    mockRollingRestart();
  });

  it('shows the OpenSearch upgrade section when an upgrade is available', async () => {
    mockClusterStats({ isUpgradeAvailable: true, targetVersion: '3.6.0' });

    render(<DataNodeUpgradePage />);

    await screen.findByText('opensearch-upgrade-section-stub');
  });

  it('keeps the section visible while a rolling upgrade is active even though versions read up to date', async () => {
    mockRollingRestart(rollingRestartJob('WAITING_GREEN'));

    render(<DataNodeUpgradePage />);

    await screen.findByText('opensearch-upgrade-section-stub');
    await screen.findByText(/rolling upgrade is in progress/i);
    expect(screen.queryByText(/embedded opensearch is up to date/i)).not.toBeInTheDocument();
  });

  it('hides the section and reports up to date once the rolling upgrade completed', async () => {
    mockRollingRestart(rollingRestartJob('COMPLETED'));

    render(<DataNodeUpgradePage />);

    await screen.findByText(/embedded opensearch is up to date/i);
    expect(screen.queryByText('opensearch-upgrade-section-stub')).not.toBeInTheDocument();
  });

  it('does not claim up to date while the rolling upgrade state is still loading', async () => {
    mockRollingRestart(null, true);

    render(<DataNodeUpgradePage />);

    await screen.findByText(/checking opensearch status/i);
    expect(screen.queryByText(/embedded opensearch is up to date/i)).not.toBeInTheDocument();
    expect(screen.queryByText('opensearch-upgrade-section-stub')).not.toBeInTheDocument();
  });
});
