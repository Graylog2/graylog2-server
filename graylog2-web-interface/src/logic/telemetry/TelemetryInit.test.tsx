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

import TelemetryInit from 'logic/telemetry/TelemetryInit';
import { asMock } from 'helpers/mocking';
import AppConfig from 'util/AppConfig';

const mockedTelemetryConfig = {
  api_key: 'key',
  host: 'http://localhost',
  enabled: true,
};
const posthogMock = { posthog_client: true };

jest.mock('util/AppConfig', () => ({
  telemetry: jest.fn(() => mockedTelemetryConfig),
}));

jest.mock('posthog-js', () => ({
  ...posthogMock,
  init: jest.fn(),
  debug: jest.fn(),
}));

const Wrapper = ({ children }: {children: React.ReactNode}) => (
  <TelemetryInit>
    {children}
  </TelemetryInit>
);

describe('<TelemetryInit>', () => {
  it('should render Telemetry and make usePosthog available', () => {
    const { result } = renderHook(() => usePostHog(), { wrapper: Wrapper });

    expect(result.current.posthog_client).toBeTruthy();
  });

  it('should not render PosthogContext when config is not present', () => {
    asMock(AppConfig.telemetry).mockReturnValue({
      api_key: undefined,
      host: undefined,
      enabled: false,
    });

    const { result } = renderHook(() => usePostHog(), { wrapper: Wrapper });

    expect(result.current).toBeUndefined();
  });
});
