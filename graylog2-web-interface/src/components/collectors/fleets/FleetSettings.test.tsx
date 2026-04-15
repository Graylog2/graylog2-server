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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import { fireEvent } from '@testing-library/react';

import { asMock } from 'helpers/mocking';
import useSendCollectorsTelemetry from 'components/collectors/hooks/useSendCollectorsTelemetry';

import FleetSettings from './FleetSettings';

jest.mock('components/collectors/hooks/useSendCollectorsTelemetry');

describe('FleetSettings telemetry', () => {
  const sendTelemetry = jest.fn();
  const fleet = {
    id: 'f-1',
    name: 'web',
    description: '',
    target_version: '',
    created_at: '2024-01-01T00:00:00.000Z',
    updated_at: '2024-01-01T00:00:00.000Z',
  };

  beforeEach(() => {
    asMock(useSendCollectorsTelemetry).mockReturnValue(sendTelemetry);
    sendTelemetry.mockClear();
  });

  it('emits FLEET.UPDATED on successful save', async () => {
    const onSave = jest.fn().mockResolvedValue(undefined);
    render(<FleetSettings fleet={fleet as never} onSave={onSave} />);
    
    const nameInput = screen.getByLabelText(/Fleet Name/i) as HTMLInputElement;
    await userEvent.clear(nameInput);
    await userEvent.type(nameInput, 'web-v2');
    
    const saveButton = screen.getByRole('button', { name: /Save changes/i });
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(onSave).toHaveBeenCalled();
    });

    expect(sendTelemetry).toHaveBeenCalledWith(
      'Fleet Updated',
      expect.objectContaining({ fleet_id: 'f-1', app_action_value: 'fleet-settings-save' }),
    );
  });

  it('emits FLEET.DELETED on confirmed delete', async () => {
    const onDelete = jest.fn().mockResolvedValue(undefined);
    render(<FleetSettings fleet={fleet as never} onSave={jest.fn()} onDelete={onDelete} />);
    
    const deleteButton = screen.getByRole('button', { name: /Delete Fleet/i });
    await userEvent.click(deleteButton);
    
    const confirmButton = screen.getByRole('button', { name: /Confirm/i });
    fireEvent.click(confirmButton);

    await waitFor(() => {
      expect(onDelete).toHaveBeenCalled();
    });

    expect(sendTelemetry).toHaveBeenCalledWith(
      'Fleet Deleted',
      expect.objectContaining({ fleet_id: 'f-1', app_action_value: 'fleet-delete' }),
    );
  });
});
