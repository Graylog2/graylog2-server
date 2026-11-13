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
import { render, waitFor } from 'wrappedTestingLibrary';
import { usePostHog } from 'posthog-js/react';

import mockComponent from 'helpers/mocking/MockComponent';
import { asMock } from 'helpers/mocking';
import useTelemetryData from 'logic/telemetry/useTelemetryData';
import TelemetryContext from 'logic/telemetry/TelemetryContext';
import TelemetryProvider from 'logic/telemetry/TelemetryProvider';
import { telemetryDebugStore, setTelemetryDebugEnabled } from 'logic/telemetry/debug/TelemetryDebugStore';

// Telemetry must be enabled before TelemetryProvider is imported — the flag is read at module
// load — so this suite carries its own AppConfig mock and covers the PostHog provider branch.
jest.mock('util/AppConfig', () => ({
  ...jest.requireActual('util/AppConfig').default,
  telemetry: () => ({ enabled: true, api_key: 'key', host: 'host' }),
  gl2ServerUrl: () => 'http://localhost:9000/api/',
}));

jest.mock('posthog-js/react');
jest.mock('logic/telemetry/TelemetryInfoModal', () => mockComponent('MockTelemetryInfoModal'));
jest.mock('./useTelemetryData');

jest.mock('logic/telemetry/useTelemetrySettings', () => ({
  useUpdateTelemetrySettings: jest.fn(() => ({
    mutateAsync: jest.fn(() => Promise.resolve()),
  })),
}));

jest.mock('hooks/useSystemDetails', () => () => ({ version: '7.1.0' }));

jest.mock('@graylog/server-api', () => ({
  Telemetry: {
    get: jest.fn(),
  },
}));

const mockTelemetryData = {
  current_user: {
    user: '1',
  },
  user_telemetry_settings: {
    telemetry_permission_asked: true,
    telemetry_enabled: true,
  },
  cluster: {
    cluster_id: '1',
  },
  license: {},
  plugin: {},
  search_cluster: {},
};

const renderSUT = () => {
  const consume = jest.fn();

  render(
    <TelemetryProvider>
      <TelemetryContext.Consumer>{consume}</TelemetryContext.Consumer>
    </TelemetryProvider>,
  );

  return consume;
};

const lastContext = (consume: jest.Mock) => consume.mock.calls[consume.mock.calls.length - 1][0];

describe('TelemetryProvider debug tap (PostHog branch)', () => {
  const capture = jest.fn();

  const mockPostHog = (loaded: boolean) =>
    asMock(usePostHog).mockReturnValue({
      __loaded: loaded,
      capture,
      group: jest.fn(),
      identify: jest.fn(),
    } as unknown as ReturnType<typeof usePostHog>);

  beforeEach(() => {
    jest.clearAllMocks();

    setTelemetryDebugEnabled(true);
    telemetryDebugStore.clear();
    asMock(useTelemetryData).mockReturnValue({
      data: mockTelemetryData,
      isSuccess: true,
      refetch: jest.fn(),
    } as unknown as ReturnType<typeof useTelemetryData>);
  });

  afterEach(() => {
    setTelemetryDebugEnabled(false);
    telemetryDebugStore.clear();
  });

  it('records a captured event as sent', async () => {
    mockPostHog(true);

    const consume = renderSUT();

    // globalProps resolve in an effect; the context value that captures is the post-effect one.
    await waitFor(() => {
      lastContext(consume).sendTelemetry('Fleet Created', { fleet_id: 'f1' });

      expect(capture).toHaveBeenCalledWith('Fleet Created', expect.objectContaining({ fleet_id: 'f1' }));
    });

    expect(telemetryDebugStore.getEntries()).toContainEqual(
      expect.objectContaining({
        eventType: 'Fleet Created',
        payload: { fleet_id: 'f1' },
        status: 'sent',
      }),
    );
  });

  it('records a dropped event as suppressed while posthog is not loaded', () => {
    mockPostHog(false);

    const consume = renderSUT();

    lastContext(consume).sendTelemetry('Fleet Created', { fleet_id: 'f1' });

    expect(capture).not.toHaveBeenCalled();
    expect(telemetryDebugStore.getEntries()).toEqual([
      expect.objectContaining({
        eventType: 'Fleet Created',
        payload: { fleet_id: 'f1' },
        status: 'suppressed',
      }),
    ]);
  });
});
