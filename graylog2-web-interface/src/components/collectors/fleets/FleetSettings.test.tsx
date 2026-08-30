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
import * as Immutable from 'immutable';
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import { fireEvent } from '@testing-library/react';
import type { Permission } from 'graylog-web-plugin/plugin';

import { asMock } from 'helpers/mocking';
import useSendCollectorsTelemetry from 'components/collectors/hooks/useSendCollectorsTelemetry';
import useCurrentUser from 'hooks/useCurrentUser';
import { adminUser } from 'fixtures/users';

import FleetSettings from './FleetSettings';

jest.mock('components/collectors/hooks/useSendCollectorsTelemetry');
jest.mock('hooks/useCurrentUser');

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
    asMock(useCurrentUser).mockReturnValue(adminUser);
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

describe('FleetSettings permissions', () => {
  const fleet = {
    id: 'f-1',
    name: 'web',
    description: '',
    target_version: null,
    created_at: '2026-01-01T00:00:00.000Z',
    updated_at: '2026-01-01T00:00:00.000Z',
  } as never;

  const userWith = (permissions: Array<string>) =>
    adminUser.toBuilder().permissions(Immutable.List(permissions as Array<Permission>)).build();

  it('hides the Danger Zone without delete permission', () => {
    asMock(useCurrentUser).mockReturnValue(userWith(['collector_fleets:read', 'collector_fleets:edit:f-1']));

    render(<FleetSettings fleet={fleet} onSave={jest.fn()} onDelete={jest.fn()} />);

    expect(screen.queryByRole('button', { name: /delete fleet/i })).not.toBeInTheDocument();
  });

  it('shows the Danger Zone with delete scoped to this fleet', () => {
    asMock(useCurrentUser).mockReturnValue(userWith(['collector_fleets:delete:f-1']));

    render(<FleetSettings fleet={fleet} onSave={jest.fn()} onDelete={jest.fn()} />);

    expect(screen.getByRole('button', { name: /delete fleet/i })).toBeInTheDocument();
  });

  it('hides the save controls without edit permission', () => {
    asMock(useCurrentUser).mockReturnValue(userWith(['collector_fleets:read']));

    render(<FleetSettings fleet={fleet} onSave={jest.fn()} onDelete={jest.fn()} />);

    expect(screen.queryByRole('button', { name: /save changes/i })).not.toBeInTheDocument();
  });

  it('shows the save controls with edit scoped to this fleet', () => {
    asMock(useCurrentUser).mockReturnValue(userWith(['collector_fleets:read', 'collector_fleets:edit:f-1']));

    render(<FleetSettings fleet={fleet} onSave={jest.fn()} onDelete={jest.fn()} />);

    expect(screen.getByRole('button', { name: /save changes/i })).toBeInTheDocument();
  });
});
