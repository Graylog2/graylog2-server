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
import { render } from 'wrappedTestingLibrary';

import mockComponent from 'helpers/mocking/MockComponent';
import { asMock, MockStore } from 'helpers/mocking';
import TelemetryInit from 'logic/telemetry/TelemetryInit';
import { TelemetrySettingsStore } from 'stores/telemetry/TelemetrySettingsStore';
import useTelemetryData from 'logic/telemetry/useTelemetryData';
import TelemetryContext from 'logic/telemetry/TelemetryContext';

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

jest.mock('stores/telemetry/TelemetrySettingsStore', () => ({
  TelemetrySettingsActions: {
    get: jest.fn(),
  },
  TelemetrySettingsStore: MockStore(),
}));

jest.useFakeTimers();

jest.mock('@graylog/server-api', () => ({
  Telemetry: {
    get: jest.fn(),
  },
}));

const renderSUT = () => {
  const consume = jest.fn();

  render(
    <TelemetryInit>
      <TelemetryContext.Consumer>
        {consume}
      </TelemetryContext.Consumer>
    </TelemetryInit>,
  );

  return consume;
};

describe('<TelemetryProvider>', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render TelemetryProvider', () => {
    asMock(useTelemetryData).mockReturnValue({ data: mockTelemetryData, isSuccess: true } as any);

    asMock(TelemetrySettingsStore.getInitialState).mockReturnValue({
      telemetrySetting: {
        telemetry_permission_asked: false,
        telemetry_enabled: true,
      },
    });

    const consume = renderSUT();

    expect(consume).toHaveBeenCalledWith(expect.objectContaining({ sendTelemetry: expect.anything() }));
  });
});
