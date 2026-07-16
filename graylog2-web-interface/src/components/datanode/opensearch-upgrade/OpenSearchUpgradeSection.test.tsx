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
import { render, screen, waitFor, within } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import asMock from 'helpers/mocking/AsMock';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useIncompatibleIndices from 'components/indices/hooks/useIncompatibleIndices';

import OpenSearchUpgradeSection from './OpenSearchUpgradeSection';
import useOpenSearchClusterStats from './hooks/useOpenSearchClusterStats';
import useOpenSearchRollingRestart, {
  rollingRestartStartError,
  useCurrentRollingRestart,
} from './hooks/useOpenSearchRollingRestart';
import type { RollingRestartJob, RollingRestartState } from './rollingRestartTypes';

jest.mock('./IncompatibleIndicesTable', () => ({
  __esModule: true,
  default: () => <div>incompatible-indices-stub</div>,
}));
jest.mock('./OpenSearchRollingUpgradeNodes', () => ({
  __esModule: true,
  default: () => <div>rolling-upgrade-nodes-stub</div>,
}));
jest.mock('./hooks/useOpenSearchClusterStats');
jest.mock('./hooks/useOpenSearchRollingRestart', () => ({
  __esModule: true,
  default: jest.fn(),
  rollingRestartStartError: jest.fn(),
  useCurrentRollingRestart: jest.fn(),
}));
jest.mock('components/indices/hooks/useIncompatibleIndices');
jest.mock('logic/telemetry/useSendTelemetry');

const EVENTS = TELEMETRY_EVENT_TYPE.DATANODE_OPENSEARCH_UPGRADE;

const mockClusterStats = (overrides: Partial<ReturnType<typeof useOpenSearchClusterStats>> = {}) =>
  asMock(useOpenSearchClusterStats).mockReturnValue({
    currentVersion: '2.19.5',
    targetVersion: '3.5.0',
    nodes: [],
    numberOfDataNodes: 3,
    unavailableDataNodeCount: 0,
    isError: false,
    isFetching: false,
    isLoading: false,
    isUpgradeAvailable: true,
    refetch: jest.fn(),
    ...overrides,
  } as ReturnType<typeof useOpenSearchClusterStats>);

type RollingRestartHookOverrides = {
  data?: RollingRestartJob | null;
  isResumingRollingRestart?: boolean;
  isStartingRollingRestart?: boolean;
  resumeRollingRestart?: jest.Mock;
  startRollingRestart?: jest.Mock;
};

const mockRollingRestart = (overrides: RollingRestartHookOverrides = {}) => {
  asMock(useCurrentRollingRestart).mockReturnValue({
    data: overrides.data ?? null,
    isLoading: false,
    refetch: jest.fn(),
  } as unknown as ReturnType<typeof useCurrentRollingRestart>);

  return asMock(useOpenSearchRollingRestart).mockReturnValue({
    data: null,
    isResumingRollingRestart: false,
    isStartingRollingRestart: false,
    resumeRollingRestart: jest.fn(() => Promise.resolve()),
    startRollingRestart: jest.fn(() => Promise.resolve()),
    ...overrides,
  } as unknown as ReturnType<typeof useOpenSearchRollingRestart>);
};

const mockIncompatibleIndices = (data: Array<unknown> = [], overrides: { isLoading?: boolean; isError?: boolean } = {}) =>
  asMock(useIncompatibleIndices).mockReturnValue({
    data,
    isError: false,
    isLoading: false,
    refetch: jest.fn(),
    ...overrides,
  } as unknown as ReturnType<typeof useIncompatibleIndices>);

const pausedJob = (): RollingRestartJob => ({
  job_definition_type: 'rolling-restart-v1',
  job_definition_id: 'job-definition-id',
  status: 'paused',
  created_at: '2026-01-01T00:00:00.000Z',
  updated_at: '2026-01-01T00:00:00.000Z',
  next_time: null,
  data: {
    type: 'rolling-restart-v1',
    sm_state: 'PAUSED_WAITING_GREEN' as RollingRestartState,
    nodes: [],
    current_node_index: -1,
    abort_requested: false,
    triggered_by: 'admin',
    paused_reason: 'Cluster did not return to GREEN within 30 minutes.',
    waiting_green_since: '2026-01-01T00:00:00.000Z',
  },
});

const failedJob = (): RollingRestartJob => {
  const job = pausedJob();

  return { ...job, status: 'error', data: { ...job.data, sm_state: 'FAILED', paused_reason: null } };
};

describe('OpenSearchUpgradeSection', () => {
  let sendTelemetry: jest.Mock;

  beforeEach(() => {
    sendTelemetry = jest.fn();
    asMock(useSendTelemetry).mockReturnValue(sendTelemetry);
    mockClusterStats();
    mockRollingRestart();
    mockIncompatibleIndices([]);
  });

  it('confirms before starting a rolling upgrade, then starts and sends telemetry', async () => {
    const startRollingRestart = jest.fn(() => Promise.resolve());
    mockRollingRestart({ startRollingRestart });
    render(<OpenSearchUpgradeSection />);

    await userEvent.click(screen.getByRole('button', { name: /start opensearch rolling upgrade/i }));

    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText(/restarts every data node, one at a time/i)).toBeInTheDocument();
    expect(startRollingRestart).not.toHaveBeenCalled();

    await userEvent.click(within(dialog).getByRole('button', { name: /^start rolling upgrade$/i }));

    expect(startRollingRestart).toHaveBeenCalledWith(false);
    expect(sendTelemetry).toHaveBeenCalledWith(EVENTS.ROLLING_UPGRADE_STARTED, expect.anything());
  });

  it('does not start the rolling upgrade when the confirmation is cancelled', async () => {
    const startRollingRestart = jest.fn(() => Promise.resolve());
    mockRollingRestart({ startRollingRestart });
    render(<OpenSearchUpgradeSection />);

    await userEvent.click(screen.getByRole('button', { name: /start opensearch rolling upgrade/i }));
    const dialog = await screen.findByRole('dialog');
    await userEvent.click(within(dialog).getByRole('button', { name: /cancel/i }));

    expect(startRollingRestart).not.toHaveBeenCalled();
    expect(sendTelemetry).not.toHaveBeenCalledWith(EVENTS.ROLLING_UPGRADE_STARTED, expect.anything());
  });

  it('offers an enabled Restart action below the rolling-upgrade node threshold', () => {
    mockClusterStats({ numberOfDataNodes: 2 });
    render(<OpenSearchUpgradeSection />);

    expect(screen.getByRole('button', { name: /^restart$/i })).toBeEnabled();
    expect(screen.queryByRole('button', { name: /start opensearch rolling upgrade/i })).not.toBeInTheDocument();
    expect(screen.queryByText(/requires at least 3 data nodes/i)).not.toBeInTheDocument();
  });

  it('warns about downtime and the journal when restarting below the 3-node threshold', async () => {
    mockClusterStats({ numberOfDataNodes: 2 });
    render(<OpenSearchUpgradeSection />);

    await userEvent.click(screen.getByRole('button', { name: /^restart$/i }));

    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText(/full restart, not a rolling one/i)).toBeInTheDocument();
    expect(within(dialog).getByText(/disk journal/i)).toBeInTheDocument();
    expect(within(dialog).queryByText(/one at a time/i)).not.toBeInTheDocument();
  });

  it('shows the data node count with how many are available', () => {
    mockClusterStats({ numberOfDataNodes: 2, unavailableDataNodeCount: 1 });
    render(<OpenSearchUpgradeSection />);

    expect(screen.getByText('Data Nodes:')).toBeInTheDocument();
    expect(screen.getByText('3 (2 available)')).toBeInTheDocument();
  });

  it('replaces the operational panels with a warning when versions look up to date but nodes are unavailable', () => {
    mockClusterStats({ isUpgradeAvailable: false, numberOfDataNodes: 2, unavailableDataNodeCount: 1 });
    mockRollingRestart({ data: failedJob() });
    render(<OpenSearchUpgradeSection />);

    expect(screen.getByText(/cannot be checked while 1 data node is unavailable/i)).toBeInTheDocument();
    expect(screen.getByText('3 (2 available)')).toBeInTheDocument();
    expect(screen.queryByText('incompatible-indices-stub')).not.toBeInTheDocument();
    expect(screen.queryByText('rolling-upgrade-nodes-stub')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /start opensearch rolling upgrade/i })).not.toBeInTheDocument();
  });

  it('keeps an active rolling upgrade visible while versions look up to date but nodes are unavailable', () => {
    mockClusterStats({ isUpgradeAvailable: false, numberOfDataNodes: 2, unavailableDataNodeCount: 1 });
    mockRollingRestart({ data: pausedJob() });
    render(<OpenSearchUpgradeSection />);

    expect(screen.getByText('rolling-upgrade-nodes-stub')).toBeInTheDocument();
    expect(screen.queryByText(/cannot be checked while/i)).not.toBeInTheDocument();
  });

  it('disables the start action while incompatible indices remain', () => {
    mockIncompatibleIndices([{ index_name: 'graylog_0' }]);
    render(<OpenSearchUpgradeSection />);

    expect(screen.getByRole('button', { name: /start opensearch rolling upgrade/i })).toBeDisabled();
    expect(screen.getByText(/resolve all incompatible indices first/i)).toBeInTheDocument();
  });

  it('disables the start action when the incompatible indices check failed', () => {
    mockIncompatibleIndices([], { isError: true });
    render(<OpenSearchUpgradeSection />);

    expect(screen.getByRole('button', { name: /start opensearch rolling upgrade/i })).toBeDisabled();
  });

  it('shows the rolling-upgrade status when a job exists and no incompatible indices remain', () => {
    mockRollingRestart({ data: pausedJob() });
    render(<OpenSearchUpgradeSection />);

    expect(screen.getByText('rolling-upgrade-nodes-stub')).toBeInTheDocument();
  });

  it('hides incompatible indices and keeps an active rolling upgrade visible even while incompatible indices remain', () => {
    mockIncompatibleIndices([{ index_name: 'graylog_0' }]);
    mockRollingRestart({ data: pausedJob() });
    render(<OpenSearchUpgradeSection />);

    expect(screen.queryByText('incompatible-indices-stub')).not.toBeInTheDocument();
    expect(screen.queryByText(/resolve all incompatible indices first/i)).not.toBeInTheDocument();
    expect(screen.getByText('rolling-upgrade-nodes-stub')).toBeInTheDocument();
  });

  it('hides incompatible indices and keeps the status visible for a finished (terminal) rolling upgrade', () => {
    mockIncompatibleIndices([{ index_name: 'graylog_0' }]);
    mockRollingRestart({ data: failedJob() });
    render(<OpenSearchUpgradeSection />);

    expect(screen.queryByText('incompatible-indices-stub')).not.toBeInTheDocument();
    expect(screen.queryByText(/resolve all incompatible indices first/i)).not.toBeInTheDocument();
    expect(screen.getByText('rolling-upgrade-nodes-stub')).toBeInTheDocument();
  });

  it('keeps a finished rolling-upgrade status visible independent of the incompatible indices check', () => {
    mockIncompatibleIndices([], { isLoading: true });
    mockRollingRestart({ data: failedJob() });
    render(<OpenSearchUpgradeSection />);

    expect(screen.getByText('rolling-upgrade-nodes-stub')).toBeInTheDocument();
  });

  it('keeps an active rolling upgrade visible while the incompatible indices check is failing', () => {
    mockIncompatibleIndices([], { isError: true });
    mockRollingRestart({ data: pausedJob() });
    render(<OpenSearchUpgradeSection />);

    expect(screen.getByText('rolling-upgrade-nodes-stub')).toBeInTheDocument();
  });

  it('shows a resume action for a paused upgrade and sends telemetry', async () => {
    const resumeRollingRestart = jest.fn(() => Promise.resolve());
    mockRollingRestart({ data: pausedJob(), resumeRollingRestart });
    render(<OpenSearchUpgradeSection />);

    expect(screen.queryByRole('button', { name: /start opensearch rolling upgrade/i })).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /resume/i }));

    expect(resumeRollingRestart).toHaveBeenCalled();
    expect(sendTelemetry).toHaveBeenCalledWith(EVENTS.ROLLING_UPGRADE_RESUMED, expect.anything());
  });

  it('opens the force-start dialog on an overridable failure and force-starts on confirm', async () => {
    const startRollingRestart = jest.fn(() => Promise.reject(new Error('precondition')));
    mockRollingRestart({ startRollingRestart });
    asMock(rollingRestartStartError).mockReturnValue({
      canRetryWithForce: true,
      failedChecks: ['Cluster status is YELLOW — must be GREEN'],
      message: 'Cluster status is YELLOW — must be GREEN',
    });
    render(<OpenSearchUpgradeSection />);

    await userEvent.click(screen.getByRole('button', { name: /start opensearch rolling upgrade/i }));

    const confirmDialog = await screen.findByRole('dialog');
    await userEvent.click(within(confirmDialog).getByRole('button', { name: /^start rolling upgrade$/i }));

    const forceDialog = await screen.findByRole('dialog');
    expect(within(forceDialog).getByText(/cluster status is yellow/i)).toBeInTheDocument();

    await userEvent.click(within(forceDialog).getByRole('button', { name: /start anyway/i }));

    await waitFor(() => expect(startRollingRestart).toHaveBeenCalledWith(true));
    expect(sendTelemetry).toHaveBeenCalledWith(EVENTS.ROLLING_UPGRADE_FORCE_STARTED, expect.anything());
  });
});
