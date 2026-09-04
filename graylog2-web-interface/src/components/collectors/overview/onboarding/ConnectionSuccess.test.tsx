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
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import asMock from 'helpers/mocking/AsMock';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendCollectorsTelemetry from 'components/collectors/hooks/useSendCollectorsTelemetry';
import { useSources } from 'components/collectors/hooks/useSourceQueries';
import { instanceKeyFn } from 'components/collectors/hooks/useInstanceQueries';
import type { CollectorInstanceView, Source } from 'components/collectors/types';
import { useCollectorLogPreview } from 'components/collectors/hooks/useCollectorLogPreview';

import ConnectionSuccess from './ConnectionSuccess';

jest.mock('components/collectors/hooks/useSendCollectorsTelemetry');
jest.mock('components/collectors/hooks/useCollectorLogPreview');
jest.mock('components/collectors/hooks/useSourceQueries', () => ({
  useSources: jest.fn(),
}));

const instance: CollectorInstanceView = {
  id: 'uid-42',
  instance_uid: 'uid-42',
  fleet_id: 'fleet-1',
  capabilities: 15,
  enrolled_at: '2026-06-10T12:00:00Z',
  last_seen: '2026-06-10T12:01:00Z',
  active_certificate_fingerprint: 'aa:bb:cc',
  active_certificate_expires_at: '2027-06-10T12:00:00Z',
  next_certificate_fingerprint: null,
  next_certificate_expires_at: null,
  status: 'online',
  identifying_attributes: { 'service.instance.id': 'uid-42' },
  non_identifying_attributes: { 'host.arch': 'arm64' },
  hostname: 'web-prod-01',
  os: 'linux',
  version: '1.2.3',
  has_pending_changes: false,
  health: null,
};

const sources = [
  { id: 's1', name: 'Syslog', type: 'file', enabled: true },
  { id: 's2', name: 'System Journal', type: 'journald', enabled: true },
  { id: 's3', name: 'Windows Event Log', type: 'windows_event_log', enabled: true },
] as Array<Source>;

const logPreview = {
  sourceLogs: {
    messages: [{ id: 'm1', timestamp: '2026-06-10T12:00:30.000Z', text: 'a source log line' }],
    total: 23,
  },
  selfLogs: {
    messages: [{ id: 'm2', timestamp: '2026-06-10T12:00:10.000Z', text: 'collector started' }],
    total: 7,
  },
  sourceCounts: { s1: 1204, s2: 38 },
  selfLogsError: null,
  sourceLogsError: null,
  isLoading: false,
};

describe('ConnectionSuccess', () => {
  const sendTelemetry = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();

    asMock(useSendCollectorsTelemetry).mockReturnValue(sendTelemetry);
    asMock(useCollectorLogPreview).mockReturnValue(logPreview);
    asMock(useSources).mockReturnValue({ data: sources } as ReturnType<typeof useSources>);
  });

  it('shows real instance data', () => {
    render(<ConnectionSuccess instance={instance} fleetName="Default Fleet" />);

    expect(screen.getByText('web-prod-01')).toBeInTheDocument();
    expect(screen.getByText('1.2.3')).toBeInTheDocument();
    expect(screen.getAllByRole('link', { name: 'Default Fleet' })).toHaveLength(2);
  });

  it('previews source logs for the connected instance', () => {
    render(<ConnectionSuccess instance={instance} fleetName="Default Fleet" />);

    expect(useCollectorLogPreview).toHaveBeenCalledWith('uid-42');
    expect(screen.getByText(/a source log line/)).toBeInTheDocument();
  });

  it('walks through the completed onboarding steps', () => {
    render(<ConnectionSuccess instance={instance} fleetName="Default Fleet" />);

    expect(useSources).toHaveBeenCalledWith('fleet-1');
    expect(screen.getByText('Connected')).toBeInTheDocument();
    expect(screen.getByText(/3 sources from fleet/)).toBeInTheDocument();
    expect(screen.getByText(/23 messages since an hour/)).toBeInTheDocument();
  });

  it('spins on the first-messages step until source messages arrive', () => {
    asMock(useCollectorLogPreview).mockReturnValue({
      ...logPreview,
      sourceLogs: { messages: [], total: 0 },
      sourceCounts: {},
    });

    render(<ConnectionSuccess instance={instance} fleetName="Default Fleet" />);

    expect(screen.getByText('Listening... usually under a minute')).toBeInTheDocument();
    expect(screen.getAllByText('Waiting for first messages...').length).toBeGreaterThan(0);
    // The source status footer, the empty log preview and the preview caption all surface this
    // hint while nothing has arrived yet, so more than one match is expected here.
    expect(screen.getAllByText(/checking every few seconds/)).toHaveLength(3);
  });

  it('marks sources that cannot apply to the collector platform', () => {
    render(<ConnectionSuccess instance={instance} fleetName="Default Fleet" />);

    expect(screen.getByText('Not applicable on Linux')).toBeInTheDocument();
  });

  it('lists all reported attributes', () => {
    render(<ConnectionSuccess instance={instance} fleetName="Default Fleet" />);

    // The attributes sit behind a Spoiler that clips them via CSS instead of unmounting them, and
    // its toggle only mounts once real layout measurements exceed the collapsed height — neither
    // is observable in jsdom, so this covers the attribute rendering only.
    expect(screen.getByText('host.arch')).toBeInTheDocument();
    expect(screen.getByText('arm64')).toBeInTheDocument();
    expect(screen.getByText('service.instance.id')).toBeInTheDocument();
  });

  it('falls back to the instance uid when hostname is missing', () => {
    render(<ConnectionSuccess instance={{ ...instance, hostname: null }} fleetName="Default Fleet" />);

    // Once as the Host fact fallback, once as the service.instance.id attribute value, which the
    // Spoiler keeps in the DOM even while collapsed.
    expect(screen.getAllByText('uid-42')).toHaveLength(2);
  });

  it('renders the what-is-next links', () => {
    render(<ConnectionSuccess instance={instance} fleetName="Default Fleet" />);

    expect(screen.getByRole('link', { name: 'Explore your data' })).toBeInTheDocument();
    // Also offered as the Log sources section action, hence two of them.
    expect(screen.getAllByRole('link', { name: 'Configure sources' })).toHaveLength(2);
    expect(screen.getByRole('link', { name: 'Manage fleets' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Install another collector' })).toBeInTheDocument();
  });

  it('switches to the troubleshooting view when the collector is offline', () => {
    render(<ConnectionSuccess instance={{ ...instance, status: 'offline' }} fleetName="Default Fleet" />);

    expect(screen.getByText('Connection Lost')).toBeInTheDocument();
    expect(screen.getByText('Get It Back Online')).toBeInTheDocument();
    expect(screen.getAllByText('Paused — collector offline').length).toBeGreaterThan(0);
    expect(screen.getByRole('link', { name: 'View instances' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Check again' })).toBeInTheDocument();
    // The preview switches from source messages to the collector's own logs.
    expect(screen.getByText(/collector started/)).toBeInTheDocument();
    expect(screen.queryByText(/a source log line/)).not.toBeInTheDocument();
  });

  it('invalidates the instance query when checking again', async () => {
    // A real client so the assertion covers the key itself: invalidating a key that matches no
    // query is a silent no-op, so a mismatch here would leave the button looking functional.
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const decoyKey = ['collectors', 'instances', 'unrelated'];
    queryClient.setQueryData(instanceKeyFn('uid-42'), { instance_uid: 'uid-42' });
    queryClient.setQueryData(decoyKey, { untouched: true });

    render(
      <QueryClientProvider client={queryClient}>
        <ConnectionSuccess instance={{ ...instance, status: 'offline' }} fleetName="Default Fleet" />
      </QueryClientProvider>,
    );

    expect(queryClient.getQueryState(instanceKeyFn('uid-42'))?.isInvalidated).toBe(false);

    await userEvent.click(screen.getByRole('button', { name: 'Check again' }));

    expect(queryClient.getQueryState(instanceKeyFn('uid-42'))?.isInvalidated).toBe(true);
    // Only this instance is refreshed, not every collector query in the cache.
    expect(queryClient.getQueryState(decoyKey)?.isInvalidated).toBe(false);
  });

  it('shows the empty state when the fleet has no sources', () => {
    asMock(useSources).mockReturnValue({ data: [] } as ReturnType<typeof useSources>);

    render(<ConnectionSuccess instance={instance} fleetName="Default Fleet" />);

    expect(screen.getByText(/0 sources from fleet/)).toBeInTheDocument();
    expect(screen.getByText('No sources configured for this fleet yet.')).toBeInTheDocument();
  });

  it('uses singular wording for a single source and a single message', () => {
    asMock(useSources).mockReturnValue({
      data: [sources[0]],
    } as ReturnType<typeof useSources>);
    asMock(useCollectorLogPreview).mockReturnValue({
      ...logPreview,
      sourceLogs: { messages: logPreview.sourceLogs.messages, total: 1 },
    });

    render(<ConnectionSuccess instance={instance} fleetName="Default Fleet" />);

    expect(screen.getByText(/1 source from fleet/)).toBeInTheDocument();
    expect(screen.getByText(/1 message since an hour/)).toBeInTheDocument();
  });

  it('keeps listening while the first log search is still running', () => {
    asMock(useCollectorLogPreview).mockReturnValue({ ...logPreview, sourceLogs: undefined });

    render(<ConnectionSuccess instance={instance} fleetName="Default Fleet" />);

    expect(screen.getByText('Listening... usually under a minute')).toBeInTheDocument();
  });

  it('falls back to Unknown for a missing fleet name', () => {
    render(<ConnectionSuccess instance={instance} fleetName={undefined} />);

    // The fleet is linked from both the timeline step and the collector facts.
    expect(screen.getAllByRole('link', { name: 'Unknown' })).toHaveLength(2);
  });

  it('falls back to Unknown for a missing collector version', () => {
    render(<ConnectionSuccess instance={{ ...instance, version: null }} fleetName="Default Fleet" />);

    expect(screen.getByText('Unknown')).toBeInTheDocument();
  });

  it('captions the preview differently depending on the collector status', () => {
    const { rerender } = render(<ConnectionSuccess instance={instance} fleetName="Default Fleet" />);

    // While online the source status footer shares this wording with the preview caption.
    expect(screen.getAllByText(/Showing messages received since an hour/)).toHaveLength(2);

    rerender(<ConnectionSuccess instance={{ ...instance, status: 'offline' }} fleetName="Default Fleet" />);

    expect(screen.getByText(/Showing Collector system messages received since an hour/)).toBeInTheDocument();
  });

  it('shows per-source message counts from the aggregation', () => {
    render(<ConnectionSuccess instance={instance} fleetName="Default Fleet" />);

    expect(screen.getByText('1,204 messages')).toBeInTheDocument();
    expect(screen.getByText('38 messages')).toBeInTheDocument();
    // s3 is the windows_event_log source, which cannot collect on this Linux host.
    expect(screen.getByText('Not applicable on Linux')).toBeInTheDocument();
  });

  describe('telemetry', () => {
    it('reports the primary open-in-search CTA with the receiving outcome', async () => {
      render(<ConnectionSuccess instance={instance} fleetName="Default Fleet" />);

      // The header CTA comes before the log preview's own open-in-search link.
      await userEvent.click(screen.getAllByRole('link', { name: 'Open in search' })[0]);

      expect(sendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.COLLECTORS.ONBOARDING.NEXT_STEP_CLICKED, {
        app_action_value: 'onboarding-open-in-search',
        link: 'search',
        outcome: 'online-receiving',
      });
    });

    it('reports the silent outcome while the collector has not delivered messages yet', async () => {
      asMock(useCollectorLogPreview).mockReturnValue({
        ...logPreview,
        sourceLogs: { messages: [], total: 0 },
        sourceCounts: {},
      });

      render(<ConnectionSuccess instance={instance} fleetName="Default Fleet" />);

      await userEvent.click(screen.getAllByRole('link', { name: 'Open in search' })[0]);

      expect(sendTelemetry).toHaveBeenCalledWith(
        TELEMETRY_EVENT_TYPE.COLLECTORS.ONBOARDING.NEXT_STEP_CLICKED,
        expect.objectContaining({ outcome: 'online-silent' }),
      );
    });

    it('reports the log preview open-in-search link separately from the header CTA', async () => {
      render(<ConnectionSuccess instance={instance} fleetName="Default Fleet" />);

      await userEvent.click(screen.getAllByRole('link', { name: 'Open in search' })[1]);

      expect(sendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.COLLECTORS.ONBOARDING.NEXT_STEP_CLICKED, {
        app_action_value: 'onboarding-log-preview-search',
        link: 'log-preview',
        outcome: 'online-receiving',
      });
    });

    it('reports which what-is-next link was followed', async () => {
      render(<ConnectionSuccess instance={instance} fleetName="Default Fleet" />);

      await userEvent.click(screen.getByRole('link', { name: 'Manage fleets' }));

      expect(sendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.COLLECTORS.ONBOARDING.NEXT_STEP_CLICKED, {
        app_action_value: 'onboarding-next-step',
        link: 'fleets',
        outcome: 'online-receiving',
      });

      await userEvent.click(screen.getByRole('link', { name: 'Install another collector' }));

      expect(sendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.COLLECTORS.ONBOARDING.NEXT_STEP_CLICKED, {
        app_action_value: 'onboarding-next-step',
        link: 'install-another',
        outcome: 'online-receiving',
      });
    });

    it('reports both configure-sources entry points distinguishably', async () => {
      render(<ConnectionSuccess instance={instance} fleetName="Default Fleet" />);

      const links = screen.getAllByRole('link', { name: 'Configure sources' });
      await userEvent.click(links[0]);
      await userEvent.click(links[1]);

      expect(sendTelemetry).toHaveBeenCalledWith(
        TELEMETRY_EVENT_TYPE.COLLECTORS.ONBOARDING.NEXT_STEP_CLICKED,
        expect.objectContaining({ app_action_value: 'onboarding-configure-sources', link: 'configure-sources' }),
      );
      expect(sendTelemetry).toHaveBeenCalledWith(
        TELEMETRY_EVENT_TYPE.COLLECTORS.ONBOARDING.NEXT_STEP_CLICKED,
        expect.objectContaining({ app_action_value: 'onboarding-next-step', link: 'configure-sources' }),
      );
    });

    it('reports the linked fleet name click', async () => {
      render(<ConnectionSuccess instance={instance} fleetName="Default Fleet" />);

      await userEvent.click(screen.getAllByRole('link', { name: 'Default Fleet' })[0]);

      expect(sendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.COLLECTORS.ONBOARDING.NEXT_STEP_CLICKED, {
        app_action_value: 'onboarding-fleet-link',
        link: 'fleet',
        outcome: 'online-receiving',
      });
    });

    it('reports the offline recovery actions', async () => {
      render(<ConnectionSuccess instance={{ ...instance, status: 'offline' }} fleetName="Default Fleet" />);

      await userEvent.click(screen.getByRole('link', { name: 'View instances' }));

      expect(sendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.COLLECTORS.ONBOARDING.NEXT_STEP_CLICKED, {
        app_action_value: 'onboarding-view-instances',
        link: 'instances',
        outcome: 'offline',
      });

      await userEvent.click(screen.getByRole('button', { name: 'Check again' }));

      expect(sendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.COLLECTORS.ONBOARDING.CHECK_AGAIN_CLICKED, {
        app_action_value: 'onboarding-check-again',
        status: 'offline',
      });
    });
  });

  describe('onboarding state telemetry', () => {
    const offline = { ...instance, status: 'offline' } as typeof instance;
    const notReceiving = { ...logPreview, sourceLogs: { messages: [], total: 0 } };

    const EVENTS = TELEMETRY_EVENT_TYPE.COLLECTORS.ONBOARDING;

    const callsFor = (eventType: string) => sendTelemetry.mock.calls.filter(([type]) => type === eventType);

    it('reports COMPLETED on entering the receiving state', () => {
      render(<ConnectionSuccess instance={instance} fleetName="Default Fleet" />);

      expect(sendTelemetry).toHaveBeenCalledWith(
        EVENTS.COMPLETED,
        expect.objectContaining({
          app_action_value: 'collector-onboarding-completed',
          instance_id: 'uid-42',
          outcome: 'online-receiving',
          from_outcome: null,
        }),
      );
    });

    it('reports AWAITING_DATA for a collector that is up but has sent nothing', () => {
      asMock(useCollectorLogPreview).mockReturnValue(notReceiving);

      render(<ConnectionSuccess instance={instance} fleetName="Default Fleet" />);

      expect(sendTelemetry).toHaveBeenCalledWith(
        EVENTS.AWAITING_DATA,
        expect.objectContaining({
          app_action_value: 'onboarding-awaiting-data',
          outcome: 'online-silent',
          had_messages: false,
        }),
      );
      expect(callsFor(EVENTS.COMPLETED)).toHaveLength(0);
    });

    it('reports CONNECTION_LOST on entering the offline state', () => {
      render(<ConnectionSuccess instance={offline} fleetName="Default Fleet" />);

      expect(sendTelemetry).toHaveBeenCalledWith(
        EVENTS.CONNECTION_LOST,
        expect.objectContaining({
          app_action_value: 'onboarding-connection-lost',
          outcome: 'offline',
          had_messages: true,
          seconds_since_last_seen: expect.any(Number),
        }),
      );
    });

    // The instance query polls on the heartbeat interval and returns a fresh object each time;
    // a state that has not changed must not produce an event per poll.
    it.each([
      ['receiving', instance, EVENTS.COMPLETED],
      ['offline', offline, EVENTS.CONNECTION_LOST],
    ])('reports only once while the %s state is unchanged', (_name, subject, eventType) => {
      const { rerender } = render(<ConnectionSuccess instance={subject} fleetName="Default Fleet" />);

      rerender(<ConnectionSuccess instance={{ ...subject }} fleetName="Default Fleet" />);
      rerender(<ConnectionSuccess instance={{ ...subject }} fleetName="Default Fleet" />);

      expect(callsFor(eventType)).toHaveLength(1);
    });

    it('reports the silent state again when a collector reconnects without sending data', () => {
      asMock(useCollectorLogPreview).mockReturnValue(notReceiving);

      const { rerender } = render(<ConnectionSuccess instance={offline} fleetName="Default Fleet" />);

      expect(callsFor(EVENTS.CONNECTION_LOST)).toHaveLength(1);

      rerender(<ConnectionSuccess instance={{ ...instance }} fleetName="Default Fleet" />);

      expect(sendTelemetry).toHaveBeenCalledWith(
        EVENTS.AWAITING_DATA,
        expect.objectContaining({ outcome: 'online-silent', from_outcome: 'offline' }),
      );
    });

    it('walks the full silent -> receiving -> offline -> silent path, one event per entry', () => {
      asMock(useCollectorLogPreview).mockReturnValue(notReceiving);
      const { rerender } = render(<ConnectionSuccess instance={instance} fleetName="Default Fleet" />);

      asMock(useCollectorLogPreview).mockReturnValue(logPreview);
      rerender(<ConnectionSuccess instance={{ ...instance }} fleetName="Default Fleet" />);

      rerender(<ConnectionSuccess instance={{ ...offline }} fleetName="Default Fleet" />);

      asMock(useCollectorLogPreview).mockReturnValue(notReceiving);
      rerender(<ConnectionSuccess instance={{ ...instance }} fleetName="Default Fleet" />);

      expect(sendTelemetry.mock.calls.map(([type]) => type)).toEqual([
        EVENTS.AWAITING_DATA,
        EVENTS.COMPLETED,
        EVENTS.CONNECTION_LOST,
        EVENTS.AWAITING_DATA,
      ]);
    });

    // COMPLETED is a funnel milestone, not a state: recovering does not mean onboarding twice.
    it('does not report COMPLETED a second time after a drop and recovery', () => {
      const { rerender } = render(<ConnectionSuccess instance={instance} fleetName="Default Fleet" />);

      rerender(<ConnectionSuccess instance={{ ...offline }} fleetName="Default Fleet" />);
      rerender(<ConnectionSuccess instance={{ ...instance }} fleetName="Default Fleet" />);

      expect(callsFor(EVENTS.COMPLETED)).toHaveLength(1);
    });
  });
});
