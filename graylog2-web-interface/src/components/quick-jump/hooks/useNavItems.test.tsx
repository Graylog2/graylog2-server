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
import type { PluginExports } from 'graylog-web-plugin/plugin';
import { renderHookWithDataRouter } from 'wrappedTestingLibrary/hooks';

import { usePluginExports } from 'views/test/testPlugins';
import CurrentUserContext from 'contexts/CurrentUserContext';
import { adminUser } from 'fixtures/users';
import { ScratchpadContext } from 'contexts/ScratchpadProvider';
import AppConfig from 'util/AppConfig';
import { isTelemetryDebugEnabled, setTelemetryDebugEnabled } from 'logic/telemetry/debug/TelemetryDebugStore';
import { ACTION_TYPE } from 'components/quick-jump/Constants';

import useNavItems from './useNavItems';

const helpMenuItemWithPath: PluginExports = {
  helpMenu: [
    {
      description: 'Test Item',
      path: '/path',
    },
  ],
};

const Wrapper = ({ children }: { children: React.ReactNode }) => (
  <ScratchpadContext.Provider
    value={{
      isScratchpadVisible: true,
      localStorageItem: 'gl-scratchpad-jest',
      setScratchpadVisibility: jest.fn(),
      toggleScratchpadVisibility: jest.fn(),
    }}>
    <CurrentUserContext.Provider value={adminUser}>{children}</CurrentUserContext.Provider>
  </ScratchpadContext.Provider>
);

describe('useNavItems', () => {
  usePluginExports(helpMenuItemWithPath);
  it('handles help menu items with `path`', () => {
    const { result } = renderHookWithDataRouter(() => useNavItems(), { wrapper: Wrapper });
    expect(result.current).toContainEqual({ 'link': '/path', 'title': 'Test Item', 'type': 'page' });
  });

  describe('telemetry debug overlay action', () => {
    afterEach(() => {
      setTelemetryDebugEnabled(false);
      jest.restoreAllMocks();
    });

    it('offers a toggle action in development mode', () => {
      jest.spyOn(AppConfig, 'gl2DevMode').mockReturnValue(true);
      setTelemetryDebugEnabled(false);

      const { result } = renderHookWithDataRouter(() => useNavItems(), { wrapper: Wrapper });

      const item = result.current.find(({ title }) => title === 'Enable Telemetry Debug Overlay');

      expect(item).toEqual(expect.objectContaining({ type: ACTION_TYPE }));

      (item as { action: (args: object) => void }).action({});

      expect(isTelemetryDebugEnabled()).toBe(true);
    });

    it('offers to disable the overlay while it is enabled', () => {
      jest.spyOn(AppConfig, 'gl2DevMode').mockReturnValue(true);
      setTelemetryDebugEnabled(true);

      const { result } = renderHookWithDataRouter(() => useNavItems(), { wrapper: Wrapper });

      expect(result.current.find(({ title }) => title === 'Disable Telemetry Debug Overlay')).toBeDefined();
    });

    it('is absent outside development mode', () => {
      jest.spyOn(AppConfig, 'gl2DevMode').mockReturnValue(false);

      const { result } = renderHookWithDataRouter(() => useNavItems(), { wrapper: Wrapper });

      expect(result.current.find(({ title }) => title?.includes('Telemetry Debug Overlay'))).toBeUndefined();
    });
  });
});
