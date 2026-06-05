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

import FirstOnboarding from './FirstOnboarding';

import { useCollectorsConfig, useCollectorsMutations, useFleets } from '../hooks';
import { mockCollectorsMutations } from '../testing/mockMutations';

jest.mock('../hooks');
jest.mock('util/Version', () => ({
  getMajorAndMinorVersion: () => '7.1',
}));
jest.mock('util/copyToClipboard', () => jest.fn(() => Promise.resolve()));
jest.mock('components/common/Tooltip', () => ({ children }: { children: React.ReactNode }) => <>{children}</>);
jest.mock('routing/useHistory', () => () => ({ push: jest.fn() }));

const mockConfig = {
  http: { hostname: 'graylog.example', port: 4317 },
  ca_cert_id: null,
  signing_cert_id: null,
  token_signing_key: null,
  otlp_server_cert_id: null,
  collector_offline_threshold: 'PT5M',
  collector_default_visibility_threshold: 'PT1H',
  collector_expiration_threshold: 'P30D',
};

const mockFleets = [
  { id: 'fleet-1', name: 'Default Fleet', created_at: '2026-01-01T00:00:00Z', updated_at: '2026-01-01T00:00:00Z' },
];

const multipleFleets = [
  { id: 'fleet-1', name: 'Production', created_at: '2026-01-01T00:00:00Z', updated_at: '2026-01-01T00:00:00Z' },
  { id: 'fleet-2', name: 'Staging', created_at: '2026-01-02T00:00:00Z', updated_at: '2026-01-02T00:00:00Z' },
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
      expiresIn: 'P7D',
    });

    expect(screen.getByText(/test-token-abc/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /linux/i })).toBeInTheDocument();
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
        expiresIn: 'P7D',
      });
    });
  });

  it('shows fleet selector when multiple fleets exist', () => {
    asMock(useFleets).mockReturnValue({ data: multipleFleets, isLoading: false });

    render(<FirstOnboarding />);

    expect(screen.getByText(/choose a fleet/i)).toBeInTheDocument();
  });

  it('reuses the token when switching platforms', async () => {
    render(<FirstOnboarding />);

    await userEvent.click(screen.getByRole('button', { name: /linux/i }));

    await waitFor(() => {
      expect(screen.getByText(/waiting for connection/i)).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: /windows/i }));

    await waitFor(() => {
      expect(screen.getByText(/install on windows/i)).toBeInTheDocument();
    });

    expect(createEnrollmentToken).toHaveBeenCalledTimes(1);
  });

  it('transitions to connected phase after simulating connection', async () => {
    render(<FirstOnboarding />);

    await userEvent.click(screen.getByRole('button', { name: /linux/i }));

    await waitFor(() => {
      expect(screen.getByText(/waiting for connection/i)).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: /simulate connection/i }));

    expect(screen.getByText(/collector connected/i)).toBeInTheDocument();
    // web-prod-01 appears twice: as the connection hostname and as an auto-detected host asset
    expect(screen.getAllByText(/web-prod-01/i).length).toBeGreaterThan(0);
  });

  it('shows spinner while config or fleets are loading', async () => {
    asMock(useCollectorsConfig).mockReturnValue({ data: undefined, isLoading: true });

    render(<FirstOnboarding />);

    // Spinner renders behind a 200ms Delayed wrapper, so wait for it to appear
    expect(await screen.findByText(/loading/i)).toBeInTheDocument();
  });
});
