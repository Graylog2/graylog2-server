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
import { prefixUrl } from 'routing/Routes';

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
});

describe('useNavItems entity creators', () => {
  const entityCreators: PluginExports = {
    entityCreators: [
      { id: 'Widget', title: 'Create widget', path: prefixUrl('/widgets/new') },
      {
        id: 'Gadget',
        title: 'Create gadget',
        path: prefixUrl('/gadgets/new'),
        requiredFeatureFlag: 'gadgets',
      },
    ],
  };

  usePluginExports(entityCreators);

  afterEach(() => {
    jest.restoreAllMocks();
  });

  const creatorTitles = () => {
    const { result } = renderHookWithDataRouter(() => useNavItems(), { wrapper: Wrapper });

    return result.current.map(({ title }) => title);
  };

  it('offers entity creators without a required feature flag', () => {
    jest.spyOn(AppConfig, 'isFeatureEnabled').mockReturnValue(false);

    expect(creatorTitles()).toContain('Create widget');
  });

  it('offers entity creators whose required feature flag is enabled', () => {
    jest.spyOn(AppConfig, 'isFeatureEnabled').mockImplementation((feature) => feature === 'gadgets');

    expect(creatorTitles()).toContain('Create gadget');
  });

  it('hides entity creators whose required feature flag is disabled', () => {
    jest.spyOn(AppConfig, 'isFeatureEnabled').mockReturnValue(false);

    expect(creatorTitles()).not.toContain('Create gadget');
  });
});
