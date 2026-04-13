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
import userEvent from '@testing-library/user-event';
import { fireEvent } from '@testing-library/react';

import { asMock } from 'helpers/mocking';
import useSendCollectorsTelemetry from 'components/collectors/hooks/useSendCollectorsTelemetry';

import SourceFormModal from './SourceFormModal';

jest.mock('components/collectors/hooks/useSendCollectorsTelemetry');

describe('SourceFormModal', () => {
  const sendTelemetry = jest.fn();

  beforeEach(() => {
    asMock(useSendCollectorsTelemetry).mockReturnValue(sendTelemetry);
    sendTelemetry.mockClear();
  });

  it('renders source type selector', async () => {
    render(<SourceFormModal fleetId="fleet-1" onClose={jest.fn()} onSave={jest.fn()} />);

    await screen.findByText('File');
    await screen.findByText('Journald');
  });

  it('renders name input field', async () => {
    render(<SourceFormModal fleetId="fleet-1" onClose={jest.fn()} onSave={jest.fn()} />);

    await screen.findByLabelText(/name/i);
  });

  it('calls onClose when cancel clicked', async () => {
    const onClose = jest.fn();
    render(<SourceFormModal fleetId="fleet-1" onClose={onClose} onSave={jest.fn()} />);

    const cancelButton = await screen.findByRole('button', { name: /cancel/i });
    await userEvent.click(cancelButton);

    expect(onClose).toHaveBeenCalled();
  });

  it('renders file path input for file source type', async () => {
    render(<SourceFormModal fleetId="fleet-1" onClose={jest.fn()} onSave={jest.fn()} />);

    await screen.findByLabelText(/file path/i);
  });
});

describe('SourceFormModal telemetry', () => {
  const sendTelemetryInstance = jest.fn();

  beforeEach(() => {
    asMock(useSendCollectorsTelemetry).mockReturnValue(sendTelemetryInstance);
    sendTelemetryInstance.mockClear();
  });

  it('emits CREATE_OPENED on mount (create path)', () => {
    render(<SourceFormModal fleetId="f-1" onClose={jest.fn()} onSave={jest.fn()} />);

    expect(sendTelemetryInstance).toHaveBeenCalledWith(
      'Collector Source Create Opened',
      expect.objectContaining({ fleet_id: 'f-1' }),
    );
  });

  it('does NOT emit CREATE_OPENED on mount in edit mode', () => {
    const source = { id: 's-1', fleet_id: 'f-1', name: 's', description: '', enabled: true, type: 'file' as const, config: { paths: ['/var/log/x'], read_mode: 'end' as const } };
    render(<SourceFormModal fleetId="f-1" source={source} onClose={jest.fn()} onSave={jest.fn()} />);

    expect(sendTelemetryInstance).not.toHaveBeenCalledWith('Collector Source Create Opened', expect.anything());
  });

  it('emits CREATE_CANCELLED with dirty=false when user cancels without touching anything', async () => {
    render(<SourceFormModal fleetId="f-1" onClose={jest.fn()} onSave={jest.fn()} />);
    await userEvent.click(screen.getByRole('button', { name: /Cancel/i }));

    expect(sendTelemetryInstance).toHaveBeenCalledWith(
      'Collector Source Create Cancelled',
      expect.objectContaining({
        fleet_id: 'f-1',
        dirty: false,
        fields_touched: [],
        source_type: 'file',
        source_type_changed_from_default: false,
        enabled: true,
        enabled_toggled: false,
      }),
    );
  });

  it('emits CREATE_CANCELLED with dirty=true and fields_touched after interaction', async () => {
    render(<SourceFormModal fleetId="f-1" onClose={jest.fn()} onSave={jest.fn()} />);

    const nameInput = screen.getByLabelText(/Name/i);
    await userEvent.type(nameInput, 'my-src');
    fireEvent.blur(nameInput);

    await userEvent.click(screen.getByRole('button', { name: /Cancel/i }));

    expect(sendTelemetryInstance).toHaveBeenCalledWith(
      'Collector Source Create Cancelled',
      expect.objectContaining({
        dirty: true,
        fields_touched: expect.arrayContaining(['name']),
      }),
    );
  });

  it('emits CREATE_CANCELLED with source_type and source_type_changed_from_default after type change', async () => {
    render(<SourceFormModal fleetId="f-1" onClose={jest.fn()} onSave={jest.fn()} />);

    const typeSelect = screen.getByLabelText(/Source Type/i);
    await userEvent.selectOptions(typeSelect, 'journald');

    await userEvent.click(screen.getByRole('button', { name: /Cancel/i }));

    expect(sendTelemetryInstance).toHaveBeenCalledWith(
      'Collector Source Create Cancelled',
      expect.objectContaining({
        source_type: 'journald',
        source_type_changed_from_default: true,
        dirty: true,
      }),
    );
  });

  it('emits CREATED with source_type and enabled on successful create', async () => {
    const onSave = jest.fn().mockResolvedValue({ id: 's-99' });
    render(<SourceFormModal fleetId="f-1" onClose={jest.fn()} onSave={onSave} />);
    await userEvent.type(screen.getByLabelText(/Name/i), 'my-file-source');
    await userEvent.type(screen.getByLabelText(/File Path/i), '/var/log/x.log');
    fireEvent.click(screen.getByRole('button', { name: /Create source/i }));
    // Wait for the promise to resolve
    await screen.findByRole('button', { name: /Create source/i });

    expect(sendTelemetryInstance).toHaveBeenCalledWith(
      'Collector Source Created',
      expect.objectContaining({
        fleet_id: 'f-1',
        source_id: 's-99',
        source_type: 'file',
        enabled: true,
      }),
    );
  });

  it('emits UPDATED with enabled flipped and enabled_changed true', async () => {
    const source = {
      id: 's-1',
      fleet_id: 'f-1',
      name: 'src',
      description: '',
      enabled: true,
      type: 'journald' as const,
      config: { read_mode: 'end' as const, priority: 'info' as const },
    };
    const onSave = jest.fn().mockResolvedValue(undefined);
    render(<SourceFormModal fleetId="f-1" source={source} onClose={jest.fn()} onSave={onSave} />);
    await userEvent.click(screen.getByLabelText(/Enabled/i)); // toggle off
    fireEvent.click(screen.getByRole('button', { name: /Update source/i }));
    await screen.findByRole('button', { name: /Update source/i });

    expect(sendTelemetryInstance).toHaveBeenCalledWith(
      'Collector Source Updated',
      expect.objectContaining({
        fleet_id: 'f-1',
        source_id: 's-1',
        source_type: 'journald',
        enabled: false,
        enabled_changed: true,
        config_changed: false,
      }),
    );
  });
});
