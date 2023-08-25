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
import { renderHook } from 'wrappedTestingLibrary/hooks';
import { usePostHog } from 'posthog-js/react';

import { asMock, MockStore } from 'helpers/mocking';
import TelemetryInit from 'logic/telemetry/TelemetryInit';
import AppConfig from 'util/AppConfig';
import { TelemetrySettingsStore } from 'stores/telemetry/TelemetrySettingsStore';

const mockedTelemetryConfig = {
  api_key: 'key',
  host: 'http://localhost',
  enabled: true,
};

jest.mock('util/AppConfig', () => ({
  gl2ServerUrl: jest.fn(() => {
    'http://localhost';
  }),
  telemetry: jest.fn(() => mockedTelemetryConfig),
}));

jest.mock('stores/telemetry/TelemetrySettingsStore', () => ({
  TelemetrySettingsActions: {
    get: jest.fn(),
  },
  TelemetrySettingsStore: MockStore(),
}));

const Wrapper = ({ children }: { children: React.ReactElement }) => (
  <TelemetryInit>
    {children}
  </TelemetryInit>
);

describe('<TelemetryInit>', () => {
  afterEach(() => {
    jest.resetAllMocks();
  });

  it('should not render PosthogContext when config is not present', async () => {
    asMock(AppConfig.telemetry).mockImplementation(() => ({
      api_key: undefined,
      host: undefined,
      enabled: false,
    }));

    asMock(TelemetrySettingsStore.getInitialState).mockReturnValue({
      telemetrySetting: {
        telemetry_permission_asked: false,
        telemetry_enabled: false,
      },
    });

    const { result } = renderHook(() => usePostHog(), { wrapper: Wrapper });

    expect(result.current.__loaded).toBeFalsy();
  });

  it('should render Telemetry and make usePosthog available', () => {
    asMock(TelemetrySettingsStore.getInitialState).mockReturnValue({
      telemetrySetting: {
        telemetry_permission_asked: false,
        telemetry_enabled: true,
      },
    });

    asMock(AppConfig.telemetry).mockImplementation(() => ({
      api_key: 'key',
      host: 'http://localhost',
      enabled: true,
    }));

    const { result } = renderHook(() => usePostHog(), { wrapper: Wrapper });

    expect(result.current.__loaded).toBeTruthy();
  });
});
