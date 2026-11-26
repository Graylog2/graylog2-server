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

import OpenSearchRollingUpgradeNodes from './OpenSearchRollingUpgradeNodes';
import type {
  RollingRestartData,
  RollingRestartJob,
  RollingRestartNode,
  RollingRestartNodeStatus,
  RollingRestartState,
} from './rollingRestartTypes';

const nodeWithStatus = (status: RollingRestartNodeStatus, datanodeId: string = 'node-1'): RollingRestartNode => ({
  hostname: `${datanodeId}.example.org`,
  datanode_id: datanodeId,
  status,
  attempts: 1,
});

const jobWithState = (smState: string, nodes: Array<RollingRestartNode> = []): RollingRestartJob => ({
  job_definition_type: 'rolling-restart-v1',
  job_definition_id: 'def-1',
  status: 'running',
  created_at: '2026-01-01T00:00:00.000Z',
  updated_at: '2026-01-01T00:00:00.000Z',
  next_time: null,
  data: {
    type: 'rolling-restart-v1',
    sm_state: smState as RollingRestartState,
    nodes,
    current_node_index: -1,
    abort_requested: false,
    triggered_by: 'admin',
    waiting_green_since: '2026-01-01T00:00:00.000Z',
  } as RollingRestartData,
});

describe('OpenSearchRollingUpgradeNodes', () => {
  it('renders the human label for a known state', () => {
    render(<OpenSearchRollingUpgradeNodes job={jobWithState('UPGRADING_NODE')} versionNodes={[]} />);

    expect(screen.getByText('Upgrading node')).toBeInTheDocument();
  });

  it('falls back to a generic label for an unknown backend state instead of an empty label', () => {
    render(<OpenSearchRollingUpgradeNodes job={jobWithState('SOME_FUTURE_STATE')} versionNodes={[]} />);

    expect(screen.getByText('In progress')).toBeInTheDocument();
  });

  it('shows a progress indicator while running', () => {
    render(<OpenSearchRollingUpgradeNodes job={jobWithState('UPGRADING_NODE')} versionNodes={[]} />);

    expect(screen.getByTestId('rolling-upgrade-progress')).toBeInTheDocument();
  });

  it('hides the progress indicator once terminal', () => {
    render(<OpenSearchRollingUpgradeNodes job={jobWithState('COMPLETED')} versionNodes={[]} />);

    expect(screen.queryByTestId('rolling-upgrade-progress')).not.toBeInTheDocument();
  });

  it('hides the progress indicator while paused, awaiting resume', () => {
    render(<OpenSearchRollingUpgradeNodes job={jobWithState('PAUSED_WAITING_GREEN')} versionNodes={[]} />);

    expect(screen.queryByTestId('rolling-upgrade-progress')).not.toBeInTheDocument();
  });

  it('shows a progress indicator on the in-flight node while running', () => {
    const nodes = [nodeWithStatus('RESTARTING', 'node-1'), nodeWithStatus('PENDING', 'node-2')];
    render(<OpenSearchRollingUpgradeNodes job={jobWithState('WAITING_NODE_JOINED', nodes)} versionNodes={[]} />);

    expect(screen.getAllByTestId('node-upgrade-progress')).toHaveLength(1);
  });

  it('hides node progress indicators while paused — a frozen status must not read as ongoing work', () => {
    const nodes = [nodeWithStatus('STARTED')];
    render(<OpenSearchRollingUpgradeNodes job={jobWithState('PAUSED_WAITING_GREEN', nodes)} versionNodes={[]} />);

    expect(screen.queryByTestId('node-upgrade-progress')).not.toBeInTheDocument();
  });

  it('describes a rolling restart without target version as a restart instead of an upgrade', () => {
    const job = jobWithState('UPGRADING_NODE', [
      nodeWithStatus('COMPLETED', 'node-1'),
      nodeWithStatus('PENDING', 'node-2'),
    ]);
    render(
      <OpenSearchRollingUpgradeNodes
        job={{ ...job, data: { ...job.data, target_opensearch_version: null } }}
        versionNodes={[]}
      />,
    );

    expect(screen.getByRole('heading', { name: /^Data Node rolling restart/ })).toBeInTheDocument();
    expect(screen.getByText('Restarting node')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Waiting for restart' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Restarted' })).toBeInTheDocument();
    expect(screen.queryByText(/upgrade/i)).not.toBeInTheDocument();
  });

  it('shows the running OpenSearch version of a restarted node instead of the available one', () => {
    const job = jobWithState('COMPLETED', [nodeWithStatus('COMPLETED')]);
    render(
      <OpenSearchRollingUpgradeNodes
        job={{ ...job, data: { ...job.data, target_opensearch_version: null } }}
        versionNodes={[{ node_id: 'node-1', current_version: '2.19.0', available_version: '3.1.0', upgradeable: true }]}
      />,
    );

    expect(screen.getByRole('cell', { name: '2.19.0' })).toBeInTheDocument();
    expect(screen.queryByText('3.1.0')).not.toBeInTheDocument();
  });

  it('keeps describing a rolling restart with target version as an upgrade', () => {
    const job = jobWithState('UPGRADING_NODE', [nodeWithStatus('COMPLETED')]);
    render(
      <OpenSearchRollingUpgradeNodes
        job={{ ...job, data: { ...job.data, target_opensearch_version: '3.1.0' } }}
        versionNodes={[]}
      />,
    );

    expect(screen.getByRole('heading', { name: /^OpenSearch rolling upgrade/ })).toBeInTheDocument();
    expect(screen.getByText('Upgrading node')).toBeInTheDocument();
    expect(screen.getByText('Upgraded')).toBeInTheDocument();
  });
});
