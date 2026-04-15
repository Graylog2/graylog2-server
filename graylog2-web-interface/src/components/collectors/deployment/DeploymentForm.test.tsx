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

import DeploymentForm from './DeploymentForm';

import { useFleets, useCollectorsMutations } from '../hooks';
import { mockCollectorsMutations } from '../testing/mockMutations';
import type { Fleet } from '../types';

jest.mock('components/collectors/hooks/useSendCollectorsTelemetry');
jest.mock('../hooks');
jest.mock('util/copyToClipboard', () => jest.fn(() => Promise.resolve()));
jest.mock('components/common/Tooltip', () => ({ children }: { children: React.ReactNode }) => <>{children}</>);

describe('DeploymentForm telemetry', () => {
  const sendTelemetry = jest.fn();
  const createEnrollmentToken = jest.fn();

  const mockFleets: Fleet[] = [
    {
      id: 'fleet-1',
      name: 'My Fleet',
      created_at: '2026-01-01T00:00:00Z',
      updated_at: '2026-01-01T00:00:00Z',
    },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    sendTelemetry.mockClear();
    asMock(useSendCollectorsTelemetry).mockReturnValue(sendTelemetry);
    asMock(useFleets).mockReturnValue({
      data: mockFleets,
      isLoading: false,
    });
    asMock(useCollectorsMutations).mockReturnValue(
      mockCollectorsMutations({
        createEnrollmentToken,
      }),
    );
    createEnrollmentToken.mockResolvedValue({
      token: 'token-value-xyz',
      expires_at: '2026-05-13T12:00:00Z',
    });
  });

  it('emits FLEET_SELECTED when fleet is selected', async () => {
    const user = userEvent.setup();
    render(<DeploymentForm />);

    // Open the fleet selector and select the fleet
    const fleetSelect = screen.getByRole('combobox', { name: /fleet/i });
    await user.click(fleetSelect);
    await user.click(screen.getByRole('option', { name: /my fleet/i }));

    expect(sendTelemetry).toHaveBeenCalledWith(
      'Collector Deployment Fleet Selected',
      expect.objectContaining({
        fleet_id: 'fleet-1',
        app_action_value: 'deployment-fleet',
      }),
    );
  });

  it('emits EXPIRY_SELECTED when expiry bucket is chosen', async () => {
    const user = userEvent.setup();
    render(<DeploymentForm />);

    await user.click(screen.getByRole('radio', { name: /30 days/i }));

    expect(sendTelemetry).toHaveBeenCalledWith(
      'Collector Enrollment Token Expiry Selected',
      expect.objectContaining({
        expires_in: 'P30D',
        app_action_value: 'deployment-expiry',
      }),
    );
  });

  it('emits GENERATED on successful token creation', async () => {
    const user = userEvent.setup();
    render(<DeploymentForm />);

    // Set fleet
    const fleetSelect = screen.getByRole('combobox', { name: /fleet/i });
    await user.click(fleetSelect);
    await user.click(screen.getByRole('option', { name: /my fleet/i }));

    // Set token name
    const nameInput = screen.getByPlaceholderText(/e\.g\. Initial Fleet Enrollment/i);
    await user.type(nameInput, 'my-token');

    // Click generate
    const generateButton = screen.getByRole('button', { name: /generate enrollment token/i });
    await user.click(generateButton);

    // Wait for the mutation call to complete and telemetry to fire
    await waitFor(
      () => {
        expect(createEnrollmentToken).toHaveBeenCalled();
        expect(sendTelemetry).toHaveBeenCalledWith(
          'Collector Enrollment Token Generated',
          expect.objectContaining({
            fleet_id: 'fleet-1',
            platform: 'linux',
            expires_in: 'P7D',
            app_action_value: 'deployment-generate',
          }),
        );
        expect(sendTelemetry).not.toHaveBeenCalledWith(
          'Collector Enrollment Token Generated',
          expect.objectContaining({ token_id: expect.anything() }),
        );
      },
      { timeout: 5000 },
    );
  });

  it('emits TOKEN_COPIED when token clipboard button is clicked', async () => {
    const user = userEvent.setup();
    render(<DeploymentForm />);

    // Generate token first
    const fleetSelect = screen.getByRole('combobox', { name: /fleet/i });
    await user.click(fleetSelect);
    await user.click(screen.getByRole('option', { name: /my fleet/i }));
    const nameInput = screen.getByPlaceholderText(/e\.g\. Initial Fleet Enrollment/i);
    await user.type(nameInput, 'my-token');
    await user.click(screen.getByRole('button', { name: /generate enrollment token/i }));

    // Wait for the mutation and render
    await waitFor(
      () => {
        expect(createEnrollmentToken).toHaveBeenCalled();
      },
      { timeout: 5000 },
    );

    // Click copy token button
    const copyTokenButtons = screen.getAllByRole('button', { name: /copy token/i });
    await user.click(copyTokenButtons[0]);

    await waitFor(
      () => {
        expect(sendTelemetry).toHaveBeenCalledWith(
          'Collector Enrollment Token Copied',
          expect.objectContaining({
            app_action_value: 'deployment-copy-token',
          }),
        );
        expect(sendTelemetry).not.toHaveBeenCalledWith(
          'Collector Enrollment Token Copied',
          expect.objectContaining({ token_id: expect.anything() }),
        );
      },
      { timeout: 5000 },
    );
  });

});
