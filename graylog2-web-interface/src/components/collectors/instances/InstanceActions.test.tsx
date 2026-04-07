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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import asMock from 'helpers/mocking/AsMock';

import InstanceActions from './InstanceActions';

import { useCollectorsMutations } from '../hooks';
import type { CollectorInstanceView } from '../types';
import { mockCollectorsMutations } from '../testing/mockMutations';

jest.mock('../hooks/useCollectorsMutations');
jest.mock('./ReassignFleetModal', () => (props: { onClose: () => void }) => (
  <div data-testid="reassign-modal">
    <button type="button" onClick={props.onClose}>
      Close modal
    </button>
  </div>
));

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
};

const deleteInstanceMock = jest.fn(() => Promise.resolve());

describe('InstanceActions', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    asMock(useCollectorsMutations).mockReturnValue(
      mockCollectorsMutations({
        deleteInstance: deleteInstanceMock,
        isDeletingInstance: false,
      }),
    );
  });

  it('renders View Logs and Details buttons', async () => {
    render(<InstanceActions instance={mockInstance} onDetailsClick={jest.fn()} />);

    await screen.findByText(/view logs/i);
    await screen.findByRole('button', { name: /details/i });
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
  });
});
