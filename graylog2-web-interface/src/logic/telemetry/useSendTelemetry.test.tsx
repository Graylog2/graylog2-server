import * as React from 'react';
import { renderHook } from 'wrappedTestingLibrary/hooks';

import TelemetryContext from 'logic/telemetry/TelemetryContext';

import useSendTelemetry from './useSendTelemetry';

jest.mock('util/AppConfig');

const contextValue = {
  sendTelemetry: jest.fn(),
};
const DummyTelemetryContext = ({ children }: React.PropsWithChildren<{}>) => (
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
