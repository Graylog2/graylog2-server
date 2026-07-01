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
import useOutdatedIndices from 'components/indices/hooks/useOutdatedIndices';

import OpenSearchUpgradeSection from './OpenSearchUpgradeSection';
import useOpenSearchClusterStats from './hooks/useOpenSearchClusterStats';
import useOpenSearchRollingRestart, { rollingRestartStartError } from './hooks/useOpenSearchRollingRestart';
import type { RollingRestartJob, RollingRestartState } from './rollingRestartTypes';

jest.mock('./OutdatedIndicesTable', () => ({ __esModule: true, default: () => null }));
jest.mock('./OpenSearchRollingUpgradeNodes', () => ({ __esModule: true, default: () => null }));
jest.mock('./hooks/useOpenSearchClusterStats');
jest.mock('./hooks/useOpenSearchRollingRestart', () => ({
  __esModule: true,
  default: jest.fn(),
  rollingRestartStartError: jest.fn(),
}));
jest.mock('components/indices/hooks/useOutdatedIndices');
jest.mock('logic/telemetry/useSendTelemetry');

const EVENTS = TELEMETRY_EVENT_TYPE.DATANODE_OPENSEARCH_UPGRADE;

const mockClusterStats = (overrides: Partial<ReturnType<typeof useOpenSearchClusterStats>> = {}) =>
  asMock(useOpenSearchClusterStats).mockReturnValue({
    currentVersion: '2.19.5',
    targetVersion: '3.5.0',
    nodes: [],
    numberOfDataNodes: 3,
    isError: false,
    isFetching: false,
    isLoading: false,
    isUpgradeAvailable: true,
    isUpToDate: false,
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

const mockRollingRestart = (overrides: RollingRestartHookOverrides = {}) =>
  asMock(useOpenSearchRollingRestart).mockReturnValue({
    data: null,
    isResumingRollingRestart: false,
    isStartingRollingRestart: false,
    resumeRollingRestart: jest.fn(() => Promise.resolve()),
    startRollingRestart: jest.fn(() => Promise.resolve()),
    ...overrides,
  } as unknown as ReturnType<typeof useOpenSearchRollingRestart>);

const mockOutdatedIndices = (data: Array<unknown> = []) =>
  asMock(useOutdatedIndices).mockReturnValue({
    data,
    isError: false,
    isLoading: false,
    refetch: jest.fn(),
  } as unknown as ReturnType<typeof useOutdatedIndices>);

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

describe('OpenSearchUpgradeSection', () => {
  let sendTelemetry: jest.Mock;

  beforeEach(() => {
    sendTelemetry = jest.fn();
    asMock(useSendTelemetry).mockReturnValue(sendTelemetry);
    mockClusterStats();
    mockRollingRestart();
    mockOutdatedIndices([]);
  });

  it('starts a rolling upgrade and sends telemetry on a healthy multi-node cluster', async () => {
    const startRollingRestart = jest.fn(() => Promise.resolve());
    mockRollingRestart({ startRollingRestart });
    render(<OpenSearchUpgradeSection />);

    await userEvent.click(screen.getByRole('button', { name: /start opensearch rolling upgrade/i }));

    expect(startRollingRestart).toHaveBeenCalledWith(false);
    expect(sendTelemetry).toHaveBeenCalledWith(EVENTS.ROLLING_UPGRADE_STARTED, expect.anything());
  });

  it('offers an apply-on-next-restart action below the rolling-upgrade node threshold', async () => {
    mockClusterStats({ numberOfDataNodes: 2 });
    render(<OpenSearchUpgradeSection />);

    await userEvent.click(screen.getByRole('button', { name: /apply opensearch upgrade on next restart/i }));

    expect(sendTelemetry).toHaveBeenCalledWith(EVENTS.APPLY_ON_NEXT_RESTART_CLICKED, expect.anything());
  });

  it('disables the start action while outdated indices remain', () => {
    mockOutdatedIndices([{ index_name: 'graylog_0' }]);
    render(<OpenSearchUpgradeSection />);

    expect(screen.getByRole('button', { name: /start opensearch rolling upgrade/i })).toBeDisabled();
    expect(screen.getByText(/resolve all outdated indices first/i)).toBeInTheDocument();
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

    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText(/cluster status is yellow/i)).toBeInTheDocument();

    await userEvent.click(within(dialog).getByRole('button', { name: /start anyway/i }));

    await waitFor(() => expect(startRollingRestart).toHaveBeenCalledWith(true));
    expect(sendTelemetry).toHaveBeenCalledWith(EVENTS.ROLLING_UPGRADE_FORCE_STARTED, expect.anything());
  });
});
