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
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import * as Immutable from 'immutable';
import type { Permission } from 'graylog-web-plugin/plugin';

import { asMock } from 'helpers/mocking';
import useSendCollectorsTelemetry from 'components/collectors/hooks/useSendCollectorsTelemetry';
import useCurrentUser from 'hooks/useCurrentUser';
import { adminUser } from 'fixtures/users';

import FleetCardsGrid from './FleetCardsGrid';

jest.mock('components/collectors/hooks/useSendCollectorsTelemetry');
jest.mock('hooks/useCurrentUser');
jest.mock('routing/useHistory', () => () => ({ push: jest.fn() }));

const userWith = (permissions: Array<string>) =>
  adminUser.toBuilder().permissions(Immutable.List(permissions as Array<Permission>)).build();

describe('FleetCardsGrid telemetry', () => {
  const sendTelemetry = jest.fn();

  beforeEach(() => {
    asMock(useSendCollectorsTelemetry).mockReturnValue(sendTelemetry);
    asMock(useCurrentUser).mockReturnValue(adminUser);
    sendTelemetry.mockClear();
  });

  it('emits FLEET_CARD_CLICKED with health degraded and instance counts', async () => {
    const fleet = {
      fleet_id: 'f-1',
      fleet_name: 'web-servers',
      total_instances: 4,
      online_instances: 2,
      offline_instances: 2,
      total_sources: 3,
    };
    render(<FleetCardsGrid fleets={[fleet]} filter="" />);
    await userEvent.click(screen.getByTestId('fleet-card'));

    expect(sendTelemetry).toHaveBeenCalledWith(
      'Collector Overview Fleet Card Clicked',
      expect.objectContaining({
        app_action_value: 'fleet-card',
        fleet_id: 'f-1',
        health: 'degraded',
        online_instances: 2,
        offline_instances: 2,
      }),
    );
  });

  it('emits health "empty" for a fleet with zero instances', async () => {
    const fleet = {
      fleet_id: 'f-2',
      fleet_name: 'empty-fleet',
      total_instances: 0,
      online_instances: 0,
      offline_instances: 0,
      total_sources: 0,
    };
    render(<FleetCardsGrid fleets={[fleet]} filter="" />);
    await userEvent.click(screen.getByTestId('fleet-card'));

    expect(sendTelemetry).toHaveBeenCalledWith(
      'Collector Overview Fleet Card Clicked',
      expect.objectContaining({ health: 'empty' }),
    );
  });
});

describe('FleetCardsGrid empty state', () => {
  beforeEach(() => {
    asMock(useSendCollectorsTelemetry).mockReturnValue(jest.fn());
  });

  it('offers the setup call to action when the user can create fleets', async () => {
    asMock(useCurrentUser).mockReturnValue(userWith(['collector_fleets:create']));

    render(<FleetCardsGrid fleets={[]} filter="" />);

    expect(await screen.findByRole('button', { name: /create fleet/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /deploy collectors/i })).toBeInTheDocument();
  });

  it('points a user who cannot create fleets at an administrator instead', async () => {
    // Every call to action in the default empty state is a dead end for this user: they cannot
    // create a fleet, and the Deployment page redirects them away.
    asMock(useCurrentUser).mockReturnValue(userWith(['collector_fleets:read']));

    render(<FleetCardsGrid fleets={[]} filter="" />);

    expect(await screen.findByText(/contact an administrator to set up the first collectors/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /create fleet/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /deploy collectors/i })).not.toBeInTheDocument();
  });

  it('still shows the no-fleets title in both cases', async () => {
    asMock(useCurrentUser).mockReturnValue(userWith(['collector_fleets:read']));

    render(<FleetCardsGrid fleets={[]} filter="" />);

    expect(await screen.findByText(/no fleets yet/i)).toBeInTheDocument();
  });
});
