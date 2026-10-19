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
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import type { Permission } from 'graylog-web-plugin/plugin';

import asMock from 'helpers/mocking/AsMock';
import useCurrentUser from 'hooks/useCurrentUser';
import { adminUser } from 'fixtures/users';

import FleetActions from './FleetActions';

import { useCollectorsMutations, useFleetsBulkStats } from '../hooks';
import useSendCollectorsTelemetry from '../hooks/useSendCollectorsTelemetry';
import type { Fleet } from '../types';
import { mockCollectorsMutations } from '../testing/mockMutations';

jest.mock('../hooks/useCollectorsMutations');
jest.mock('../hooks/useFleetQueries', () => ({
  ...jest.requireActual('../hooks/useFleetQueries'),
  useFleetsBulkStats: jest.fn(),
}));
jest.mock('../hooks/useSendCollectorsTelemetry');
jest.mock('hooks/useCurrentUser');

const userWith = (permissions: Array<string>) =>
  adminUser
    .toBuilder()
    .permissions(Immutable.List(permissions as Array<Permission>))
    .build();

const fleet: Fleet = {
  id: 'f-1',
  name: 'web',
  description: '',
  created_at: '2026-01-01T00:00:00.000Z',
  updated_at: '2026-01-01T00:00:00.000Z',
};

const statsWithAssignedInstances = (assignedInstances: number) => ({
  data: {
    fleets: [
      {
        fleet_id: 'f-1',
        fleet_name: 'web',
        total_instances: assignedInstances,
        online_instances: 0,
        offline_instances: assignedInstances,
        total_sources: 0,
        assigned_instances: assignedInstances,
      },
    ],
  },
});

const deleteFleetMock = jest.fn(() => Promise.resolve());
const sendTelemetryMock = jest.fn();

describe('FleetActions', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    asMock(useSendCollectorsTelemetry).mockReturnValue(sendTelemetryMock);
    asMock(useCollectorsMutations).mockReturnValue(mockCollectorsMutations({ deleteFleet: deleteFleetMock }));
    asMock(useCurrentUser).mockReturnValue(adminUser);
    asMock(useFleetsBulkStats).mockReturnValue(statsWithAssignedInstances(0) as never);
  });

  const openMoreActions = async () => {
    await userEvent.click(await screen.findByRole('button', { name: /more actions/i }));
  };

  it('deletes the fleet after confirmation and emits FLEET.DELETED', async () => {
    render(<FleetActions fleet={fleet} />);

    await openMoreActions();
    await userEvent.click(await screen.findByRole('menuitem', { name: /delete/i }));
    await userEvent.click(await screen.findByRole('button', { name: 'Confirm' }));

    expect(deleteFleetMock).toHaveBeenCalledWith('f-1');
    expect(sendTelemetryMock).toHaveBeenCalledWith(
      'Fleet Deleted',
      expect.objectContaining({ fleet_id: 'f-1', app_action_value: 'fleet-delete' }),
    );
  });

  it('does not delete the fleet when the confirmation is cancelled', async () => {
    render(<FleetActions fleet={fleet} />);

    await openMoreActions();
    await userEvent.click(await screen.findByRole('menuitem', { name: /delete/i }));
    await userEvent.click(await screen.findByRole('button', { name: /cancel/i }));

    expect(deleteFleetMock).not.toHaveBeenCalled();
  });

  it('disables the delete action while the fleet has assigned instances', async () => {
    asMock(useFleetsBulkStats).mockReturnValue(statsWithAssignedInstances(2) as never);

    render(<FleetActions fleet={fleet} />);

    await openMoreActions();
    const deleteItem = await screen.findByRole('menuitem', { name: /delete/i });

    expect(deleteItem).toBeDisabled();

    await userEvent.click(deleteItem);

    expect(screen.queryByRole('button', { name: 'Confirm' })).not.toBeInTheDocument();
    expect(deleteFleetMock).not.toHaveBeenCalled();
  });

  it('disables the delete action until fleet stats have loaded', async () => {
    asMock(useFleetsBulkStats).mockReturnValue({ data: undefined } as never);

    render(<FleetActions fleet={fleet} />);

    await openMoreActions();

    expect(await screen.findByRole('menuitem', { name: /delete/i })).toBeDisabled();
  });

  it('hides the delete action without delete permission on this fleet', async () => {
    asMock(useCurrentUser).mockReturnValue(userWith(['collector_fleets:read']));

    render(<FleetActions fleet={fleet} />);

    await screen.findByRole('link', { name: /received messages/i });

    expect(screen.queryByRole('button', { name: /more actions/i })).not.toBeInTheDocument();
  });

  it('shows the delete action with delete scoped to this fleet', async () => {
    asMock(useCurrentUser).mockReturnValue(userWith(['collector_fleets:read', 'collector_fleets:delete:f-1']));

    render(<FleetActions fleet={fleet} />);

    await openMoreActions();

    await screen.findByRole('menuitem', { name: /delete/i });
  });
});
