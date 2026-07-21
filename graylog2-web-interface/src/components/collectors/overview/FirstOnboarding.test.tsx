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

import { asMock } from 'helpers/mocking';
import selectEvent from 'helpers/selectEvent';
import type { CollectorInstanceView } from 'components/collectors/types';

import FirstOnboarding from './FirstOnboarding';

import { useCollectorsConfig, useCollectorsMutations, useFleets } from '../hooks';
import { mockCollectorsMutations } from '../testing/mockMutations';

jest.mock('../hooks');
jest.mock('util/Version', () => ({
  getMajorAndMinorVersion: () => '7.1',
}));
jest.mock('util/copyToClipboard', () => jest.fn(() => Promise.resolve()));
jest.mock('components/common/Tooltip', () => ({ children }: { children: React.ReactNode }) => <>{children}</>);
const mockPushWithState = jest.fn();

jest.mock('routing/useHistory', () => () => ({
  push: jest.fn(),
  pushWithState: mockPushWithState,
}));

const mockInvalidateQueries = jest.fn();
const mockSetQueryData = jest.fn();

jest.mock('@tanstack/react-query', () => ({
  ...jest.requireActual('@tanstack/react-query'),
  useQueryClient: () => ({ invalidateQueries: mockInvalidateQueries, setQueryData: mockSetQueryData }),
}));

// WaitingForConnection polls the backend — stub it out with a button to trigger onConnected.
jest.mock('./onboarding/WaitingForConnection', () => {
  const mockInstance = {
    id: 'inst-1',
    instance_uid: 'uid-web-prod-01',
    fleet_id: 'fleet-1',
    capabilities: 0,
    enrolled_at: '2026-06-10T12:00:00Z',
    last_seen: '2026-06-10T12:00:00Z',
    active_certificate_fingerprint: '',
    active_certificate_expires_at: '',
    next_certificate_fingerprint: null,
    next_certificate_expires_at: null,
    identifying_attributes: {},
    non_identifying_attributes: {},
    hostname: 'web-prod-01',
    os: 'linux',
    version: '1.2.3',
    status: 'online' as const,
    has_pending_changes: false,
  };

  return function WaitingForConnectionStub({
    onConnected,
  }: {
    onConnected: (instance: CollectorInstanceView) => void;
  }) {
    return (
      <div>
        <span>Waiting for connection...</span>
        <button type="button" onClick={() => onConnected(mockInstance)}>
          Simulate connection
        </button>
      </div>
    );
  };
});

const mockConfig = {
  http: { hostname: 'graylog.example', port: 4317 },
  ca_cert_id: null,
  signing_cert_id: null,
  token_signing_key: null,
  otlp_server_cert_id: null,
  collector_offline_threshold: 'PT5M',
  collector_default_visibility_threshold: 'PT1H',
  collector_expiration_threshold: 'P30D',
  collector_heartbeat_interval: 'PT30S',
};

const mockFleets = [
  { id: 'fleet-1', name: 'Default Fleet', created_at: '2026-01-01T00:00:00Z', updated_at: '2026-01-01T00:00:00Z' },
];

const multipleFleets = [
  { id: 'fleet-1', name: 'Production', created_at: '2026-01-01T00:00:00Z', updated_at: '2026-01-01T00:00:00Z' },
  {
    id: 'fleet-2',
    name: 'Staging',
    description: 'Pre-release staging environment',
    created_at: '2026-01-02T00:00:00Z',
    updated_at: '2026-01-02T00:00:00Z',
  },
];

describe('FirstOnboarding', () => {
  const createEnrollmentToken = jest.fn();
  const createFleet = jest.fn();
  const createSource = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useCollectorsConfig).mockReturnValue({ data: mockConfig, isLoading: false });
    asMock(useFleets).mockReturnValue({ data: mockFleets, isLoading: false });
    asMock(useCollectorsMutations).mockReturnValue(
      mockCollectorsMutations({ createEnrollmentToken, createFleet, createSource }),
    );
    createEnrollmentToken.mockResolvedValue({
      token: 'test-token-abc',
      fleet_id: 'fleet-1',
      expires_at: '2026-06-04T00:00:00Z',
    });
    createFleet.mockResolvedValue({
      id: 'new-fleet-id',
      name: 'Onboarding - 2026-05-28',
      description: 'Created by Graylog 7.1 onboarding wizard',
      created_at: '2026-05-28T00:00:00Z',
      updated_at: '2026-05-28T00:00:00Z',
    });
    createSource.mockResolvedValue({});
  });

  it('renders the platform picker initially', () => {
    render(<FirstOnboarding />);

    expect(screen.getByText(/deploy lightweight collectors/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /linux/i })).toBeInTheDocument();
  });

  it('auto-selects the single fleet and shows install command', async () => {
    render(<FirstOnboarding />);

    await userEvent.click(screen.getByRole('button', { name: /linux/i }));

    await waitFor(() => {
      expect(screen.getByText(/waiting for connection/i)).toBeInTheDocument();
    });

    expect(createFleet).not.toHaveBeenCalled();
    expect(createEnrollmentToken).toHaveBeenCalledWith({
      name: 'onboarding',
      fleetId: 'fleet-1',
      expiresIn: 'P1D',
    });

    expect(screen.getByText(/test-token-abc/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /linux/i })).toBeInTheDocument();

    // A single fleet auto-selects without prompting, but the (changeable) fleet box is still shown.
    expect(screen.queryByRole('button', { name: /create new fleet/i })).not.toBeInTheDocument();
    expect(screen.getByText('Default Fleet')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /change fleet/i })).toBeInTheDocument();
  });

  it('lets the user change the auto-selected fleet when only one exists', async () => {
    render(<FirstOnboarding />);

    await userEvent.click(screen.getByRole('button', { name: /linux/i }));
    await userEvent.click(await screen.findByRole('button', { name: /change fleet/i }));

    // Changing reveals the full create-or-select choice.
    expect(screen.getByRole('button', { name: /create new fleet/i })).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /select existing fleet/i })).toBeInTheDocument();
    expect(screen.queryByText(/run this on linux/i)).not.toBeInTheDocument();
  });

  it('creates an onboarding fleet when no fleets exist', async () => {
    asMock(useFleets).mockReturnValue({ data: [], isLoading: false });

    render(<FirstOnboarding />);

    await userEvent.click(screen.getByRole('button', { name: /linux/i }));

    await waitFor(() => {
      expect(createFleet).toHaveBeenCalledWith(
        expect.objectContaining({
          name: expect.stringContaining('Onboarding'),
          description: expect.stringContaining('7.1'),
        }),
      );
    });

    expect(createSource).toHaveBeenCalledTimes(3);

    await waitFor(() => {
      expect(createEnrollmentToken).toHaveBeenCalledWith({
        name: 'onboarding',
        fleetId: 'new-fleet-id',
        expiresIn: 'P1D',
      });
    });
  });

  it('does not show the fleet choice until a platform is selected', () => {
    asMock(useFleets).mockReturnValue({ data: multipleFleets, isLoading: false });

    render(<FirstOnboarding />);

    expect(screen.getByRole('button', { name: /linux/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /create new fleet/i })).not.toBeInTheDocument();
  });

  it('shows the fleet choice after a platform is selected, before any command', async () => {
    asMock(useFleets).mockReturnValue({ data: multipleFleets, isLoading: false });

    render(<FirstOnboarding />);

    await userEvent.click(screen.getByRole('button', { name: /linux/i }));

    expect(await screen.findByRole('button', { name: /create new fleet/i })).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /select existing fleet/i })).toBeInTheDocument();

    // No fleet decided yet: the command box must not appear.
    expect(screen.queryByText(/run this on linux/i)).not.toBeInTheDocument();
    expect(createEnrollmentToken).not.toHaveBeenCalled();
  });

  it('creates a new fleet from the create-new button when multiple fleets exist', async () => {
    asMock(useFleets).mockReturnValue({ data: multipleFleets, isLoading: false });

    render(<FirstOnboarding />);

    await userEvent.click(screen.getByRole('button', { name: /linux/i }));
    await userEvent.click(await screen.findByRole('button', { name: /create new fleet/i }));

    await waitFor(() => {
      expect(createFleet).toHaveBeenCalledWith(
        expect.objectContaining({ name: expect.stringContaining('Onboarding') }),
      );
    });

    expect(createSource).toHaveBeenCalledTimes(3);

    await waitFor(() => {
      expect(createEnrollmentToken).toHaveBeenCalledWith({
        name: 'onboarding',
        fleetId: 'new-fleet-id',
        expiresIn: 'P1D',
      });
    });

    expect(await screen.findByText(/run this on linux/i)).toBeInTheDocument();
  });

  it('uses an existing fleet selected from the dropdown', async () => {
    asMock(useFleets).mockReturnValue({ data: multipleFleets, isLoading: false });

    render(<FirstOnboarding />);

    await userEvent.click(screen.getByRole('button', { name: /linux/i }));
    await screen.findByRole('button', { name: /create new fleet/i });

    await selectEvent.chooseOption('Select existing fleet', 'Staging');

    await waitFor(() => {
      expect(createEnrollmentToken).toHaveBeenCalledWith({
        name: 'onboarding',
        fleetId: 'fleet-2',
        expiresIn: 'P1D',
      });
    });

    expect(createFleet).not.toHaveBeenCalled();
    expect(await screen.findByText(/run this on linux/i)).toBeInTheDocument();
  });

  it('shows the selected fleet name and description with a change button once chosen', async () => {
    asMock(useFleets).mockReturnValue({ data: multipleFleets, isLoading: false });

    render(<FirstOnboarding />);

    await userEvent.click(screen.getByRole('button', { name: /linux/i }));
    await screen.findByRole('button', { name: /create new fleet/i });
    await selectEvent.chooseOption('Select existing fleet', 'Staging');

    expect(await screen.findByText(/run this on linux/i)).toBeInTheDocument();

    // The choice controls are replaced by a summary of the selected fleet.
    expect(screen.getByText('Staging')).toBeInTheDocument();
    expect(screen.getByText(/pre-release staging environment/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /change fleet/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /create new fleet/i })).not.toBeInTheDocument();
  });

  it('shows the newly created fleet details after using the create-new button', async () => {
    asMock(useFleets).mockReturnValue({ data: multipleFleets, isLoading: false });

    render(<FirstOnboarding />);

    await userEvent.click(screen.getByRole('button', { name: /linux/i }));
    await userEvent.click(await screen.findByRole('button', { name: /create new fleet/i }));

    expect(await screen.findByText('Onboarding - 2026-05-28')).toBeInTheDocument();
    expect(screen.getByText(/created by graylog 7\.1 onboarding wizard/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /change fleet/i })).toBeInTheDocument();
  });

  it('returns to the fleet choice and hides the command when changing the fleet', async () => {
    asMock(useFleets).mockReturnValue({ data: multipleFleets, isLoading: false });

    render(<FirstOnboarding />);

    await userEvent.click(screen.getByRole('button', { name: /linux/i }));
    await screen.findByRole('button', { name: /create new fleet/i });
    await selectEvent.chooseOption('Select existing fleet', 'Staging');

    await userEvent.click(await screen.findByRole('button', { name: /change fleet/i }));

    expect(screen.getByRole('button', { name: /create new fleet/i })).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /select existing fleet/i })).toBeInTheDocument();
    expect(screen.queryByText(/run this on linux/i)).not.toBeInTheDocument();
  });

  it('reuses the token when switching platforms', async () => {
    render(<FirstOnboarding />);

    await userEvent.click(screen.getByRole('button', { name: /linux/i }));

    await waitFor(() => {
      expect(screen.getByText(/waiting for connection/i)).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: /windows/i }));

    await waitFor(() => {
      expect(screen.getByText(/run this on windows/i)).toBeInTheDocument();
    });

    expect(createEnrollmentToken).toHaveBeenCalledTimes(1);
  });

  it('navigates to the instance result page once the collector connects', async () => {
    render(<FirstOnboarding />);

    await userEvent.click(screen.getByRole('button', { name: /linux/i }));

    await waitFor(() => {
      expect(screen.getByText(/waiting for connection/i)).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: /simulate connection/i }));

    expect(mockSetQueryData).toHaveBeenCalledWith(
      ['collectors', 'instances', 'single', 'uid-web-prod-01'],
      expect.objectContaining({ instance_uid: 'uid-web-prod-01' }),
    );
    expect(mockInvalidateQueries).toHaveBeenCalledWith({ queryKey: ['collectors'] });
    expect(mockPushWithState).toHaveBeenCalledWith('/system/collectors/onboarding/uid-web-prod-01', {
      platformId: 'linux',
      fleetName: 'Default Fleet',
    });
  });

  it('shows spinner while config or fleets are loading', async () => {
    asMock(useCollectorsConfig).mockReturnValue({ data: undefined, isLoading: true });

    render(<FirstOnboarding />);

    // Spinner renders behind a 200ms Delayed wrapper, so wait for it to appear
    expect(await screen.findByText(/loading/i)).toBeInTheDocument();
  });
});
