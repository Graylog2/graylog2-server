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
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendCollectorsTelemetry from 'components/collectors/hooks/useSendCollectorsTelemetry';
import Routes from 'routing/Routes';
import { useInstances } from 'components/collectors/hooks/useInstanceQueries';
import useFleetReceivingCounts from 'components/collectors/hooks/useFleetReceivingCounts';
import useQuery from 'routing/useQuery';
import useHistory from 'routing/useHistory';

import DeployTab from './DeployTab';

import { useFleets, useCollectorsConfig, useCollectorsMutations } from '../hooks';
import { mockCollectorsMutations } from '../testing/mockMutations';
import type { Fleet } from '../types';

jest.mock('components/collectors/hooks/useSendCollectorsTelemetry');
jest.mock('components/collectors/hooks/useInstanceQueries');
jest.mock('components/collectors/hooks/useFleetReceivingCounts');
jest.mock('../hooks');
jest.mock('routing/useQuery');
jest.mock('routing/useHistory');
jest.mock('util/copyToClipboard', () => jest.fn(() => Promise.resolve()));
jest.mock('components/common/Tooltip', () => ({ children }: { children: React.ReactNode }) => <>{children}</>);

const fleets: Fleet[] = [
  { id: 'fleet-1', name: 'web-servers', created_at: '2026-01-01T00:00:00Z', updated_at: '2026-01-01T00:00:00Z' },
  { id: 'fleet-2', name: 'db-servers', created_at: '2026-01-01T00:00:00Z', updated_at: '2026-01-01T00:00:00Z' },
];

describe('DeployTab', () => {
  const createEnrollmentToken = jest.fn();
  const historyPush = jest.fn();
  const sendTelemetry = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useQuery).mockReturnValue({});
    asMock(useHistory).mockReturnValue({ push: historyPush } as unknown as ReturnType<typeof useHistory>);
    asMock(useSendCollectorsTelemetry).mockReturnValue(sendTelemetry);
    asMock(useFleets).mockReturnValue({ data: fleets, isLoading: false } as ReturnType<typeof useFleets>);
    asMock(useCollectorsConfig).mockReturnValue({
      data: { signing_cert_id: 'cert', http: { hostname: '127.0.0.1', port: 14401 } },
      isLoading: false,
    } as ReturnType<typeof useCollectorsConfig>);
    asMock(useInstances).mockReturnValue({ data: [], error: null } as ReturnType<typeof useInstances>);
    asMock(useFleetReceivingCounts).mockReturnValue({ counts: undefined, error: null });
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

    // Step 3 unlocked: command contains the token (Linux and macOS share the same template,
    // and inactive tab panels stay mounted, so the command shows up once per unix-y platform)
    expect((await screen.findAllByText(/enroll-token the-token-value/)).length).toBeGreaterThan(0);
    // The enroll endpoint derives from the server URL (the preset mocks gl2ServerUrl as
    // http://localhost:9000/api/), not from the collectors config's ingest hostname
    // (127.0.0.1 in the mock above) — the two differ deliberately here.
    expect(screen.getAllByText(/enroll-endpoint http:\/\/localhost:9000 /).length).toBeGreaterThan(0);
    expect(screen.queryByText(/127\.0\.0\.1/)).not.toBeInTheDocument();
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
    await user.clear(screen.getByRole('textbox', { name: /name/i }));
    await user.type(screen.getByRole('textbox', { name: /name/i }), 'my own name');
    await user.click(screen.getByRole('radio', { name: /30 days/i }));
    await user.click(screen.getByRole('button', { name: /generate token/i }));

    await waitFor(() => {
      expect(createEnrollmentToken).toHaveBeenCalledWith({
        name: 'my own name',
        fleetId: 'fleet-1',
        expiresIn: 'P30D',
      });
    });

    // Summary row with change-token escape hatch
    expect(await screen.findByRole('button', { name: /change token/i })).toBeInTheDocument();
  });

  it('defaults the custom token name to the selected fleet', async () => {
    const user = userEvent.setup();
    render(<DeployTab />);

    await user.click(screen.getByRole('combobox', { name: /select existing fleet/i }));
    await user.click(screen.getByRole('option', { name: /web-servers/i }));

    await user.click(screen.getByRole('radio', { name: /custom token/i }));

    expect(screen.getByRole('textbox', { name: /name/i })).toHaveValue('web-servers rollout');

    await user.click(screen.getByRole('button', { name: /generate token/i }));

    await waitFor(() => {
      expect(createEnrollmentToken).toHaveBeenCalledWith({
        name: 'web-servers rollout',
        fleetId: 'fleet-1',
        expiresIn: 'P7D',
      });
    });
  });

  it('lets the user leave the auto-selected lone fleet via "Change fleet"', async () => {
    const user = userEvent.setup();
    asMock(useFleets).mockReturnValue({ data: [fleets[0]], isLoading: false } as ReturnType<typeof useFleets>);

    render(<DeployTab />);

    // The lone fleet is auto-selected
    expect(screen.getByText('web-servers')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /change fleet/i }));

    // Auto-selection must not kick back in — the chooser stays visible
    expect(screen.getByRole('combobox', { name: /select existing fleet/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /create new fleet/i })).toBeInTheDocument();
  });

  it('preselects the fleet from the ?fleet URL parameter', () => {
    asMock(useQuery).mockReturnValue({ fleet: 'fleet-2' });

    render(<DeployTab />);

    expect(screen.getByText('db-servers')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /change fleet/i })).toBeInTheDocument();
  });

  it('pushes the selected fleet to the URL', async () => {
    const user = userEvent.setup();
    render(<DeployTab />);

    await user.click(screen.getByRole('combobox', { name: /select existing fleet/i }));
    await user.click(screen.getByRole('option', { name: /web-servers/i }));

    expect(historyPush).toHaveBeenCalledWith(expect.stringContaining('fleet=fleet-1'));
  });

  describe('telemetry', () => {
    const pickFleet = async (user: ReturnType<typeof userEvent.setup>) => {
      await user.click(screen.getByRole('combobox', { name: /select existing fleet/i }));
      await user.click(screen.getByRole('option', { name: /web-servers/i }));
    };

    it('reports a clicked fleet selection', async () => {
      const user = userEvent.setup();
      render(<DeployTab />);

      await pickFleet(user);

      expect(sendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.COLLECTORS.ENROLLMENT_TOKEN.FLEET_SELECTED, {
        app_action_value: 'deployment-fleet',
        fleet_id: 'fleet-1',
        via: 'click',
      });
    });

    // The wizard writes the selected fleet into ?fleet, so the deep-link effect used to fire a
    // second FLEET_SELECTED for the component's own push. Simulate the URL actually changing --
    // a static useQuery mock cannot reproduce this.
    it('reports a clicked fleet selection once, not again when it lands in the URL', async () => {
      const user = userEvent.setup();
      asMock(useHistory).mockReturnValue({
        push: (url: string) => {
          historyPush(url);
          asMock(useQuery).mockReturnValue({ fleet: 'fleet-1' });
        },
      } as unknown as ReturnType<typeof useHistory>);

      const { rerender } = render(<DeployTab />);

      await pickFleet(user);
      rerender(<DeployTab />);

      const calls = sendTelemetry.mock.calls.filter(
        ([event]) => event === TELEMETRY_EVENT_TYPE.COLLECTORS.ENROLLMENT_TOKEN.FLEET_SELECTED,
      );

      expect(calls).toEqual([
        [
          TELEMETRY_EVENT_TYPE.COLLECTORS.ENROLLMENT_TOKEN.FLEET_SELECTED,
          { app_action_value: 'deployment-fleet', fleet_id: 'fleet-1', via: 'click' },
        ],
      ]);
    });

    it('reports a fleet preselected via the ?fleet URL parameter exactly once', () => {
      asMock(useQuery).mockReturnValue({ fleet: 'fleet-2' });

      render(<DeployTab />);

      const calls = sendTelemetry.mock.calls.filter(
        ([event]) => event === TELEMETRY_EVENT_TYPE.COLLECTORS.ENROLLMENT_TOKEN.FLEET_SELECTED,
      );

      expect(calls).toEqual([
        [
          TELEMETRY_EVENT_TYPE.COLLECTORS.ENROLLMENT_TOKEN.FLEET_SELECTED,
          { app_action_value: 'deployment-fleet', fleet_id: 'fleet-2', via: 'url' },
        ],
      ]);
    });

    it('reports jumping to fleet creation', async () => {
      const user = userEvent.setup();
      render(<DeployTab />);

      await user.click(screen.getByRole('button', { name: /create new fleet/i }));

      expect(sendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.COLLECTORS.FLEET.CREATE_OPENED, {
        app_action_value: 'deployment-create-fleet',
      });
      expect(historyPush).toHaveBeenCalledWith(Routes.SYSTEM.COLLECTORS.FLEETS_NEW);
    });

    it('reports clearing the selected fleet', async () => {
      const user = userEvent.setup();
      render(<DeployTab />);

      await pickFleet(user);
      await user.click(screen.getByRole('button', { name: /change fleet/i }));

      expect(sendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.COLLECTORS.DEPLOYMENT.FLEET_CLEARED, {
        app_action_value: 'deployment-change-fleet',
      });
    });

    it('reports the token mode toggle', async () => {
      const user = userEvent.setup();
      render(<DeployTab />);

      await pickFleet(user);
      await user.click(screen.getByRole('radio', { name: /custom token/i }));

      expect(sendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.COLLECTORS.ENROLLMENT_TOKEN.MODE_SELECTED, {
        app_action_value: 'deployment-token-mode',
        mode: 'custom',
      });
    });

    it('reports a generated short-lived token with its mode and effective expiry', async () => {
      const user = userEvent.setup();
      render(<DeployTab />);

      await pickFleet(user);
      await user.click(screen.getByRole('button', { name: /generate token/i }));

      await waitFor(() => {
        expect(sendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.COLLECTORS.ENROLLMENT_TOKEN.GENERATED, {
          app_action_value: 'deployment-generate',
          fleet_id: 'fleet-1',
          mode: 'short-lived',
          expires_in: 'P1D',
        });
      });
    });

    it('reports a generated custom token with the chosen expiry', async () => {
      const user = userEvent.setup();
      render(<DeployTab />);

      await pickFleet(user);
      await user.click(screen.getByRole('radio', { name: /custom token/i }));
      await user.click(screen.getByRole('radio', { name: /30 days/i }));
      await user.click(screen.getByRole('button', { name: /generate token/i }));

      await waitFor(() => {
        expect(sendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.COLLECTORS.ENROLLMENT_TOKEN.GENERATED, {
          app_action_value: 'deployment-generate',
          fleet_id: 'fleet-1',
          mode: 'custom',
          expires_in: 'P30D',
        });
      });

      expect(sendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.COLLECTORS.ENROLLMENT_TOKEN.EXPIRY_SELECTED, {
        app_action_value: 'deployment-expiry',
        expires_in: 'P30D',
      });
    });

    it('reports a failed token generation without leaking the error', async () => {
      const user = userEvent.setup();
      createEnrollmentToken.mockRejectedValue(new Error('boom'));

      render(<DeployTab />);

      await pickFleet(user);
      await user.click(screen.getByRole('button', { name: /generate token/i }));

      await waitFor(() => {
        expect(sendTelemetry).toHaveBeenCalledWith(
          TELEMETRY_EVENT_TYPE.COLLECTORS.ENROLLMENT_TOKEN.GENERATE_FAILED,
          {
            app_action_value: 'deployment-generate-failed',
            fleet_id: 'fleet-1',
            mode: 'short-lived',
          },
        );
      });

      expect(sendTelemetry).not.toHaveBeenCalledWith(
        TELEMETRY_EVENT_TYPE.COLLECTORS.ENROLLMENT_TOKEN.GENERATED,
        expect.anything(),
      );
    });

    it('reports discarding a generated token via "Change token"', async () => {
      const user = userEvent.setup();
      render(<DeployTab />);

      await pickFleet(user);
      await user.click(screen.getByRole('button', { name: /generate token/i }));
      await user.click(await screen.findByRole('button', { name: /change token/i }));

      expect(sendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.COLLECTORS.ENROLLMENT_TOKEN.CHANGE_CLICKED, {
        app_action_value: 'deployment-change-token',
      });
    });

    it('reports switching the install platform', async () => {
      const user = userEvent.setup();
      render(<DeployTab />);

      await pickFleet(user);
      await user.click(screen.getByRole('button', { name: /generate token/i }));

      await user.click(await screen.findByRole('tab', { name: /windows/i }));

      expect(sendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.COLLECTORS.INSTALL.PLATFORM_SELECTED, {
        app_action_value: 'deployment-platform',
        platform: 'windows',
      });
    });

    it('reports copying the install command with the active platform', async () => {
      const user = userEvent.setup();
      render(<DeployTab />);

      await pickFleet(user);
      await user.click(screen.getByRole('button', { name: /generate token/i }));

      await user.click((await screen.findAllByRole('button', { name: /copy command/i }))[0]);

      await waitFor(() => {
        expect(sendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.COLLECTORS.INSTALL.COMMAND_COPIED, {
          app_action_value: 'deployment-copy-command',
          platform: 'linux',
        });
      });
    });

    it('reports copying the raw token', async () => {
      const user = userEvent.setup();
      render(<DeployTab />);

      await pickFleet(user);
      await user.click(screen.getByRole('button', { name: /generate token/i }));

      await user.click((await screen.findAllByRole('button', { name: /copy token only/i }))[0]);

      await waitFor(() => {
        expect(sendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.COLLECTORS.ENROLLMENT_TOKEN.TOKEN_COPIED, {
          app_action_value: 'deployment-copy-token',
        });
      });
    });
  });

  it('clears the generated token when the fleet changes', async () => {
    const user = userEvent.setup();
    render(<DeployTab />);

    await user.click(screen.getByRole('combobox', { name: /select existing fleet/i }));
    await user.click(screen.getByRole('option', { name: /web-servers/i }));
    await user.click(screen.getByRole('button', { name: /generate token/i }));

    expect((await screen.findAllByText(/enroll-token the-token-value/)).length).toBeGreaterThan(0);

    await user.click(screen.getByRole('button', { name: /change fleet/i }));

    expect(screen.getByText(/generate a token above to see the install command/i)).toBeInTheDocument();
  });
});
