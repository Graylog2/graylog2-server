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
import { render, screen, act } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import copyToClipboard from 'util/copyToClipboard';
import { telemetryDebugStore, setTelemetryDebugEnabled } from 'logic/telemetry/debug/TelemetryDebugStore';

import TelemetryDebugOverlay from './TelemetryDebugOverlay';

jest.mock('util/copyToClipboard', () => jest.fn(() => Promise.resolve()));

const record = (eventType: string, payload: object = {}, status: 'sent' | 'suppressed' | 'disabled' = 'disabled') =>
  act(() => {
    telemetryDebugStore.record(eventType, payload, status);
  });

const expandOverlay = async (user: ReturnType<typeof userEvent.setup>) => {
  await user.click(screen.getByRole('button', { name: /telemetry debug/i }));
};

describe('TelemetryDebugOverlay', () => {
  beforeAll(() => {
    // jsdom has no PointerEvent; without this, fireEvent.pointer* dispatches plain Events that
    // carry no clientY, so drags would silently be no-ops.
    if (!window.PointerEvent) {
      (window as { PointerEvent: unknown }).PointerEvent = window.MouseEvent;
    }
  });

  beforeEach(() => {
    localStorage.clear();
    telemetryDebugStore.clear();
    setTelemetryDebugEnabled(true);
  });

  afterEach(() => {
    act(() => {
      setTelemetryDebugEnabled(false);
      telemetryDebugStore.clear();
    });
  });

  it('renders nothing while debugging is disabled', () => {
    setTelemetryDebugEnabled(false);

    render(<TelemetryDebugOverlay />);

    expect(screen.queryByRole('button', { name: /telemetry debug/i })).not.toBeInTheDocument();
  });

  it('shows a collapsed badge with the event count', () => {
    render(<TelemetryDebugOverlay />);

    record('Fleet Created');
    record('Fleet Deleted');

    expect(screen.getByRole('button', { name: /telemetry debug/i })).toHaveTextContent('2');
  });

  it('lists recorded events newest first with their status', async () => {
    const user = userEvent.setup();
    render(<TelemetryDebugOverlay />);

    record('Fleet Created', { fleet_id: 'f1' }, 'disabled');
    record('Fleet Deleted', { fleet_id: 'f2' }, 'sent');

    await expandOverlay(user);

    const rows = screen.getAllByRole('row').slice(1); // skip the header row

    expect(rows[0]).toHaveTextContent('Fleet Deleted');
    expect(rows[0]).toHaveTextContent('sent');
    expect(rows[1]).toHaveTextContent('Fleet Created');
    expect(rows[1]).toHaveTextContent('disabled');
  });

  it('expands a row into the payload and copies it as JSON', async () => {
    const user = userEvent.setup();
    render(<TelemetryDebugOverlay />);

    record('Fleet Created', { fleet_id: 'f1', app_action_value: 'fleet-create-submit' });

    await expandOverlay(user);
    await user.click(screen.getByText('Fleet Created'));

    expect(screen.getByText(/"fleet_id": "f1"/)).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /copy event as json/i }));

    expect(copyToClipboard).toHaveBeenCalledWith(expect.stringContaining('"fleet_id": "f1"'));
  });

  it('filters events by name and payload content', async () => {
    const user = userEvent.setup();
    render(<TelemetryDebugOverlay />);

    record('Fleet Created', { fleet_id: 'f1' });
    record('Collector Source Created', { source_type: 'journald' });

    await expandOverlay(user);
    await user.type(screen.getByRole('textbox', { name: /filter events/i }), 'journald');

    expect(screen.getByText('Collector Source Created')).toBeInTheDocument();
    expect(screen.queryByText('Fleet Created')).not.toBeInTheDocument();
  });

  it('pauses the view while the buffer keeps recording', async () => {
    const user = userEvent.setup();
    render(<TelemetryDebugOverlay />);

    record('Fleet Created');

    await expandOverlay(user);
    await user.click(screen.getByRole('button', { name: /pause/i }));

    record('Fleet Deleted');

    expect(screen.queryByText('Fleet Deleted')).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /resume/i }));

    expect(screen.getByText('Fleet Deleted')).toBeInTheDocument();
  });

  it('clears the buffer', async () => {
    const user = userEvent.setup();
    render(<TelemetryDebugOverlay />);

    record('Fleet Created');

    await expandOverlay(user);
    await user.click(screen.getByRole('button', { name: /clear/i }));

    expect(screen.queryByText('Fleet Created')).not.toBeInTheDocument();
    expect(screen.getByText(/no telemetry events recorded/i)).toBeInTheDocument();
  });

  it('exports the whole session as JSON', async () => {
    const user = userEvent.setup();
    render(<TelemetryDebugOverlay />);

    record('Fleet Created', { fleet_id: 'f1' });
    record('Fleet Deleted', { fleet_id: 'f2' });

    await expandOverlay(user);
    await user.click(screen.getByRole('button', { name: /export session/i }));

    expect(copyToClipboard).toHaveBeenCalledWith(expect.stringContaining('"fleet_id": "f2"'));
    expect(copyToClipboard).toHaveBeenCalledWith(expect.stringContaining('"fleet_id": "f1"'));
  });

  it('resizes via the drag handle and persists the height', async () => {
    const user = userEvent.setup();
    render(<TelemetryDebugOverlay />);

    await expandOverlay(user);

    const handle = screen.getByRole('separator', { name: /resize telemetry debug panel/i });

    // Drag the top edge 100px upwards from the 400px default -> 500px tall.
    await user.pointer([
      { keys: '[MouseLeft>]', target: handle, coords: { y: 500 } },
      { target: handle, coords: { y: 400 } },
      { keys: '[/MouseLeft]', target: handle, coords: { y: 400 } },
    ]);

    expect(screen.getByRole('region', { name: /telemetry debug/i })).toHaveStyle({ height: '500px' });
    expect(localStorage.getItem('gl.telemetry-debug.height')).toBe('500');
  });

  it('restores the persisted height when reopened', async () => {
    localStorage.setItem('gl.telemetry-debug.height', '333');

    const user = userEvent.setup();
    render(<TelemetryDebugOverlay />);

    await expandOverlay(user);

    expect(screen.getByRole('region', { name: /telemetry debug/i })).toHaveStyle({ height: '333px' });
  });

  it('does not shrink below the minimum height', async () => {
    const user = userEvent.setup();
    render(<TelemetryDebugOverlay />);

    await expandOverlay(user);

    const handle = screen.getByRole('separator', { name: /resize telemetry debug panel/i });

    await user.pointer([
      { keys: '[MouseLeft>]', target: handle, coords: { y: 100 } },
      { target: handle, coords: { y: 5000 } },
      { keys: '[/MouseLeft]', target: handle, coords: { y: 5000 } },
    ]);

    expect(screen.getByRole('region', { name: /telemetry debug/i })).toHaveStyle({ height: '120px' });
  });

  it('flags PII-suspect payload values with the matched rule', async () => {
    const user = userEvent.setup();
    render(<TelemetryDebugOverlay />);

    record('Collector Onboarding Completed', { hostname: 'web-prod-01.example.com', fleet_id: 'f1' });

    await expandOverlay(user);
    await user.click(screen.getByText('Collector Onboarding Completed'));

    expect(screen.getByTitle(/pii suspect: hostname/i)).toBeInTheDocument();
  });
});
