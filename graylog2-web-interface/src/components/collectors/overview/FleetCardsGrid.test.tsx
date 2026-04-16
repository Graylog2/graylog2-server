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

import { asMock } from 'helpers/mocking';
import useSendCollectorsTelemetry from 'components/collectors/hooks/useSendCollectorsTelemetry';

import FleetCardsGrid from './FleetCardsGrid';

jest.mock('components/collectors/hooks/useSendCollectorsTelemetry');
jest.mock('routing/useHistory', () => () => ({ push: jest.fn() }));

describe('FleetCardsGrid telemetry', () => {
  const sendTelemetry = jest.fn();

  beforeEach(() => {
    asMock(useSendCollectorsTelemetry).mockReturnValue(sendTelemetry);
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
