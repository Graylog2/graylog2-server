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
import * as Immutable from 'immutable';
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import type { Permission } from 'graylog-web-plugin/plugin';

import asMock from 'helpers/mocking/AsMock';
import selectEvent from 'helpers/selectEvent';
import useCurrentUser from 'hooks/useCurrentUser';
import { adminUser } from 'fixtures/users';

import ReassignFleetModal from './ReassignFleetModal';

import { useFleets, useCollectorsMutations } from '../hooks';
import useSendCollectorsTelemetry from '../hooks/useSendCollectorsTelemetry';
import type { Fleet } from '../types';
import { mockCollectorsMutations } from '../testing/mockMutations';

jest.mock('../hooks/useFleetQueries');
jest.mock('../hooks/useCollectorsMutations');
jest.mock('../hooks/useSendCollectorsTelemetry');
jest.mock('hooks/useCurrentUser');

const userWith = (permissions: Array<string>) =>
  adminUser.toBuilder().permissions(Immutable.List(permissions as Array<Permission>)).build();

const mockFleets: Fleet[] = [
  {
    id: 'fleet-1',
    name: 'Production',
    created_at: '2026-01-01T00:00:00Z',
    updated_at: '2026-01-01T00:00:00Z',
  },
  {
    id: 'fleet-2',
    name: 'Staging',
    created_at: '2026-01-01T00:00:00Z',
    updated_at: '2026-01-01T00:00:00Z',
  },
  {
    id: 'fleet-3',
    name: 'Development',
    created_at: '2026-01-01T00:00:00Z',
    updated_at: '2026-01-01T00:00:00Z',
  },
];

const reassignInstancesMock = jest.fn(() => Promise.resolve());
const sendTelemetryMock = jest.fn();

describe('ReassignFleetModal', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    asMock(useSendCollectorsTelemetry).mockReturnValue(sendTelemetryMock);
    asMock(useFleets).mockReturnValue({
      data: mockFleets,
      isLoading: false,
    });

    asMock(useCollectorsMutations).mockReturnValue(
      mockCollectorsMutations({
        reassignInstances: reassignInstancesMock,
        isReassigningInstances: false,
      }),
    );
    asMock(useCurrentUser).mockReturnValue(adminUser);
  });

  it('renders modal title with instance count', async () => {
    render(<ReassignFleetModal instanceUids={['uid-1', 'uid-2']} onClose={jest.fn()} />);

    await screen.findByText(/reassign 2 instances to fleet/i);
  });

  it('renders singular title for single instance', async () => {
    render(<ReassignFleetModal instanceUids={['uid-1']} onClose={jest.fn()} />);

    await screen.findByText(/reassign 1 instance to fleet/i);
  });

  it('excludes current fleet from options', async () => {
    render(<ReassignFleetModal instanceUids={['uid-1']} currentFleetId="fleet-1" onClose={jest.fn()} />);

    const select = await screen.findByText(/select a fleet/i);
    await userEvent.click(select);

    expect(screen.queryByText('Production')).not.toBeInTheDocument();
    await screen.findByText('Staging');
    await screen.findByText('Development');
  });

  it('disables submit button when no fleet is selected', async () => {
    render(<ReassignFleetModal instanceUids={['uid-1']} onClose={jest.fn()} />);

    const submitButton = await screen.findByRole('button', { name: /reassign instance/i });

    expect(submitButton).toBeDisabled();
  });

  it('calls onClose when cancel is clicked', async () => {
    const onClose = jest.fn();
    render(<ReassignFleetModal instanceUids={['uid-1']} onClose={onClose} />);

    await userEvent.click(await screen.findByRole('button', { name: /cancel/i }));

    expect(onClose).toHaveBeenCalled();
  });

  it('calls reassignInstances with correct args on submit', async () => {
    render(<ReassignFleetModal instanceUids={['uid-1', 'uid-2']} onClose={jest.fn()} />);

    // Select a fleet
    await userEvent.click(await screen.findByText(/select a fleet/i));
    await userEvent.click(await screen.findByText('Staging'));

    // Submit
    await userEvent.click(await screen.findByRole('button', { name: /reassign instances/i }));

    await waitFor(() => {
      expect(reassignInstancesMock).toHaveBeenCalledWith({
        instanceUids: ['uid-1', 'uid-2'],
        fleetId: 'fleet-2',
      });
    });
  });

  it('calls onSuccess and onClose after successful reassignment', async () => {
    const onClose = jest.fn();
    const onSuccess = jest.fn();

    render(<ReassignFleetModal instanceUids={['uid-1']} onClose={onClose} onSuccess={onSuccess} />);

    await userEvent.click(await screen.findByText(/select a fleet/i));
    await userEvent.click(await screen.findByText('Staging'));
    await userEvent.click(await screen.findByRole('button', { name: /reassign instance/i }));

    await waitFor(() => {
      expect(onSuccess).toHaveBeenCalled();
      expect(onClose).toHaveBeenCalled();
    });
  });

  it('shows spinner while fleets are loading', async () => {
    asMock(useFleets).mockReturnValue({
      data: undefined,
      isLoading: true,
    });

    render(<ReassignFleetModal instanceUids={['uid-1']} onClose={jest.fn()} />);

    await screen.findByText(/loading/i);
  });

  describe('telemetry', () => {
    it('emits REASSIGNED telemetry on single-instance reassignment success', async () => {
      render(<ReassignFleetModal instanceUids={['uid-1']} currentFleetId="fleet-1" onClose={jest.fn()} />);

      // Select a fleet
      await userEvent.click(await screen.findByText(/select a fleet/i));
      await userEvent.click(await screen.findByText('Staging'));

      // Submit
      await userEvent.click(await screen.findByRole('button', { name: /reassign instance/i }));

      await waitFor(() => {
        expect(sendTelemetryMock).toHaveBeenCalledWith(
          'Collector Instance Reassigned',
          expect.objectContaining({
            instance_id: 'uid-1',
            from_fleet_id: 'fleet-1',
            to_fleet_id: 'fleet-2',
          }),
        );
      });
    });

    it('emits BULK_REASSIGNED telemetry on multi-instance reassignment success', async () => {
      render(<ReassignFleetModal instanceUids={['uid-1', 'uid-2', 'uid-3']} onClose={jest.fn()} />);

      // Select a fleet
      await userEvent.click(await screen.findByText(/select a fleet/i));
      await userEvent.click(await screen.findByText('Staging'));

      // Submit
      await userEvent.click(await screen.findByRole('button', { name: /reassign instances/i }));

      await waitFor(() => {
        expect(sendTelemetryMock).toHaveBeenCalledWith(
          'Collector Instances Bulk Reassigned',
          expect.objectContaining({
            count: 3,
            to_fleet_id: 'fleet-2',
          }),
        );
      });
    });
  });
});

describe('ReassignFleetModal permissions', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    asMock(useSendCollectorsTelemetry).mockReturnValue(jest.fn());
    asMock(useCollectorsMutations).mockReturnValue(
      mockCollectorsMutations({ reassignInstances: jest.fn(), isReassigningInstances: false }),
    );
    asMock(useFleets).mockReturnValue({
      data: [
        { id: 'f-1', name: 'Allowed' },
        { id: 'f-2', name: 'Forbidden' },
      ],
      isLoading: false,
    } as never);
  });

  it('offers only fleets the user may read and assign into', async () => {
    asMock(useCurrentUser).mockReturnValue(
      userWith(['collector_fleets:read:f-1', 'collector_fleets:assign_instance:f-1']),
    );

    render(<ReassignFleetModal instanceUids={['i-1']} onClose={jest.fn()} />);

    const input = await selectEvent.findSelectInput('Select a fleet');
    selectEvent.openMenu(input);

    expect(await screen.findByText('Allowed')).toBeInTheDocument();
    expect(screen.queryByText('Forbidden')).not.toBeInTheDocument();
  });

  it('blames permissions only when other fleets exist but none are assignable', async () => {
    asMock(useCurrentUser).mockReturnValue(userWith([]));

    render(<ReassignFleetModal instanceUids={['i-1']} onClose={jest.fn()} />);

    expect(
      await screen.findByText(/you do not have permission to assign collectors to any other fleet/i),
    ).toBeInTheDocument();
  });

  it('says there is nowhere else to move to when the only fleet is the current one', async () => {
    // A fully-permitted user with a single fleet in the system: the instance already lives in it,
    // so there is no target left. This is not a permission problem and must not be reported as one.
    asMock(useFleets).mockReturnValue({
      data: [{ id: 'f-1', name: 'Onboarding' }],
      isLoading: false,
    } as never);
    asMock(useCurrentUser).mockReturnValue(userWith(['collector_fleets:read', 'collector_fleets:assign_instance']));

    render(<ReassignFleetModal instanceUids={['i-1']} currentFleetId="f-1" onClose={jest.fn()} />);

    expect(await screen.findByText(/there is no other fleet to assign this collector to/i)).toBeInTheDocument();
    expect(screen.queryByText(/do not have permission/i)).not.toBeInTheDocument();
  });

  it('pluralises the no-other-fleet message for a bulk selection', async () => {
    asMock(useFleets).mockReturnValue({
      data: [{ id: 'f-1', name: 'Onboarding' }],
      isLoading: false,
    } as never);
    asMock(useCurrentUser).mockReturnValue(userWith(['collector_fleets:read', 'collector_fleets:assign_instance']));

    render(<ReassignFleetModal instanceUids={['i-1', 'i-2']} currentFleetId="f-1" onClose={jest.fn()} />);

    expect(await screen.findByText(/there is no other fleet to assign these collectors to/i)).toBeInTheDocument();
  });
});
