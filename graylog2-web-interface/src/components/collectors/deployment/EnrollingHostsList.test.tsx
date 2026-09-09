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
import userEvent from '@testing-library/user-event';

import asMock from 'helpers/mocking/AsMock';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendCollectorsTelemetry from 'components/collectors/hooks/useSendCollectorsTelemetry';
import { useInstances } from 'components/collectors/hooks/useInstanceQueries';
import { useCollectorLogPreview } from 'components/collectors/hooks/useCollectorLogPreview';
import useFleetReceivingCounts from 'components/collectors/hooks/useFleetReceivingCounts';
import { useSources } from 'components/collectors/hooks/useSourceQueries';
import type { CollectorInstanceView } from 'components/collectors/types';

import EnrollingHostsList from './EnrollingHostsList';

jest.mock('components/collectors/hooks/useSendCollectorsTelemetry');

jest.mock('components/collectors/hooks/useInstanceQueries', () => ({
  useInstances: jest.fn(),
}));

jest.mock('components/collectors/hooks/useFleetReceivingCounts');

jest.mock('components/collectors/hooks/useCollectorLogPreview', () => ({
  useCollectorLogPreview: jest.fn(),
}));

jest.mock('components/collectors/hooks/useSourceQueries', () => ({
  useSources: jest.fn(),
}));

const instance = (id: string, hostname: string, enrolledAt: string): CollectorInstanceView =>
  ({
    id,
    instance_uid: `uid-${id}`,
    fleet_id: 'fleet-1',
    enrolled_at: enrolledAt,
    last_seen: enrolledAt,
    status: 'online',
    identifying_attributes: {},
    non_identifying_attributes: {},
    hostname,
    os: 'linux',
    version: '1.0.0',
  }) as CollectorInstanceView;

describe('EnrollingHostsList', () => {
  const defaultProps = { fleetId: 'fleet-1', fleetName: 'web-servers' };

  const mockInstances = (data: CollectorInstanceView[] | undefined, error: Error | null = null) =>
    asMock(useInstances).mockReturnValue({ data, error } as ReturnType<typeof useInstances>);

  const sendTelemetry = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();

    asMock(useSendCollectorsTelemetry).mockReturnValue(sendTelemetry);

    asMock(useCollectorLogPreview).mockReturnValue({
      selfLogs: undefined,
      sourceLogs: undefined,
      sourceCounts: undefined,
      selfLogsError: null,
      sourceLogsError: null,
      isLoading: false,
    } as unknown as ReturnType<typeof useCollectorLogPreview>);

    asMock(useSources).mockReturnValue({ data: [] } as unknown as ReturnType<typeof useSources>);
    asMock(useFleetReceivingCounts).mockReturnValue({ counts: undefined, error: null });
  });

  it('polls instances for the fleet silently', () => {
    mockInstances([]);

    render(<EnrollingHostsList {...defaultProps} />);

    expect(useInstances).toHaveBeenCalledWith('fleet-1', { refetchInterval: 3000, silent: true });
  });

  it('shows a listening hint and no rows for pre-existing instances', () => {
    mockInstances([instance('pre-existing', 'old-host', '2026-06-10T10:00:00Z')]);

    render(<EnrollingHostsList {...defaultProps} />);

    expect(screen.getByText(/hosts running the command appear here/i)).toBeInTheDocument();
    expect(screen.queryByText('old-host')).not.toBeInTheDocument();
  });

  it('lists hosts that appear after the baseline', () => {
    mockInstances([]);

    const { rerender } = render(<EnrollingHostsList {...defaultProps} />);

    mockInstances([instance('fresh', 'web-prod-02', '2026-08-11T10:00:00Z')]);

    rerender(<EnrollingHostsList {...defaultProps} />);

    expect(screen.getByText('web-prod-02')).toBeInTheDocument();
    expect(screen.getByText(/1 connected/i)).toBeInTheDocument();
  });

  it('shows per-host receiving status from the fleet-wide aggregation', () => {
    asMock(useFleetReceivingCounts).mockReturnValue({ counts: { 'uid-loud': 12, 'uid-quiet': 0 }, error: null });
    mockInstances([]);

    const { rerender } = render(<EnrollingHostsList {...defaultProps} />);

    mockInstances([
      instance('loud', 'host-loud', '2026-08-11T10:00:00Z'),
      instance('quiet', 'host-quiet', '2026-08-11T10:01:00Z'),
    ]);

    rerender(<EnrollingHostsList {...defaultProps} />);

    expect(useFleetReceivingCounts).toHaveBeenCalledWith('fleet-1');
    expect(screen.getByText('Receiving')).toBeInTheDocument();
    expect(screen.getByText(/listening…/i)).toBeInTheDocument();
  });

  it('expands a row into the inline setup view and collapses it again', async () => {
    const user = userEvent.setup();
    mockInstances([]);

    const { rerender } = render(<EnrollingHostsList {...defaultProps} />);

    mockInstances([instance('fresh', 'web-prod-02', '2026-08-11T10:00:00Z')]);

    rerender(<EnrollingHostsList {...defaultProps} />);

    await user.click(screen.getByRole('button', { name: /view setup/i }));

    // The concise connection-success view: onboarding timeline + collector facts
    expect(screen.getByText('Sources Configured')).toBeInTheDocument();
    expect(useCollectorLogPreview).toHaveBeenCalledWith('uid-fresh');

    await user.click(screen.getByRole('button', { name: /hide setup/i }));

    expect(screen.queryByText('Sources Configured')).not.toBeInTheDocument();
  });

  it('orders enrolling hosts newest first', () => {
    mockInstances([]);

    const { rerender } = render(<EnrollingHostsList {...defaultProps} />);

    mockInstances([
      instance('older', 'host-older', '2026-08-11T10:00:00Z'),
      instance('newer', 'host-newer', '2026-08-11T11:00:00Z'),
    ]);

    rerender(<EnrollingHostsList {...defaultProps} />);

    const hosts = screen.getAllByText(/host-/);

    expect(hosts.map((host) => host.textContent)).toEqual(['host-newer', 'host-older']);
  });

  describe('telemetry', () => {
    it('reports the first enrolled host exactly once', () => {
      mockInstances([]);

      const { rerender } = render(<EnrollingHostsList {...defaultProps} />);

      expect(sendTelemetry).not.toHaveBeenCalled();

      mockInstances([instance('fresh', 'web-prod-02', '2026-08-11T10:00:00Z')]);
      rerender(<EnrollingHostsList {...defaultProps} />);

      mockInstances([
        instance('fresh', 'web-prod-02', '2026-08-11T10:00:00Z'),
        instance('later', 'web-prod-03', '2026-08-11T10:05:00Z'),
      ]);
      rerender(<EnrollingHostsList {...defaultProps} />);

      const calls = sendTelemetry.mock.calls.filter(
        ([event]) => event === TELEMETRY_EVENT_TYPE.COLLECTORS.DEPLOYMENT.FIRST_HOST_ENROLLED,
      );

      expect(calls).toEqual([
        [
          TELEMETRY_EVENT_TYPE.COLLECTORS.DEPLOYMENT.FIRST_HOST_ENROLLED,
          { app_action_value: 'deployment-first-host-enrolled', fleet_id: 'fleet-1' },
        ],
      ]);
    });

    it('reports expanding and collapsing a host setup row', async () => {
      const user = userEvent.setup();
      mockInstances([]);

      const { rerender } = render(<EnrollingHostsList {...defaultProps} />);

      mockInstances([instance('fresh', 'web-prod-02', '2026-08-11T10:00:00Z')]);
      rerender(<EnrollingHostsList {...defaultProps} />);

      await user.click(screen.getByRole('button', { name: /view setup/i }));

      expect(sendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.COLLECTORS.DEPLOYMENT.HOST_SETUP_TOGGLED, {
        app_action_value: 'deployment-host-setup',
        expanded: true,
        instance_id: 'uid-fresh',
      });

      await user.click(screen.getByRole('button', { name: /hide setup/i }));

      expect(sendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.COLLECTORS.DEPLOYMENT.HOST_SETUP_TOGGLED, {
        app_action_value: 'deployment-host-setup',
        expanded: false,
        instance_id: 'uid-fresh',
      });
    });

    it('reports the view-all-instances link', async () => {
      const user = userEvent.setup();
      mockInstances([]);

      render(<EnrollingHostsList {...defaultProps} />);

      await user.click(screen.getByRole('link', { name: /view all in instances/i }));

      expect(sendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.COLLECTORS.DEPLOYMENT.LINK_CLICKED, {
        app_action_value: 'deployment-view-all-instances',
        link: 'instances',
      });
    });

    it('reports opening an enrolled host in search from the setup view', async () => {
      const user = userEvent.setup();
      mockInstances([]);

      const { rerender } = render(<EnrollingHostsList {...defaultProps} />);

      mockInstances([instance('fresh', 'web-prod-02', '2026-08-11T10:00:00Z')]);
      rerender(<EnrollingHostsList {...defaultProps} />);

      await user.click(screen.getByRole('button', { name: /view setup/i }));
      await user.click(screen.getByRole('link', { name: /open in search/i }));

      expect(sendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.COLLECTORS.DEPLOYMENT.LINK_CLICKED, {
        app_action_value: 'deployment-open-in-search',
        link: 'search',
        instance_id: 'uid-fresh',
      });
    });
  });

  it('shows an inline notice when polling fails, and keeps listening', () => {
    mockInstances(undefined, new Error('nope'));

    render(<EnrollingHostsList {...defaultProps} />);

    expect(screen.getByText(/having trouble reaching the server/i)).toBeInTheDocument();
  });
});
