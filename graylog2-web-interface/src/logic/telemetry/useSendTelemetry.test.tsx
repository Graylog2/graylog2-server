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

import TelemetryContext from 'logic/telemetry/TelemetryContext';

import useSendTelemetry from './useSendTelemetry';

jest.mock('util/AppConfig');

const contextValue = {
  sendTelemetry: jest.fn(),
};
const DummyTelemetryContext = ({ children = undefined }: React.PropsWithChildren<{}>) => (
  <TelemetryContext.Provider value={contextValue}>{children}</TelemetryContext.Provider>
);

describe('useSendTelemetry', () => {
  const setLocation = (pathname: string) => Object.defineProperty(window, 'location', {
    value: {
      pathname,
    },
    writable: true,
  });

  const oldLocation = window.location;

  afterEach(() => {
    window.location = oldLocation;
  });

  it('should return `sendTelemetry` that retrieves current route', () => {
    setLocation('/welcome');
    const { result } = renderHook(() => useSendTelemetry(), { wrapper: DummyTelemetryContext });

    const sendTelemetry = result.current;

    expect(sendTelemetry).toBeDefined();

    sendTelemetry('$pageview', { app_section: 'welcome section' });

    expect(contextValue.sendTelemetry).toHaveBeenCalledWith('$pageview', { app_path_pattern: undefined, app_section: 'welcome section' });
  });
});
