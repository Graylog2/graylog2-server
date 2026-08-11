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
import useSendCollectorsTelemetry from 'components/collectors/hooks/useSendCollectorsTelemetry';
import { useInstances } from 'components/collectors/hooks/useInstanceQueries';

import DeployTab from './DeployTab';

import { useFleets, useCollectorsConfig, useCollectorsMutations } from '../hooks';
import { mockCollectorsMutations } from '../testing/mockMutations';
import type { Fleet } from '../types';

jest.mock('components/collectors/hooks/useSendCollectorsTelemetry');
jest.mock('components/collectors/hooks/useInstanceQueries');
jest.mock('../hooks');
jest.mock('util/copyToClipboard', () => jest.fn(() => Promise.resolve()));
jest.mock('components/common/Tooltip', () => ({ children }: { children: React.ReactNode }) => <>{children}</>);

const fleets: Fleet[] = [
  { id: 'fleet-1', name: 'web-servers', created_at: '2026-01-01T00:00:00Z', updated_at: '2026-01-01T00:00:00Z' },
  { id: 'fleet-2', name: 'db-servers', created_at: '2026-01-01T00:00:00Z', updated_at: '2026-01-01T00:00:00Z' },
];

describe('DeployTab', () => {
  const createEnrollmentToken = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useSendCollectorsTelemetry).mockReturnValue(jest.fn());
    asMock(useFleets).mockReturnValue({ data: fleets, isLoading: false } as ReturnType<typeof useFleets>);
    asMock(useCollectorsConfig).mockReturnValue({
      data: { signing_cert_id: 'cert', http: { hostname: '127.0.0.1', port: 14401 } },
      isLoading: false,
    } as ReturnType<typeof useCollectorsConfig>);
    asMock(useInstances).mockReturnValue({ data: [], error: null } as ReturnType<typeof useInstances>);
    asMock(useCollectorsMutations).mockReturnValue(mockCollectorsMutations({ createEnrollmentToken }));

    createEnrollmentToken.mockResolvedValue({
      token: 'the-token-value',
      fleet_id: 'fleet-1',
      expires_at: '2026-08-12T10:00:00Z',
    });
  });

  it('locks the install step until a token is generated', () => {
    render(<DeployTab />);

    expect(screen.getByText(/generate a token above to see the install command/i)).toBeInTheDocument();
  });

  it('generates a short-lived token for the selected fleet and shows the install command', async () => {
    const user = userEvent.setup();
    render(<DeployTab />);

    // Step 1: pick a fleet
    await user.click(screen.getByRole('combobox', { name: /select existing fleet/i }));
    await user.click(screen.getByRole('option', { name: /web-servers/i }));

    // Step 2: default short-lived token
    await user.click(screen.getByRole('button', { name: /generate token/i }));

    await waitFor(() => {
      expect(createEnrollmentToken).toHaveBeenCalledWith({
        name: 'deployment',
        fleetId: 'fleet-1',
        expiresIn: 'P1D',
      });
    });

    // Step 3 unlocked: command contains the token (Linux and macOS share the same curl template,
    // and inactive tab panels stay mounted, so the command shows up once per unix-y platform)
    expect((await screen.findAllByText(/ENROLLMENT_TOKEN=the-token-value/)).length).toBeGreaterThan(0);
    expect(screen.getAllByRole('button', { name: /copy token only/i }).length).toBeGreaterThan(0);
    // Enrolling hosts list is live
    expect(screen.getByText(/enrolling hosts/i)).toBeInTheDocument();
  });

  it('generates a named custom token with the chosen expiry', async () => {
    const user = userEvent.setup();
    render(<DeployTab />);

    await user.click(screen.getByRole('combobox', { name: /select existing fleet/i }));
    await user.click(screen.getByRole('option', { name: /web-servers/i }));

    await user.click(screen.getByRole('radio', { name: /custom token/i }));
    await user.type(screen.getByRole('textbox', { name: /name/i }), 'web-servers-rollout');
    await user.click(screen.getByRole('radio', { name: /30 days/i }));
    await user.click(screen.getByRole('button', { name: /generate token/i }));

    await waitFor(() => {
      expect(createEnrollmentToken).toHaveBeenCalledWith({
        name: 'web-servers-rollout',
        fleetId: 'fleet-1',
        expiresIn: 'P30D',
      });
    });

    // Summary row with change-token escape hatch
    expect(await screen.findByRole('button', { name: /change token/i })).toBeInTheDocument();
  });

  it('clears the generated token when the fleet changes', async () => {
    const user = userEvent.setup();
    render(<DeployTab />);

    await user.click(screen.getByRole('combobox', { name: /select existing fleet/i }));
    await user.click(screen.getByRole('option', { name: /web-servers/i }));
    await user.click(screen.getByRole('button', { name: /generate token/i }));

    expect((await screen.findAllByText(/ENROLLMENT_TOKEN=the-token-value/)).length).toBeGreaterThan(0);

    await user.click(screen.getByRole('button', { name: /change fleet/i }));

    expect(screen.getByText(/generate a token above to see the install command/i)).toBeInTheDocument();
  });
});
