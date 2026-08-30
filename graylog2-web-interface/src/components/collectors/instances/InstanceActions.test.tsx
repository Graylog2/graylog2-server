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
import useCurrentUser from 'hooks/useCurrentUser';
import { adminUser } from 'fixtures/users';

import InstanceActions from './InstanceActions';

import { useCollectorsMutations } from '../hooks';
import useSendCollectorsTelemetry from '../hooks/useSendCollectorsTelemetry';
import type { CollectorInstanceView } from '../types';
import { mockCollectorsMutations } from '../testing/mockMutations';

jest.mock('../hooks/useCollectorsMutations');
jest.mock('../hooks/useSendCollectorsTelemetry');
jest.mock('hooks/useCurrentUser');
jest.mock('./ReassignFleetModal', () => (props: { onClose: () => void }) => (
  <div data-testid="reassign-modal">
    <button type="button" onClick={props.onClose}>
      Close modal
    </button>
  </div>
));

const userWith = (permissions: Array<string>) =>
  adminUser.toBuilder().permissions(Immutable.List(permissions as Array<Permission>)).build();

const mockInstance: CollectorInstanceView = {
  id: 'inst-1',
  instance_uid: 'uid-1',
  capabilities: 15,
  fleet_id: 'fleet-1',
  enrolled_at: '2026-01-01T00:00:00Z',
  last_seen: new Date().toISOString(),
  active_certificate_fingerprint: 'aa:bb:cc',
  active_certificate_expires_at: '2027-03-17T12:00:00Z',
  next_certificate_fingerprint: null,
  next_certificate_expires_at: null,
  identifying_attributes: {},
  non_identifying_attributes: {},
  hostname: 'prod-web-01',
  os: 'linux',
  version: '1.2.0',
  status: 'online',
  has_pending_changes: false,
  health: null,
};

const deleteInstanceMock = jest.fn(() => Promise.resolve());
const sendTelemetryMock = jest.fn();

describe('InstanceActions', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    asMock(useSendCollectorsTelemetry).mockReturnValue(sendTelemetryMock);
    asMock(useCollectorsMutations).mockReturnValue(
      mockCollectorsMutations({
        deleteInstance: deleteInstanceMock,
        isDeletingInstance: false,
      }),
    );
    asMock(useCurrentUser).mockReturnValue(adminUser);
  });

  it('renders View System Logs and Details buttons', async () => {
    render(<InstanceActions instance={mockInstance} onDetailsClick={jest.fn()} />);

    await screen.findByText(/view system logs/i);
    await screen.findByRole('button', { name: /details/i });
  });

  it('renders Received messages link pointing to agent_id filter', async () => {
    render(<InstanceActions instance={mockInstance} onDetailsClick={jest.fn()} />);

    const link = await screen.findByRole('link', { name: /received messages/i });
    expect(link).toHaveAttribute('href', expect.stringContaining('agent_id'));
    expect(link).toHaveAttribute('href', expect.stringContaining('uid-1'));
  });

  it('calls onDetailsClick when Details is clicked', async () => {
    const onDetailsClick = jest.fn();
    render(<InstanceActions instance={mockInstance} onDetailsClick={onDetailsClick} />);

    await userEvent.click(await screen.findByRole('button', { name: /details/i }));

    expect(onDetailsClick).toHaveBeenCalledWith(mockInstance);
  });

  describe('MoreActions dropdown', () => {
    const openMoreActions = async () => {
      await userEvent.click(await screen.findByRole('button', { name: /more actions/i }));
    };

    it('shows Reassign to fleet menu item', async () => {
      render(<InstanceActions instance={mockInstance} onDetailsClick={jest.fn()} />);

      await openMoreActions();

      await screen.findByRole('menuitem', { name: /reassign to fleet/i });
    });

    it('shows Delete menu item', async () => {
      render(<InstanceActions instance={mockInstance} onDetailsClick={jest.fn()} />);

      await openMoreActions();

      await screen.findByRole('menuitem', { name: /delete/i });
    });

    it('opens reassign modal when Reassign to fleet is clicked', async () => {
      render(<InstanceActions instance={mockInstance} onDetailsClick={jest.fn()} />);

      await openMoreActions();
      await userEvent.click(await screen.findByRole('menuitem', { name: /reassign to fleet/i }));

      await screen.findByTestId('reassign-modal');
    });

    it('opens delete confirmation when Delete is clicked', async () => {
      render(<InstanceActions instance={mockInstance} onDetailsClick={jest.fn()} />);

      await openMoreActions();
      await userEvent.click(await screen.findByRole('menuitem', { name: /delete/i }));

      await screen.findByText(/are you sure you want to delete/i);
      await screen.findByText('prod-web-01');
    });

    it('shows re-enrollment warning in delete confirmation', async () => {
      render(<InstanceActions instance={mockInstance} onDetailsClick={jest.fn()} />);

      await openMoreActions();
      await userEvent.click(await screen.findByRole('menuitem', { name: /delete/i }));

      await screen.findByText(/re-enrolled/i);
    });

    it('calls deleteInstance when delete is confirmed', async () => {
      render(<InstanceActions instance={mockInstance} onDetailsClick={jest.fn()} />);

      await openMoreActions();
      await userEvent.click(await screen.findByRole('menuitem', { name: /delete/i }));
      await userEvent.click(await screen.findByRole('button', { name: /confirm/i }));

      await waitFor(() => {
        expect(deleteInstanceMock).toHaveBeenCalledWith('uid-1');
      });
    });

    it('shows instance_uid when hostname is null', async () => {
      const instanceWithoutHostname = { ...mockInstance, hostname: null };
      render(<InstanceActions instance={instanceWithoutHostname} onDetailsClick={jest.fn()} />);

      await openMoreActions();
      await userEvent.click(await screen.findByRole('menuitem', { name: /delete/i }));

      await screen.findByText('uid-1');
    });

    it('emits DELETED telemetry on confirmed delete', async () => {
      render(<InstanceActions instance={mockInstance} onDetailsClick={jest.fn()} />);

      await openMoreActions();
      await userEvent.click(await screen.findByRole('menuitem', { name: /delete/i }));
      await userEvent.click(await screen.findByRole('button', { name: /confirm/i }));

      await waitFor(() => {
        expect(sendTelemetryMock).toHaveBeenCalledWith(
          'Collector Instance Deleted',
          expect.objectContaining({
            instance_id: 'uid-1',
            fleet_id: 'fleet-1',
            status: 'online',
            has_pending_changes: false,
            version: '1.2.0',
          }),
        );
      });
    });
  });

  describe('Received messages telemetry', () => {
    it('emits RECEIVED_MESSAGES_CLICKED with the shared instance payload', async () => {
      render(<InstanceActions instance={mockInstance} onDetailsClick={jest.fn()} />);

      await userEvent.click(await screen.findByRole('link', { name: /received messages/i }));

      expect(sendTelemetryMock).toHaveBeenCalledWith(
        'Collector Instance Received Messages Clicked',
        expect.objectContaining({
          app_action_value: 'instance-received-messages',
          instance_id: 'uid-1',
          fleet_id: 'fleet-1',
          status: 'online',
          has_pending_changes: false,
          version: '1.2.0',
        }),
      );
    });
  });

  describe('View Logs button telemetry', () => {
    it('emits VIEW_LOGS_CLICKED with the shared instance payload when View Logs is clicked', async () => {
      render(<InstanceActions instance={mockInstance} onDetailsClick={jest.fn()} />);

      await userEvent.click(await screen.findByText(/view system logs/i));

      expect(sendTelemetryMock).toHaveBeenCalledWith(
        'Collector Instance View Logs Clicked',
        expect.objectContaining({
          instance_id: 'uid-1',
          fleet_id: 'fleet-1',
          status: 'online',
          has_pending_changes: false,
          version: '1.2.0',
        }),
      );
    });
  });

  describe('Details button telemetry', () => {
    it('emits DETAILS_OPENED with the shared instance payload when Details is clicked', async () => {
      const onDetailsClick = jest.fn();
      render(<InstanceActions instance={mockInstance} onDetailsClick={onDetailsClick} />);

      await userEvent.click(await screen.findByRole('button', { name: /details/i }));

      expect(sendTelemetryMock).toHaveBeenCalledWith(
        'Collector Instance Details Opened',
        expect.objectContaining({
          instance_id: 'uid-1',
          fleet_id: 'fleet-1',
          status: 'online',
          has_pending_changes: false,
          version: '1.2.0',
        }),
      );
      expect(onDetailsClick).toHaveBeenCalledWith(mockInstance);
    });
  });
});

describe('InstanceActions permissions', () => {
  const instance = {
    instance_uid: 'i-1',
    fleet_id: 'f-1',
    hostname: 'host-1',
    status: 'online',
  } as never;

  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useCollectorsMutations).mockReturnValue(
      mockCollectorsMutations({ deleteInstance: jest.fn(), isDeletingInstance: false }),
    );
  });

  it('hides the actions menu when neither reassign nor delete is permitted', () => {
    asMock(useCurrentUser).mockReturnValue(userWith(['collector_fleets:read:f-1']));

    render(<InstanceActions instance={instance} onDetailsClick={jest.fn()} />);

    expect(screen.queryByText(/reassign to fleet/i)).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /more actions/i })).not.toBeInTheDocument();
  });

  it('shows the reassign action when assign is permitted on the instance fleet', async () => {
    asMock(useCurrentUser).mockReturnValue(
      userWith(['collector_fleets:read:f-1', 'collector_fleets:assign_instance:f-1']),
    );

    render(<InstanceActions instance={instance} onDetailsClick={jest.fn()} />);

    await userEvent.click(screen.getByRole('button', { name: /more actions/i }));

    expect(await screen.findByText(/reassign to fleet/i)).toBeInTheDocument();
  });

  it('shows only delete when delete_instance is permitted but not assign', async () => {
    asMock(useCurrentUser).mockReturnValue(
      userWith(['collector_fleets:read:f-1', 'collector_fleets:delete_instance:f-1']),
    );

    render(<InstanceActions instance={instance} onDetailsClick={jest.fn()} />);

    await userEvent.click(screen.getByRole('button', { name: /more actions/i }));

    expect(screen.queryByText(/reassign to fleet/i)).not.toBeInTheDocument();
    expect(await screen.findByRole('menuitem', { name: /delete/i })).toBeInTheDocument();
  });

  it('hides View System Logs without read permission on the collector system logs stream', async () => {
    // The link targets a search scoped to the built-in collector system logs stream. Without
    // streams:read on it the link lands on the "Missing Stream Permissions" page.
    asMock(useCurrentUser).mockReturnValue(userWith(['collector_fleets:read:f-1']));

    render(<InstanceActions instance={instance} onDetailsClick={jest.fn()} />);

    await screen.findByRole('button', { name: /details/i });

    expect(screen.queryByRole('link', { name: /view system logs/i })).not.toBeInTheDocument();
    expect(screen.queryByText(/view system logs/i)).not.toBeInTheDocument();
  });

  it('shows View System Logs with read permission on that stream', async () => {
    asMock(useCurrentUser).mockReturnValue(
      userWith(['collector_fleets:read:f-1', 'streams:read:000000000000000000000005']),
    );

    render(<InstanceActions instance={instance} onDetailsClick={jest.fn()} />);

    expect(await screen.findByRole('link', { name: /view system logs/i })).toBeInTheDocument();
  });
});
