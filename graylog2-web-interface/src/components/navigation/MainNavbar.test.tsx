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
import { render, screen, within } from 'wrappedTestingLibrary';
import Immutable from 'immutable';
import * as React from 'react';
import type { PluginExports } from 'graylog-web-plugin/plugin';
import { PluginStore } from 'graylog-web-plugin/plugin';
import { defaultUser } from 'defaultMockValues';
import userEvent from '@testing-library/user-event';

import AppConfig from 'util/AppConfig';
import { asMock } from 'helpers/mocking';
import useCurrentUser from 'hooks/useCurrentUser';
import { adminUser } from 'fixtures/users';
import { itemStateIndicatorSelector } from 'components/common/NavItemStateIndicator';

import MainNavbar from './MainNavbar';

jest.mock('hooks/useCurrentUser');

describe('MainNavbar', () => {
  const SUT = (props: Partial<React.ComponentProps<typeof MainNavbar>>) => <MainNavbar pathname="/" {...props} />;

  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(defaultUser);
  });

  describe('renders custom navigation elements supplied by plugins', () => {
    const ARCHIVES_LINK_TITLE = 'Archives';

    const plugin = {
      metadata: { name: 'DummyPlugin ' },
      exports: {
        navigation: [
          { path: '/after/test', description: 'After specified item', position: { after: ARCHIVES_LINK_TITLE } },
          { path: '/something', description: 'Perpetuum Mobile' },
          { path: '/system/archives', description: ARCHIVES_LINK_TITLE, permissions: 'archive:read' },
          {
            description: 'Neat Stuff',
            path: '/',
            children: [
              { path: '/somethingelse', description: 'Something Else', permissions: 'somethingelse' },
              { path: '/completelydiffrent', description: 'Completely Different', permissions: 'completelydifferent' },
            ],
          },
          {
            description: 'Feature flag test',
            path: '/',
            requiredFeatureFlag: 'enable_main_nav_item',
          },
          {
            description: 'Feature flag dropdown test',
            path: '/',
            children: [
              {
                path: '/newpluginroute',
                description: 'New dropdown route',
                requiredFeatureFlag: 'enable_dropdown_nav_item',
              },
            ],
          },
          {
            description: 'Merged dropdown test',
            path: '/',
            children: [{ path: '/another-route', description: 'Dropdown menu item 1' }],
          },
          {
            description: 'Merged dropdown test',
            path: '/',
            children: [{ path: '/just-another-route', description: 'Dropdown menu item 2' }],
          },
        ],
      } as PluginExports,
    };

    beforeEach(() => {
      asMock(AppConfig.gl2AppPathPrefix).mockReturnValue('/');
      asMock(AppConfig.isFeatureEnabled).mockReturnValue(false);
      PluginStore.register(plugin);
    });

    afterEach(() => {
      PluginStore.unregister(plugin);
    });

    it('contains top-level navigation element', async () => {
      render(<SUT />);

      await screen.findByRole('link', { name: /perpetuum mobile/i });
    });

    it('does not prefix plugin navigation item paths with app prefix', async () => {
      asMock(AppConfig.gl2AppPathPrefix).mockReturnValue('/my/crazy/prefix');
      render(<SUT />);

      expect(await screen.findByRole('link', { name: /perpetuum mobile/i })).toHaveAttribute('href', '/something');
    });

    it('does not contain navigation elements from plugins where permissions are missing', () => {
      asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder().permissions(Immutable.List([])).build());

      render(<SUT />);

      expect(screen.queryByRole('link', { name: /archives/i })).not.toBeInTheDocument();
    });

    it('does not contain navigation elements from plugins when elements require a feature flag to be enabled', () => {
      render(<SUT />);

      expect(screen.queryByRole('link', { name: /feature flag test/i })).not.toBeInTheDocument();
    });

    it('contains navigation elements from plugins when elements require a feature flag which is enabled', async () => {
      asMock(AppConfig.isFeatureEnabled).mockReturnValue(true);
      render(<SUT />);

      await screen.findByRole('link', { name: /feature flag test/i });
    });

    it('contains restricted navigation elements from plugins if permissions are present', async () => {
      asMock(useCurrentUser).mockReturnValue(
        adminUser
          .toBuilder()
          // @ts-expect-error
          .permissions(Immutable.List(['archive:read']))
          .build(),
      );

      render(<SUT />);

      await screen.findByRole('link', { name: /archives/i });
    });

    it('does not render dropdown contributed by plugin if permissions for all elements are missing', () => {
      asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder().permissions(Immutable.List([])).build());

      render(<SUT />);

      expect(screen.queryByRole('button', { name: /neat stuff/i })).not.toBeInTheDocument();
    });

    it('renders dropdown contributed by plugin if permissions are sufficient', async () => {
      asMock(useCurrentUser).mockReturnValue(
        adminUser
          .toBuilder()
          // @ts-expect-error
          .permissions(Immutable.List(['somethingelse', 'completelydifferent']))
          .build(),
      );

      render(<SUT />);

      await screen.findByRole('button', { name: /neat stuff/i });
    });

    it('does not render dropdown contributed by plugin if required feature flag is not enabled', () => {
      render(<SUT />);

      expect(screen.queryByRole('button', { name: /feature flag dropdown test/i })).not.toBeInTheDocument();
    });

    it('renders dropdown contributed by plugin if required feature flag is enabled', async () => {
      asMock(AppConfig.isFeatureEnabled).mockReturnValue(true);
      render(<SUT />);

      await screen.findByRole('button', { name: /feature flag dropdown test/i });
    });

    it('sets dropdown title based on match', async () => {
      asMock(useCurrentUser).mockReturnValue(
        adminUser
          .toBuilder()
          // @ts-expect-error
          .permissions(Immutable.List(['somethingelse', 'completelydifferent']))
          .build(),
      );

      render(<SUT pathname="/somethingelse" />);

      await screen.findByRole('button', { name: /neat stuff \/ something else/i });
    });

    it('should merge navigation dropdowns when their description is equal', async () => {
      render(<SUT />);

      await userEvent.click(await screen.findByRole('button', { name: /Merged dropdown test/i }));

      await screen.findByRole('menuitem', { name: /Dropdown menu item 1/i });
      await screen.findByRole('menuitem', { name: /Dropdown menu item 2/i });
    });

    describe('uses correct position', () => {
      it('should render an item after a specified item', async () => {
        render(<SUT />);

        const targetItem = await screen.findByRole('link', { name: ARCHIVES_LINK_TITLE });
        const itemWithPosition = await screen.findByRole('link', { name: /After specified item/i });

        expect(itemWithPosition.compareDocumentPosition(targetItem)).toBe(2);
      });
    });

    it('keeps the expanded menu from shrinking, so that its measured width stays meaningful', async () => {
      render(<SUT />);

      await screen.findByRole('link', { name: /perpetuum mobile/i });

      expect(screen.getByRole('list')).toHaveStyleRule('flex', '0 0 auto');
    });

    // Bootstrap's base stylesheet, which is still loaded, underlines anchors on hover. Its own nav
    // rules used to suppress that, but they no longer apply to the navigation bar, so a hovered link
    // would otherwise show a line right beneath its text.
    it('does not underline a plain link on hover', async () => {
      render(<SUT />);

      expect(await screen.findByRole('link', { name: /perpetuum mobile/i })).toHaveStyleRule(
        'text-decoration',
        'none',
        { modifier: ':hover' },
      );
    });

    // `NavItem` renders the state indicator for every navigation item, but only the item itself can
    // decide when to show it. A dropdown trigger does so; a plain link has to do the same, or it ends
    // up with no hover or active state at all.
    describe('state indicator of a plain link', () => {
      const listItemFor = async (name: RegExp) => {
        await screen.findByRole('link', { name });

        return screen.getAllByRole('listitem').find((item) => within(item).queryByRole('link', { name }));
      };

      it('is shown while the link is hovered', async () => {
        render(<SUT />);

        expect(await listItemFor(/perpetuum mobile/i)).toHaveStyleRule('border-color', /.+/, {
          modifier: `> a:hover ${itemStateIndicatorSelector}`,
        });
      });

      it('is shown while the link is the active route', async () => {
        render(<SUT />);

        expect(await listItemFor(/perpetuum mobile/i)).toHaveStyleRule('border-color', /.+/, {
          modifier: `> a.active ${itemStateIndicatorSelector}`,
        });
      });
    });

    describe('when collapsed', () => {
      const openBurgerMenu = async () =>
        userEvent.click(await screen.findByRole('button', { name: 'Toggle navigation' }));

      it('replaces the navigation items with a burger toggle', async () => {
        render(<SUT collapsed />);

        await screen.findByRole('button', { name: 'Toggle navigation' });

        expect(screen.queryByRole('link', { name: /perpetuum mobile/i })).not.toBeInTheDocument();
      });

      it('shows top-level items after opening the burger menu', async () => {
        render(<SUT collapsed />);

        await openBurgerMenu();

        await screen.findByRole('menuitem', { name: /perpetuum mobile/i });
      });

      it('shows the children of a nested item after opening its submenu', async () => {
        asMock(useCurrentUser).mockReturnValue(
          adminUser
            .toBuilder()
            // @ts-expect-error
            .permissions(Immutable.List(['somethingelse', 'completelydifferent']))
            .build(),
        );

        render(<SUT collapsed />);

        await openBurgerMenu();

        const submenu = await screen.findByRole('menuitem', { name: /neat stuff/i });

        expect(screen.queryByRole('menuitem', { name: /something else/i })).not.toBeInTheDocument();

        await userEvent.click(submenu);

        await screen.findByRole('menuitem', { name: /something else/i });
      });

      it('omits items the user is not permitted to see', async () => {
        asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder().permissions(Immutable.List([])).build());

        render(<SUT collapsed />);

        await openBurgerMenu();

        expect(screen.queryByRole('menuitem', { name: /archives/i })).not.toBeInTheDocument();
      });

      it('omits items which require a feature flag that is not enabled', async () => {
        render(<SUT collapsed />);

        await openBurgerMenu();

        expect(screen.queryByRole('menuitem', { name: /feature flag test/i })).not.toBeInTheDocument();
      });
    });
  });

  describe('uses correct permissions:', () => {
    it('should not show `Enterprise` item if user is lacking permissions', () => {
      asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder().permissions(Immutable.List()).build());

      render(<SUT />);

      expect(screen.queryByRole('link', { name: /enterprise/i })).not.toBeInTheDocument();
    });

    it('should show `Enterprise` item if user has permission to read license', async () => {
      asMock(useCurrentUser).mockReturnValue(
        adminUser
          .toBuilder()
          .permissions(Immutable.List(['licenseinfos:read']))
          .build(),
      );

      render(<SUT />);

      await screen.findByRole('link', { name: /enterprise/i });
    });
  });
});
