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
import { useCollectorStats, useFleetsBulkStats } from 'components/collectors/hooks';

import CollectorsOverview from './CollectorsOverview';

jest.mock('components/collectors/hooks/useSendCollectorsTelemetry');
jest.mock('components/collectors/hooks', () => ({
  __esModule: true,
  ...jest.requireActual('components/collectors/hooks'),
  useCollectorStats: jest.fn(),
  useFleetsBulkStats: jest.fn(),
}));
jest.mock('routing/useHistory', () => () => ({ push: jest.fn() }));
jest.mock('./RecentActivity', () => () => null);

describe('CollectorsOverview telemetry', () => {
  const sendTelemetry = jest.fn();
  const stats = {
    total_instances: 10,
    online_instances: 7,
    offline_instances: 3,
    total_fleets: 2,
    total_sources: 5,
  };

  beforeEach(() => {
    asMock(useSendCollectorsTelemetry).mockReturnValue(sendTelemetry);
    asMock(useCollectorStats).mockReturnValue({ data: stats, isLoading: false, isError: false } as never);
    asMock(useFleetsBulkStats).mockReturnValue({ data: { fleets: [] }, isLoading: false, isError: false } as never);
    sendTelemetry.mockClear();
  });

  it.each([
    ['Instances', { card: 'instances', value: 10, variant: 'default', navigates_to: 'instances' }],
    ['Online', { card: 'online', value: 7, variant: 'success', navigates_to: 'instances-online' }],
    ['Offline', { card: 'offline', value: 3, variant: 'warning', navigates_to: 'instances-offline' }],
    ['Fleets', { card: 'fleets', value: 2, variant: 'default', navigates_to: 'fleets' }],
  ])('emits STAT_CARD_CLICKED on %s card click', async (label, expected) => {
    render(<CollectorsOverview />);
    await userEvent.click(screen.getByRole('button', { name: new RegExp(label) }));

    expect(sendTelemetry).toHaveBeenCalledWith(
      'Collector Stat Card Clicked',
      expect.objectContaining({
        ...expected,
        total_instances: 10,
        online_instances: 7,
        offline_instances: 3,
        total_fleets: 2,
        total_sources: 5,
      }),
    );
  });
});
