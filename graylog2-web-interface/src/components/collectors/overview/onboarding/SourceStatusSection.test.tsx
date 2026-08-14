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

import type { CollectorInstanceView, Source, SourceType } from 'components/collectors/types';

import SourceStatusSection from './SourceStatusSection';

// A type the API may return but the frontend does not know, e.g. because its definition is on a
// newer server version. Widened through `string` on purpose: the `SourceType` union claims this
// value cannot exist, which is precisely the situation being tested.
const UNRECOGNISED_TYPE = 'source_type_from_a_newer_server' as string as SourceType;

const instance = {
  id: 'uid-42',
  instance_uid: 'uid-42',
  fleet_id: 'fleet-1',
  status: 'online',
  os: 'linux',
} as CollectorInstanceView;

const offlineInstance = { ...instance, status: 'offline' } as CollectorInstanceView;

const source = (overrides: Partial<Source>) =>
  ({ id: 's1', name: 'Syslog', type: 'file', enabled: true, ...overrides }) as Source;

describe('SourceStatusSection', () => {
  it('tells the user when the fleet has no sources yet', () => {
    render(<SourceStatusSection instance={instance} sources={[]} receiving={false} />);

    expect(screen.getByText('No sources configured for this fleet yet.')).toBeInTheDocument();
  });

  it('shows the empty-fleet copy when sources is undefined (loading is not yet distinguished from empty)', () => {
    // Known gap: the component currently treats `sources === undefined` (still loading) the same
    // as an empty fleet. A real loading state (e.g. a Spinner) is a separate scoping decision for
    // a human to make, not something to add here.
    render(<SourceStatusSection instance={instance} sources={undefined} receiving={false} />);

    expect(screen.getByText('No sources configured for this fleet yet.')).toBeInTheDocument();
  });

  it('waits for the first messages while nothing is flowing', () => {
    render(<SourceStatusSection instance={instance} sources={[source({})]} receiving={false} />);

    expect(screen.getByText('Syslog')).toBeInTheDocument();
    expect(screen.getByText('Waiting for first messages...')).toBeInTheDocument();
    expect(screen.getByText(/checking every few seconds/)).toBeInTheDocument();
  });

  it('reports sources as receiving once messages arrive', () => {
    render(<SourceStatusSection instance={instance} sources={[source({})]} receiving />);

    expect(screen.getByText('Receiving')).toBeInTheDocument();
    // The polling hint is only useful while we are still waiting for something to show up.
    expect(screen.queryByText(/checking every few seconds/)).not.toBeInTheDocument();
  });

  it('pauses every source while the collector is offline', () => {
    render(<SourceStatusSection instance={offlineInstance} sources={[source({})]} receiving={false} />);

    expect(screen.getByText('Paused — collector offline')).toBeInTheDocument();
    expect(screen.queryByText(/checking every few seconds/)).not.toBeInTheDocument();
  });

  it('marks sources that cannot run on the collector platform', () => {
    render(
      <SourceStatusSection
        instance={instance}
        sources={[source({ type: 'windows_event_log', name: 'Windows Event Log' })]}
        receiving
      />,
    );

    expect(screen.getByText('Not applicable on Linux')).toBeInTheDocument();
  });

  it('applies the platform restriction per source type', () => {
    render(
      <SourceStatusSection
        instance={{ ...instance, os: 'windows' } as CollectorInstanceView}
        sources={[
          source({ id: 's1', type: 'journald', name: 'System Journal' }),
          source({ id: 's2', type: 'windows_event_log', name: 'Windows Event Log' }),
          source({ id: 's3', type: 'file', name: 'Syslog' }),
        ]}
        receiving
      />,
    );

    // journald cannot run on Windows, the event log can, and a plain file source runs everywhere.
    expect(screen.getByText('Not applicable on Windows')).toBeInTheDocument();
    expect(screen.getAllByText('Receiving')).toHaveLength(2);
  });

  it('reports a disabled source as disabled rather than as waiting', () => {
    render(<SourceStatusSection instance={instance} sources={[source({ enabled: false })]} receiving={false} />);

    expect(screen.getByText('Disabled')).toBeInTheDocument();
    expect(screen.queryByText('Waiting for first messages...')).not.toBeInTheDocument();
  });

  it('prefers the disabled status over the offline and platform ones', () => {
    render(
      <SourceStatusSection
        instance={offlineInstance}
        sources={[source({ type: 'windows_event_log', enabled: false })]}
        receiving={false}
      />,
    );

    expect(screen.getByText('Disabled')).toBeInTheDocument();
    expect(screen.queryByText('Paused — collector offline')).not.toBeInTheDocument();
    expect(screen.queryByText('Not applicable on Linux')).not.toBeInTheDocument();
  });

  it('declines to guess at a source type it does not recognise', () => {
    render(
      <SourceStatusSection
        instance={instance}
        sources={[source({ id: 's1', name: 'macOS Unified Log', type: UNRECOGNISED_TYPE })]}
        receiving={false}
        sourceCounts={{}}
      />,
    );

    expect(screen.getByText('Unrecognised source type')).toBeInTheDocument();
    // It must not pretend to be waiting on messages for a source it cannot reason about.
    expect(screen.queryByText('Waiting for first messages...')).not.toBeInTheDocument();
    expect(screen.queryByText('No messages yet')).not.toBeInTheDocument();
    // Nor claim a definite incompatibility, which is what the platform copy asserts.
    expect(screen.queryByText(/Not applicable/)).not.toBeInTheDocument();
  });

  it('claims no message count for an unrecognised source type', () => {
    render(
      <SourceStatusSection
        instance={instance}
        sources={[source({ id: 's1', name: 'macOS Unified Log', type: UNRECOGNISED_TYPE })]}
        receiving
        sourceCounts={{ s1: 1204 }}
      />,
    );

    expect(screen.getByText('—')).toBeInTheDocument();
    expect(screen.queryByText('1,204 messages')).not.toBeInTheDocument();
    expect(screen.queryByText('Receiving')).not.toBeInTheDocument();
  });

  it('prefers the disabled status over the unrecognised one', () => {
    render(
      <SourceStatusSection
        instance={instance}
        sources={[source({ id: 's1', type: UNRECOGNISED_TYPE, enabled: false })]}
        receiving={false}
      />,
    );

    expect(screen.getByText('Disabled')).toBeInTheDocument();
    expect(screen.queryByText('Unrecognised source type')).not.toBeInTheDocument();
  });

  it('prefers the unrecognised status over the offline one', () => {
    render(
      <SourceStatusSection
        instance={offlineInstance}
        sources={[source({ id: 's1', type: UNRECOGNISED_TYPE })]}
        receiving={false}
      />,
    );

    expect(screen.getByText('Unrecognised source type')).toBeInTheDocument();
    expect(screen.queryByText('Paused — collector offline')).not.toBeInTheDocument();
  });

  it('still recognises every type the frontend knows about', () => {
    render(
      <SourceStatusSection
        instance={instance}
        sources={[
          source({ id: 's1', name: 'Syslog', type: 'file' }),
          source({ id: 's2', name: 'System Journal', type: 'journald' }),
        ]}
        receiving
        sourceCounts={{ s1: 1, s2: 2 }}
      />,
    );

    // Guards against the membership check accidentally rejecting known types.
    expect(screen.queryByText('Unrecognised source type')).not.toBeInTheDocument();
    expect(screen.getAllByText('Receiving')).toHaveLength(2);
  });

  it('prefers the platform status over the offline one', () => {
    render(
      <SourceStatusSection
        instance={offlineInstance}
        sources={[source({ type: 'windows_event_log' })]}
        receiving={false}
      />,
    );

    expect(screen.getByText('Not applicable on Linux')).toBeInTheDocument();
    expect(screen.queryByText('Paused — collector offline')).not.toBeInTheDocument();
  });

  it('shows each source its own message count', () => {
    render(
      <SourceStatusSection
        instance={instance}
        sources={[source({ id: 's1', name: 'Syslog' }), source({ id: 's2', name: 'System Journal' })]}
        receiving
        sourceCounts={{ s1: 1204, s2: 38 }}
      />,
    );

    expect(screen.getByText('1,204 messages')).toBeInTheDocument();
    expect(screen.getByText('38 messages')).toBeInTheDocument();
    expect(screen.getAllByText('Receiving')).toHaveLength(2);
  });

  it('does not claim a source is receiving when only its siblings are', () => {
    render(
      <SourceStatusSection
        instance={instance}
        sources={[source({ id: 's1', name: 'Syslog' }), source({ id: 's2', name: 'Nginx Access' })]}
        receiving
        sourceCounts={{ s1: 1204 }}
      />,
    );

    expect(screen.getByText('Receiving')).toBeInTheDocument();
    // s2 has no bucket, so it produced nothing even though the collector is delivering.
    expect(screen.getByText('No messages yet')).toBeInTheDocument();
    expect(screen.queryByText('Waiting for first messages...')).not.toBeInTheDocument();
  });

  it('waits for first messages when nothing has arrived for the collector at all', () => {
    render(
      <SourceStatusSection
        instance={instance}
        sources={[source({ id: 's1', name: 'Syslog' })]}
        receiving={false}
        sourceCounts={{}}
      />,
    );

    expect(screen.getByText('Waiting for first messages...')).toBeInTheDocument();
    expect(screen.queryByText('No messages yet')).not.toBeInTheDocument();
  });

  it('falls back to the aggregate status when counts are unavailable', () => {
    render(
      <SourceStatusSection
        instance={instance}
        sources={[source({ id: 's1', name: 'Syslog' })]}
        receiving
        sourceCounts={undefined}
      />,
    );

    // No count is known, so the row must not claim zero.
    expect(screen.getByText('Receiving')).toBeInTheDocument();
    expect(screen.queryByText('No messages yet')).not.toBeInTheDocument();
    expect(screen.queryByText('0')).not.toBeInTheDocument();
  });

  it('shows no count for sources that cannot be collecting', () => {
    render(
      <SourceStatusSection
        instance={instance}
        sources={[
          source({ id: 's1', name: 'Old Debug Source', enabled: false }),
          source({ id: 's2', name: 'Windows Event Log', type: 'windows_event_log' }),
        ]}
        receiving
        sourceCounts={{ s1: 5, s2: 7 }}
      />,
    );

    // Structural non-collection must not read as a throughput number.
    expect(screen.queryByText('5')).not.toBeInTheDocument();
    expect(screen.queryByText('7')).not.toBeInTheDocument();
    expect(screen.getAllByText('—')).toHaveLength(2);
  });

  it('explains the window the counts cover', () => {
    render(
      <SourceStatusSection instance={instance} sources={[source({ id: 's1' })]} receiving sourceCounts={{ s1: 3 }} />,
    );

    expect(screen.getByText(/Messages received in the last 15 minutes/)).toBeInTheDocument();
  });
});
