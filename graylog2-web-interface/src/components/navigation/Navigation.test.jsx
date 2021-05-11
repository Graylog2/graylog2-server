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
import mockComponent from 'helpers/mocking/MockComponent';
import { viewsManager } from 'fixtures/users';

import Routes from 'routing/Routes';
import AppConfig from 'util/AppConfig';
import CurrentUserContext from 'contexts/CurrentUserContext';

jest.mock('./SystemMenu', () => mockComponent('SystemMenu'));
jest.mock('./NavigationBrand', () => mockComponent('NavigationBrand'));
jest.mock('./NavigationLink', () => mockComponent('NavigationLink'));
jest.mock('./ScratchpadToggle', () => mockComponent('ScratchpadToggle'));
jest.mock('components/throughput/GlobalThroughput', () => mockComponent('GlobalThroughput'));
jest.mock('components/navigation/NotificationBadge', () => mockComponent('NotificationBadge'));

jest.mock('util/AppConfig', () => ({
  gl2AppPathPrefix: jest.fn(() => ''),
  gl2ServerUrl: jest.fn(() => undefined),
  gl2DevMode: jest.fn(() => false),
  isFeatureEnabled: jest.fn(() => false),
  isCloud: jest.fn(() => false),
}));

jest.mock('routing/withLocation', () => (x) => x);

const currentUser = viewsManager;
const findLink = (wrapper, title) => wrapper.find(`NavigationLink[description="${title}"]`);

jest.mock('./DevelopmentHeaderBadge', () => () => <span />);

describe('Navigation', () => {
  let Navigation;

  // We can't use prop types here, they are not compatible with mount and require in this case
  // eslint-disable-next-line react/prop-types
  const SimpleNavigation = ({ component: Component, permissions, ...props }) => (
    <CurrentUserContext.Provider value={{ ...currentUser, permissions }}>
      <Component {...props} />
    </CurrentUserContext.Provider>
  );

  SimpleNavigation.defaultProps = {
    location: { pathname: '/' },
    permissions: [],
  };

  beforeEach(() => {
    // eslint-disable-next-line global-require
    Navigation = require('./Navigation').default;
  });

  describe('has common elements', () => {
    let wrapper;

    beforeEach(() => {
      wrapper = mount(<SimpleNavigation component={Navigation} />);
    });

    it('contains brand icon', () => {
      const brand = wrapper.find('NavbarBrand');

      expect(brand).toExist();
      expect(brand.find('LinkContainer')).toHaveProp('to', Routes.STARTPAGE);
      expect(brand.find('NavigationBrand')).toExist();
    });

    it('contains user menu including correct user details', () => {
      const usermenu = wrapper.find('UserMenu');

      expect(usermenu).toHaveProp('userId', currentUser.id);
      expect(usermenu).toHaveProp('fullName', currentUser.full_name);
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
            children: [
              { path: '/somethingelse', description: 'Something Else', permissions: 'somethingelse' },
              { path: '/completelydiffrent', description: 'Completely Different', permissions: 'completelydifferent' },
            ],
          },
        ],
      },
    };

    beforeEach(() => {
      AppConfig.gl2AppPathPrefix = jest.fn(() => '');
      PluginStore.register(plugin);
    });

    afterEach(() => {
      PluginStore.unregister(plugin);
    });

    it('contains top-level navigation element', () => {
      const wrapper = mount(<SimpleNavigation component={Navigation} />);

      expect(findLink(wrapper, 'Perpetuum Mobile')).toExist();
    });

    it('prefix plugin navigation item paths with app prefix', () => {
      AppConfig.gl2AppPathPrefix.mockReturnValue('/my/crazy/prefix');
      const wrapper = mount(<SimpleNavigation component={Navigation} />);

      expect(findLink(wrapper, 'Perpetuum Mobile')).toHaveProp('path', '/my/crazy/prefix/something');
    });

    it('does not contain navigation elements from plugins where permissions are missing', () => {
      const wrapper = mount(<SimpleNavigation component={Navigation} />);

      expect(findLink(wrapper, 'Archives')).not.toExist();
    });

    it('contains restricted navigation elements from plugins if permissions are present', () => {
      const wrapper = mount(<SimpleNavigation component={Navigation}
                                              permissions={['archive:read']} />);

      expect(findLink(wrapper, 'Archives')).toExist();
    });

    it('does not render dropdown contributed by plugin if permissions for all elements are missing', () => {
      const wrapper = mount(<SimpleNavigation component={Navigation} />);

      expect(wrapper.find('NavDropdown[title="Neat Stuff"]')).not.toExist();
    });

    it('renders dropdown contributed by plugin if permissions are sufficient', () => {
      const wrapper = mount(<SimpleNavigation component={Navigation}
                                              permissions={['somethingelse', 'completelydifferent']} />);

      expect(wrapper.find('NavDropdown[title="Neat Stuff"]')).toExist();
    });

    it('sets dropdown title based on match', () => {
      const wrapper = mount(<SimpleNavigation component={Navigation}
                                              location={{ pathname: '/somethingelse' }}
                                              permissions={['somethingelse', 'completelydifferent']} />);

      expect(wrapper.find('NavDropdown[title="Neat Stuff / Something Else"]')).toExist();
    });
  });

  describe('uses correct permissions:', () => {
    const verifyPermissions = ({ permissions, count, links }) => {
      const wrapper = mount(<SimpleNavigation component={Navigation}
                                              permissions={permissions} />);
      const navigationLinks = wrapper.find('NavItem');

      expect(navigationLinks).toHaveLength(count);

      links.forEach((title) => expect(wrapper.find(`NavItem[children="${title}"]`)).toExist());
    };

    it.each`
    permissions                    | count | links
    ${[]}                          | ${5}  | ${['Search', 'Streams', 'Alerts', 'Dashboards']}
  `('shows $links for user with $permissions permissions', verifyPermissions);

    it('should not show `Enterprise` item if user is lacking permissions', () => {
      const wrapper = mount(<SimpleNavigation component={Navigation}
                                              permissions={[]} />);

      expect(wrapper.find('NavigationLink[description="Enterprise"]')).not.toExist();
    });

    it('should show `Enterprise` item if user has permission to read license', () => {
      const wrapper = mount(<SimpleNavigation component={Navigation}
                                              permissions={['licenseinfos:read']} />);

      expect(wrapper.find('NavigationLink[description="Enterprise"]')).toExist();
    });
  });
});
