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

import ReassignFleetModal from './ReassignFleetModal';

import { useFleets, useCollectorsMutations } from '../hooks';
import type { Fleet } from '../types';

jest.mock('../hooks/useFleetQueries');
jest.mock('../hooks/useCollectorsMutations');

const mockFleets: Fleet[] = [
  { id: 'fleet-1', name: 'Production', created_at: '2026-01-01T00:00:00Z', updated_at: '2026-01-01T00:00:00Z', target_version: '' },
  { id: 'fleet-2', name: 'Staging', created_at: '2026-01-01T00:00:00Z', updated_at: '2026-01-01T00:00:00Z', target_version: '' },
  { id: 'fleet-3', name: 'Development', created_at: '2026-01-01T00:00:00Z', updated_at: '2026-01-01T00:00:00Z', target_version: '' },
];

const reassignInstancesMock = jest.fn(() => Promise.resolve());

describe('ReassignFleetModal', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    asMock(useFleets).mockReturnValue({
      data: mockFleets,
      isLoading: false,
    } as unknown as ReturnType<typeof useFleets>);

    asMock(useCollectorsMutations).mockReturnValue({
      reassignInstances: reassignInstancesMock,
      isReassigningInstances: false,
    } as unknown as ReturnType<typeof useCollectorsMutations>);
  });

  it('renders modal title with instance count', async () => {
    render(
      <ReassignFleetModal instanceUids={['uid-1', 'uid-2']} onClose={jest.fn()} />,
    );

    await screen.findByText(/reassign 2 instances to fleet/i);
  });

  it('renders singular title for single instance', async () => {
    render(
      <ReassignFleetModal instanceUids={['uid-1']} onClose={jest.fn()} />,
    );

    await screen.findByText(/reassign 1 instance to fleet/i);
  });

  it('excludes current fleet from options', async () => {
    render(
      <ReassignFleetModal instanceUids={['uid-1']} currentFleetId="fleet-1" onClose={jest.fn()} />,
    );

    const select = await screen.findByText(/select a fleet/i);
    await userEvent.click(select);

    expect(screen.queryByText('Production')).not.toBeInTheDocument();
    await screen.findByText('Staging');
    await screen.findByText('Development');
  });

  it('disables submit button when no fleet is selected', async () => {
    render(
      <ReassignFleetModal instanceUids={['uid-1']} onClose={jest.fn()} />,
    );

    const submitButton = await screen.findByRole('button', { name: /reassign instance/i });

    expect(submitButton).toBeDisabled();
  });

  it('calls onClose when cancel is clicked', async () => {
    const onClose = jest.fn();
    render(
      <ReassignFleetModal instanceUids={['uid-1']} onClose={onClose} />,
    );

    await userEvent.click(await screen.findByRole('button', { name: /cancel/i }));

    expect(onClose).toHaveBeenCalled();
  });

  it('calls reassignInstances with correct args on submit', async () => {
    render(
      <ReassignFleetModal instanceUids={['uid-1', 'uid-2']} onClose={jest.fn()} />,
    );

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

    render(
      <ReassignFleetModal instanceUids={['uid-1']} onClose={onClose} onSuccess={onSuccess} />,
    );

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
    } as unknown as ReturnType<typeof useFleets>);

    render(
      <ReassignFleetModal instanceUids={['uid-1']} onClose={jest.fn()} />,
    );

    await screen.findByText(/loading/i);
  });
});
