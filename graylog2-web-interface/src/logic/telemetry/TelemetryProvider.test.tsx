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
import type { PostHog } from 'posthog-js/react';
import { usePostHog } from 'posthog-js/react';
import { act, render } from 'wrappedTestingLibrary';

import mockComponent from 'helpers/mocking/MockComponent';
import { asMock, MockStore } from 'helpers/mocking';
import TelemetryInit from 'logic/telemetry/TelemetryInit';
import { TelemetrySettingsStore } from 'stores/telemetry/TelemetrySettingsStore';
import TelemetryProvider from 'logic/telemetry/TelemetryProvider';
import useTelemetryData from 'logic/telemetry/useTelemetryData';

const mockedTelemetryConfig = {
  api_key: 'key',
  host: 'http://localhost',
  enabled: true,
};
const mockTelemetryData = {
  current_user: {
    user: '1',
  },
  user_telemetry_settings: {
    telemetry_permission_asked: false,
    telemetry_enabled: true,
  },
  cluster: {
    cluster_id: '1',
  },
  license: {},
  plugin: {},
  search_cluster: {},
};
jest.mock('logic/telemetry/TelemetryInfoModal', () => mockComponent('MockTelemetryInfoModal'));
jest.mock('./useTelemetryData');

jest.mock('util/AppConfig', () => ({
  gl2ServerUrl: jest.fn(() => {
    'http://localhost';
  }),
  gl2AppPathPrefix: jest.fn,
  telemetry: jest.fn(() => mockedTelemetryConfig),
}));

jest.mock('posthog-js/react');

jest.mock('stores/telemetry/TelemetrySettingsStore', () => ({
  TelemetrySettingsActions: {
    get: jest.fn(),
  },
  TelemetrySettingsStore: MockStore(),
}));

jest.mock('@graylog/server-api', () => ({
  Telemetry: {
    get: jest.fn(),
  },
}));

const Wrapper = ({ children }: { children: React.ReactElement }) => (
  <TelemetryInit>
    <TelemetryProvider>
      {children}
    </TelemetryProvider>
  </TelemetryInit>
);

describe('<TelemetryProvider>', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render TelemetryProvider', async () => {
    jest.useFakeTimers();
    const mockPostHogIdentify = jest.fn();
    const mockPostHogGroup = jest.fn();

    asMock(useTelemetryData).mockReturnValue({ data: mockTelemetryData, isSuccess: true } as any);

    asMock(TelemetrySettingsStore.getInitialState).mockReturnValue({
      telemetrySetting: {
        telemetry_permission_asked: false,
        telemetry_enabled: false,
      },
    });

    asMock(usePostHog).mockReturnValue({
      group: mockPostHogGroup,
      identify: mockPostHogIdentify,
      capture: jest.fn(),
    } as unknown as PostHog);

    render(
      <Wrapper>
        <div>Test</div>
      </Wrapper>,
    );

    act(() => {
      jest.advanceTimersByTime(2000);
    });

    await expect(mockPostHogGroup).toHaveBeenCalled();
    await expect(mockPostHogIdentify).toHaveBeenCalled();
  });
});
