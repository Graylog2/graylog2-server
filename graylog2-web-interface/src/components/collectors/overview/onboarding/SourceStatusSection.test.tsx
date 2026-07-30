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

import type { CollectorInstanceView, Source } from 'components/collectors/types';

import SourceStatusSection from './SourceStatusSection';

const instance = {
  id: 'uid-42',
  instance_uid: 'uid-42',
  fleet_id: 'fleet-1',
  status: 'online',
  os: 'linux',
} as unknown as CollectorInstanceView;

const offlineInstance = { ...instance, status: 'offline' } as CollectorInstanceView;

const source = (overrides: Partial<Source>) =>
  ({ id: 's1', name: 'Syslog', type: 'file', enabled: true, ...overrides }) as Source;

describe('SourceStatusSection', () => {
  it('tells the user when the fleet has no sources yet', () => {
    render(<SourceStatusSection instance={instance} sources={[]} receiving={false} />);

    expect(screen.getByText('No sources configured for this fleet yet.')).toBeInTheDocument();
  });

  it('tells the user when the sources are still loading', () => {
    render(<SourceStatusSection instance={instance} sources={undefined} receiving={false} />);

    expect(screen.getByText('No sources configured for this fleet yet.')).toBeInTheDocument();
  });

  it('waits for the first messages while nothing is flowing', () => {
    render(<SourceStatusSection instance={instance} sources={[source({})]} receiving={false} />);

    expect(screen.getByText('Syslog')).toBeInTheDocument();
    expect(screen.getByText('Waiting for first messages...')).toBeInTheDocument();
    expect(screen.getByText('Checking every few seconds')).toBeInTheDocument();
  });

  it('reports sources as receiving once messages arrive', () => {
    render(<SourceStatusSection instance={instance} sources={[source({})]} receiving />);

    expect(screen.getByText('Receiving')).toBeInTheDocument();
    // The polling hint is only useful while we are still waiting for something to show up.
    expect(screen.queryByText('Checking every few seconds')).not.toBeInTheDocument();
  });

  it('pauses every source while the collector is offline', () => {
    render(<SourceStatusSection instance={offlineInstance} sources={[source({})]} receiving={false} />);

    expect(screen.getByText('Paused — collector offline')).toBeInTheDocument();
    expect(screen.queryByText('Checking every few seconds')).not.toBeInTheDocument();
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
});
