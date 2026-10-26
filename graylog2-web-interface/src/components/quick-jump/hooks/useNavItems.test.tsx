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
import { adminUser, alice } from 'fixtures/users';
import type User from 'logic/users/User';
import { ScratchpadContext } from 'contexts/ScratchpadProvider';
import { asMock } from 'helpers/mocking';
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

const wrapperFor =
  (user: User) =>
  ({ children }: { children: React.ReactNode }) => (
    <ScratchpadContext.Provider
      value={{
        isScratchpadVisible: true,
        localStorageItem: 'gl-scratchpad-jest',
        setScratchpadVisibility: jest.fn(),
        toggleScratchpadVisibility: jest.fn(),
      }}>
      <CurrentUserContext.Provider value={user}>{children}</CurrentUserContext.Provider>
    </ScratchpadContext.Provider>
  );

const Wrapper = wrapperFor(adminUser);

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

  const creatorTitles = () => {
    const { result } = renderHookWithDataRouter(() => useNavItems(), { wrapper: Wrapper });

    return result.current.map(({ title }) => title);
  };

  it('offers entity creators without a required feature flag', () => {
    asMock(AppConfig.isFeatureEnabled).mockReturnValue(false);

    expect(creatorTitles()).toContain('Create widget');
  });

  it('offers entity creators whose required feature flag is enabled', () => {
    asMock(AppConfig.isFeatureEnabled).mockImplementation((feature) => feature === 'gadgets');

    expect(creatorTitles()).toContain('Create gadget');
  });

  it('hides entity creators whose required feature flag is disabled', () => {
    asMock(AppConfig.isFeatureEnabled).mockReturnValue(false);

    expect(creatorTitles()).not.toContain('Create gadget');
  });
});

describe('useNavItems main navigation', () => {
  const navigation: PluginExports = {
    navigation: [
      { description: 'Plain link', path: prefixUrl('/plain') },
      { description: 'Flagged link', path: prefixUrl('/flagged'), requiredFeatureFlag: 'links' },
      {
        description: 'Menu',
        children: [
          { description: 'Plain child', path: prefixUrl('/menu/plain') },
          { description: 'Flagged child', path: prefixUrl('/menu/flagged'), requiredFeatureFlag: 'children' },
        ],
      },
      {
        description: 'Flagged menu',
        requiredFeatureFlag: 'menus',
        children: [{ description: 'Child', path: prefixUrl('/flagged-menu/child') }],
      },
    ],
  };

  usePluginExports(navigation);

  const navigationTitles = () => {
    const { result } = renderHookWithDataRouter(() => useNavItems(), { wrapper: Wrapper });

    return result.current.map(({ title }) => title);
  };

  it('offers navigation items without a required feature flag', () => {
    asMock(AppConfig.isFeatureEnabled).mockReturnValue(false);

    expect(navigationTitles()).toEqual(expect.arrayContaining(['Plain link', 'Menu / Plain child']));
  });

  it('offers navigation items whose required feature flag is enabled', () => {
    asMock(AppConfig.isFeatureEnabled).mockReturnValue(true);

    expect(navigationTitles()).toEqual(
      expect.arrayContaining(['Flagged link', 'Menu / Flagged child', 'Flagged menu / Child']),
    );
  });

  it('hides navigation items whose required feature flag is disabled', () => {
    asMock(AppConfig.isFeatureEnabled).mockReturnValue(false);

    const titles = navigationTitles();

    expect(titles).not.toContain('Flagged link');
    expect(titles).not.toContain('Menu / Flagged child');
  });

  it('hides the children of a dropdown whose required feature flag is disabled', () => {
    asMock(AppConfig.isFeatureEnabled).mockImplementation((feature) => feature !== 'menus');

    expect(navigationTitles()).not.toContain('Flagged menu / Child');
  });
});

describe('useNavItems page navigation', () => {
  // The first page of a group is the group's own page and is never offered on its own.
  const pageNavigation: PluginExports = {
    pageNavigation: [
      {
        description: 'Group',
        children: [
          { description: 'Overview', path: prefixUrl('/group') },
          { description: 'Plain page', path: prefixUrl('/group/plain') },
          { description: 'Flagged page', path: prefixUrl('/group/flagged'), requiredFeatureFlag: 'pages' },
        ],
      },
    ],
  };

  usePluginExports(pageNavigation);

  const pageTitles = () => {
    const { result } = renderHookWithDataRouter(() => useNavItems(), { wrapper: Wrapper });

    return result.current.map(({ title }) => title);
  };

  it('offers pages whose required feature flag is enabled', () => {
    asMock(AppConfig.isFeatureEnabled).mockReturnValue(true);

    expect(pageTitles()).toEqual(expect.arrayContaining(['Group / Plain page', 'Group / Flagged page']));
  });

  it('hides pages whose required feature flag is disabled', () => {
    asMock(AppConfig.isFeatureEnabled).mockReturnValue(false);

    const titles = pageTitles();

    expect(titles).toContain('Group / Plain page');
    expect(titles).not.toContain('Group / Flagged page');
  });
});

describe('useNavItems conditions', () => {
  const met = () => true;
  const notMet = () => false;

  const plugins: PluginExports = {
    navigation: [
      { description: 'Shown link', path: prefixUrl('/shown'), useCondition: met },
      { description: 'Hidden link', path: prefixUrl('/hidden'), useCondition: notMet },
      {
        description: 'Menu',
        children: [
          { description: 'Shown child', path: prefixUrl('/menu/shown'), useCondition: met },
          { description: 'Hidden child', path: prefixUrl('/menu/hidden'), useCondition: notMet },
        ],
      },
      {
        description: 'Hidden menu',
        useCondition: notMet,
        children: [{ description: 'Child', path: prefixUrl('/hidden-menu/child') }],
      },
    ],
    pageNavigation: [
      {
        description: 'Group',
        children: [
          { description: 'Overview', path: prefixUrl('/group') },
          { description: 'Shown page', path: prefixUrl('/group/shown'), useCondition: met },
          { description: 'Hidden page', path: prefixUrl('/group/hidden'), useCondition: notMet },
        ],
      },
    ],
  };

  usePluginExports(plugins);

  const titles = () => {
    const { result } = renderHookWithDataRouter(() => useNavItems(), { wrapper: Wrapper });

    return result.current.map(({ title }) => title);
  };

  it('offers navigation items and pages whose condition is met', () => {
    expect(titles()).toEqual(expect.arrayContaining(['Shown link', 'Menu / Shown child', 'Group / Shown page']));
  });

  it('hides navigation items and pages whose condition is not met', () => {
    const result = titles();

    expect(result).not.toContain('Hidden link');
    expect(result).not.toContain('Menu / Hidden child');
    expect(result).not.toContain('Group / Hidden page');
  });

  it('hides the children of a dropdown whose condition is not met', () => {
    expect(titles()).not.toContain('Hidden menu / Child');
  });
});

describe('useNavItems dropdown permissions', () => {
  const navigation: PluginExports = {
    navigation: [
      {
        description: 'Admin menu',
        permissions: 'roles:read',
        children: [{ description: 'Child', path: prefixUrl('/admin-menu/child') }],
      },
    ],
  };

  usePluginExports(navigation);

  const titlesFor = (user: User) => {
    const { result } = renderHookWithDataRouter(() => useNavItems(), { wrapper: wrapperFor(user) });

    return result.current.map(({ title }) => title);
  };

  it('offers the children of a dropdown the user is permitted to see', () => {
    expect(titlesFor(adminUser)).toContain('Admin menu / Child');
  });

  it('hides the children of a dropdown the user is not permitted to see', () => {
    expect(titlesFor(alice)).not.toContain('Admin menu / Child');
  });
});
