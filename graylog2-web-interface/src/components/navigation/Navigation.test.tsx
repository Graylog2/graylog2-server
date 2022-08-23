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
import { mount } from 'wrappedEnzyme';
import { PluginStore } from 'graylog-web-plugin/plugin';
import { Route, MemoryRouter, useLocation } from 'react-router-dom';
import Immutable from 'immutable';
import type { Location } from 'history';

import mockComponent from 'helpers/mocking/MockComponent';
import { adminUser } from 'fixtures/users';
import { asMock } from 'helpers/mocking';
import Routes from 'routing/Routes';
import AppConfig from 'util/AppConfig';
import Navigation from 'components/navigation/Navigation';
import useCurrentUser from 'hooks/useCurrentUser';

jest.mock('./SystemMenu', () => mockComponent('SystemMenu'));
jest.mock('./NavigationBrand', () => mockComponent('NavigationBrand'));
jest.mock('./NavigationLink', () => mockComponent('NavigationLink'));
jest.mock('./ScratchpadToggle', () => mockComponent('ScratchpadToggle'));
jest.mock('components/throughput/GlobalThroughput', () => mockComponent('GlobalThroughput'));
jest.mock('components/navigation/NotificationBadge', () => mockComponent('NotificationBadge'));
jest.mock('hooks/useCurrentUser');
jest.mock('./DevelopmentHeaderBadge', () => () => <span />);
jest.mock('routing/withLocation', () => (x) => x);

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useLocation: jest.fn(() => ({
    pathname: '',
  })),
}));

jest.mock('util/AppConfig', () => ({
  gl2AppPathPrefix: jest.fn(() => ''),
  gl2ServerUrl: jest.fn(() => undefined),
  gl2DevMode: jest.fn(() => false),
  isFeatureEnabled: jest.fn(() => false),
  isCloud: jest.fn(() => false),
}));

describe('Navigation', () => {
  const findLink = (wrapper, title) => wrapper.find(`NavigationLink[description="${title}"]`);

  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(adminUser);
    asMock(useLocation).mockReturnValue({ pathname: '/' } as Location<{ pathname: string }>);
  });

  describe('has common elements', () => {
    let wrapper;

    beforeEach(() => {
      asMock(useCurrentUser).mockReturnValue(adminUser);
      wrapper = mount(<Navigation />);
    });

    it('contains brand icon', () => {
      const brand = wrapper.find('NavbarBrand');

      expect(brand).toExist();
      expect(brand.find('LinkContainer')).toHaveProp('to', Routes.STARTPAGE);
      expect(brand.find('NavigationBrand')).toExist();
    });

    it('contains user menu including correct user details', () => {
      const usermenu = wrapper.find('UserMenu');

      expect(usermenu).toHaveProp('userId', adminUser.id);
      expect(usermenu).toHaveProp('fullName', adminUser.fullName);
    });

    it('contains help menu', () => {
      expect(wrapper.find('HelpMenu')).toExist();
    });

    it('contains global throughput', () => {
      expect(wrapper.find('GlobalThroughput')).toExist();
    });

    it('contains notification badge', () => {
      expect(wrapper.find('NotificationBadge')).toExist();
    });
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

    it('contains top-level navigation element', () => {
      const wrapper = mount(<Navigation />);

      expect(findLink(wrapper, 'Perpetuum Mobile')).toExist();
    });

    it('prefix plugin navigation item paths with app prefix', () => {
      asMock(AppConfig.gl2AppPathPrefix).mockReturnValue('/my/crazy/prefix');
      const wrapper = mount(<Navigation />);

      expect(findLink(wrapper, 'Perpetuum Mobile')).toHaveProp('path', '/my/crazy/prefix/something');
    });

    it('does not contain navigation elements from plugins where permissions are missing', () => {
      asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder()
        .permissions(Immutable.List([]))
        .build());

      const wrapper = mount(<Navigation />);

      expect(findLink(wrapper, 'Archives')).not.toExist();
    });

    it('does not contain navigation elements from plugins when elements require a feature flag to be enabled', () => {
      const wrapper = mount(<Navigation />);

      expect(findLink(wrapper, 'Feature flag test')).not.toExist();
    });

    it('contains navigation elements from plugins when elements require a feature flag which is enabled', () => {
      asMock(AppConfig.isFeatureEnabled).mockReturnValue(true);
      const wrapper = mount(<Navigation />);

      expect(findLink(wrapper, 'Feature flag test')).toExist();
    });

    it('contains restricted navigation elements from plugins if permissions are present', () => {
      asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder()
        .permissions(Immutable.List(['archive:read']))
        .build());

      const wrapper = mount(<Navigation />);

      expect(findLink(wrapper, 'Archives')).toExist();
    });

    it('does not render dropdown contributed by plugin if permissions for all elements are missing', () => {
      asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder()
        .permissions(Immutable.List([]))
        .build());

      const wrapper = mount(<Navigation />);

      expect(wrapper.find('NavDropdown[title="Neat Stuff"]')).not.toExist();
    });

    it('renders dropdown contributed by plugin if permissions are sufficient', () => {
      asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder()
        .permissions(Immutable.List(['somethingelse', 'completelydifferent']))
        .build());

      const wrapper = mount(<Navigation />);

      expect(wrapper.find('NavDropdown[title="Neat Stuff"]')).toExist();
    });

    it('does not render dropdown contributed by plugin if required feature flag is not enabled', () => {
      const wrapper = mount(<Navigation />);

      expect(wrapper.find('NavDropdown[title="Feature flag dropdown test"]')).not.toExist();
    });

    it('renders dropdown contributed by plugin if required feature flag is enabled', () => {
      asMock(AppConfig.isFeatureEnabled).mockReturnValue(true);
      const wrapper = mount(<Navigation />);

      expect(wrapper.find('NavDropdown[title="Feature flag dropdown test"]')).toExist();
    });

    it('sets dropdown title based on match', () => {
      asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder()
        .permissions(Immutable.List(['somethingelse', 'completelydifferent']))
        .build());

      asMock(useLocation).mockReturnValue({ pathname: '/somethingelse' } as Location<{ pathname: string }>);

      const wrapper = mount((
        <MemoryRouter initialEntries={['/somethingelse']}>
          <Route path="/somethingelse">
            <Navigation />
          </Route>
        </MemoryRouter>
      ));

      expect(wrapper.find('NavDropdown[title="Neat Stuff / Something Else"]')).toExist();
    });
  });

  describe('uses correct permissions:', () => {
    const verifyPermissions = ({ count, links }) => {
      const wrapper = mount(<Navigation />);
      const navigationLinks = wrapper.find('NavItem');

      expect(navigationLinks).toHaveLength(count);

      links.forEach((title) => expect(wrapper.find(`NavItem[children="${title}"]`)).toExist());
    };

    // eslint-disable-next-line jest/expect-expect
    it.each`
    permissions                    | count | links
    ${[]}                          | ${5}  | ${['Search', 'Streams', 'Alerts', 'Dashboards']}
  `('shows $links for user with $permissions permissions', verifyPermissions);

    it('should not show `Enterprise` item if user is lacking permissions', () => {
      asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder()
        .permissions(Immutable.List())
        .build());

      const wrapper = mount(<Navigation />);

      expect(wrapper.find('NavigationLink[description="Enterprise"]')).not.toExist();
    });

    it('should show `Enterprise` item if user has permission to read license', () => {
      asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder()
        .permissions(Immutable.List(['licenseinfos:read']))
        .build());

      const wrapper = mount(<Navigation />);

      expect(wrapper.find('NavigationLink[description="Enterprise"]')).toExist();
    });
  });
});
