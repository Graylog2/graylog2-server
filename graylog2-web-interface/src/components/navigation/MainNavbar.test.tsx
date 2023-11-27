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
import { render, screen } from 'wrappedTestingLibrary';
import Immutable from 'immutable';
import * as React from 'react';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import { defaultUser } from 'defaultMockValues';

import AppConfig from 'util/AppConfig';
import { asMock } from 'helpers/mocking';
import useCurrentUser from 'hooks/useCurrentUser';
import { adminUser } from 'fixtures/users';
import PerspectivesProvider from 'components/perspectives/contexts/PerspectivesProvider';
import PerspectivesBindings from 'components/perspectives/bindings';

import MainNavbar from './MainNavbar';

jest.mock('hooks/useCurrentUser');

describe('MainNavbar', () => {
  const SUT = (props: Partial<React.ComponentProps<typeof MainNavbar>>) => (
    <PerspectivesProvider>
      <MainNavbar pathname="/" {...props} />
    </PerspectivesProvider>
  );

  beforeAll(() => {
    PluginStore.register(new PluginManifest({}, PerspectivesBindings));
  });

  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(defaultUser);
  });

  describe('renders custom navigation elements supplied by plugins', () => {
    const plugin = {
      metadata: { name: 'DummyPlugin ' },
      exports: {
        navigation: [
          { path: '/something', description: 'Perpetuum Mobile' },
          { path: '/system/archives', description: 'Archives', permissions: 'archive:read' },
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
              { path: '/newpluginroute', description: 'New dropdown route', requiredFeatureFlag: 'enable_dropdown_nav_item' },
            ],
          },
        ],
      },
    };

    beforeEach(() => {
      AppConfig.gl2AppPathPrefix = jest.fn(() => '');
      AppConfig.isFeatureEnabled = jest.fn(() => false);
      PluginStore.register(plugin);
    });

    afterEach(() => {
      PluginStore.unregister(plugin);
    });

    it('contains top-level navigation element', async () => {
      render(<SUT />);

      await screen.findByRole('link', { name: /perpetuum mobile/i });
    });

    it('prefix plugin navigation item paths with app prefix', async () => {
      asMock(AppConfig.gl2AppPathPrefix).mockReturnValue('/my/crazy/prefix');
      render(<SUT />);

      expect(await screen.findByRole('link', { name: /perpetuum mobile/i })).toHaveAttribute('href', '/my/crazy/prefix/something');
    });

    it('does not contain navigation elements from plugins where permissions are missing', () => {
      asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder()
        .permissions(Immutable.List([]))
        .build());

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
      asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder()
        .permissions(Immutable.List(['archive:read']))
        .build());

      render(<SUT />);

      await screen.findByRole('link', { name: /archives/i });
    });

    it('does not render dropdown contributed by plugin if permissions for all elements are missing', () => {
      asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder()
        .permissions(Immutable.List([]))
        .build());

      render(<SUT />);

      expect(screen.queryByRole('button', { name: /neat stuff/i })).not.toBeInTheDocument();
    });

    it('renders dropdown contributed by plugin if permissions are sufficient', async () => {
      asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder()
        .permissions(Immutable.List(['somethingelse', 'completelydifferent']))
        .build());

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
      asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder()
        .permissions(Immutable.List(['somethingelse', 'completelydifferent']))
        .build());

      render(<SUT pathname="/somethingelse" />);

      await screen.findByRole('button', { name: /neat stuff \/ something else/i });
    });
  });

  describe('uses correct permissions:', () => {
    it('should not show `Enterprise` item if user is lacking permissions', () => {
      asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder()
        .permissions(Immutable.List())
        .build());

      render(<SUT />);

      expect(screen.queryByRole('link', { name: /enterprise/i })).not.toBeInTheDocument();
    });

    it('should show `Enterprise` item if user has permission to read license', async () => {
      asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder()
        .permissions(Immutable.List(['licenseinfos:read']))
        .build());

      render(<SUT />);

      expect(await screen.findByRole('link', { name: /enterprise/i })).toBeInTheDocument();
    });
  });
});
