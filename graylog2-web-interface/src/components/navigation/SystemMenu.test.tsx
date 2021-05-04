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
import { admin } from 'fixtures/users';

import CurrentUserContext from 'contexts/CurrentUserContext';

import AppConfig from '../../util/AppConfig';

const findLink = (wrapper, title) => wrapper.find(`NavigationLink[description="${title}"]`);
const containsLink = (wrapper, title, length = 1) => expect(findLink(wrapper, title)).toHaveLength(length);
const containsAllLinks = (wrapper, titles) => titles.forEach((title) => containsLink(wrapper, title));

jest.mock('util/AppConfig', () => ({
  gl2AppPathPrefix: jest.fn(() => ''),
  gl2ServerUrl: jest.fn(() => undefined),
  gl2DevMode: jest.fn(() => false),
  isCloud: jest.fn(() => false),
}));

jest.mock('routing/withLocation', () => (x) => x);

describe('SystemMenu', () => {
  let exports;

  beforeEach(() => {
    exports = {};

    const PluginStore = { exports: jest.fn((key) => exports[key] || []) };

    jest.doMock('graylog-web-plugin/plugin', () => ({ PluginStore }));
    AppConfig.gl2AppPathPrefix = jest.fn(() => '');
  });

  const SimpleSystemMenu = ({ permissions, component: Component, location }: { permissions?: Array<string>, component: any, location?: { pathname: string }}) => (
    <CurrentUserContext.Provider value={{ ...admin, permissions: permissions ?? [] }}>
      <Component location={location} />
    </CurrentUserContext.Provider>
  );

  SimpleSystemMenu.defaultProps = {
    permissions: [],
    location: { pathname: '/' },
  };

  describe('uses correct permissions:', () => {
    let SystemMenu;

    beforeEach(() => {
      // eslint-disable-next-line global-require
      SystemMenu = require('./SystemMenu').default;
    });

    const verifyPermissions = ({ permissions, count, links }) => {
      const wrapper = mount(<SimpleSystemMenu component={SystemMenu} permissions={permissions} />);
      const navigationLinks = wrapper.find('NavigationLink');

      expect(navigationLinks).toHaveLength(count);

      containsAllLinks(navigationLinks, links);
    };

    it.each`
    permissions                    | count | links
    ${[]}                          | ${2}  | ${['Overview', 'Nodes']}
    ${['clusterconfigentry:read']} | ${3}  | ${['Configurations']}
    ${['inputs:read']}             | ${4}  | ${['Inputs', 'Grok Patterns']}
    ${['outputs:read']}            | ${3}  | ${['Outputs']}
    ${['indices:read']}            | ${3}  | ${['Indices']}
    ${['loggers:read']}            | ${3}  | ${['Logging']}
    ${['authentication:edit']}     | ${3}  | ${['Authentication']}
    ${['users:list']}              | ${3}  | ${['Users and Teams']}
    ${['roles:read']}              | ${3}  | ${['Roles']}
    ${['dashboards:create', 'inputs:create', 'streams:create']} | ${4}  | ${['Content Packs']}
    ${['inputs:edit']}             | ${3}  | ${['Lookup Tables']}
    ${['sidecars:read']}           | ${3}  | ${['Sidecars']}
    ${['inputs:create']}           | ${3}  | ${['Pipelines']}
  `('shows $links for user with $permissions permissions', verifyPermissions);
  });

  describe('uses items from plugins:', () => {
    let SystemMenu;

    beforeEach(() => {
      exports.systemnavigation = [
        { path: '/system/licenses', description: 'Licenses', permissions: 'inputs:create' },
        { path: '/system/auditlog', description: 'Audit Log' },
      ];

      // eslint-disable-next-line global-require
      SystemMenu = require('./SystemMenu').default;
    });

    afterEach(() => {
      exports.systemnavigation = [];
    });

    it('includes plugin item in system navigation', () => {
      const wrapper = mount(<SimpleSystemMenu component={SystemMenu} />);
      containsLink(wrapper, 'Audit Log');

      expect(findLink(wrapper, 'Audit Log')).toHaveProp('path', '/system/auditlog');
      expect(wrapper.find('NavigationLink[description="Licenses"]')).not.toExist();
    });

    it('includes plugin item in system navigation if required permissions are present', () => {
      const wrapper = mount(<SimpleSystemMenu component={SystemMenu} permissions={['inputs:create']} />);

      containsLink(wrapper, 'Audit Log');
      containsLink(wrapper, 'Licenses');
    });

    it('does not include plugin item in system navigation if required permissions are not present', () => {
      const wrapper = mount(<SimpleSystemMenu component={SystemMenu} permissions={[]} />);

      expect(findLink(wrapper, 'Licenses')).not.toExist();
    });

    it('prefixes plugin path with current application path prefix', () => {
      AppConfig.gl2AppPathPrefix = jest.fn(() => '/my/fancy/prefix');
      const wrapper = mount(<SimpleSystemMenu component={SystemMenu} />);

      expect(findLink(wrapper, 'Audit Log')).toHaveProp('path', '/my/fancy/prefix/system/auditlog');
    });
  });

  describe('sets a location-specific title for the dropdown', () => {
    let SystemMenu;

    beforeEach(() => {
      exports.systemnavigation = [
        { path: '/system/licenses', description: 'Licenses', permissions: 'inputs:create' },
        { path: '/system/auditlog', description: 'Audit Log' },
      ];

      // eslint-disable-next-line global-require
      SystemMenu = require('./SystemMenu').default;
    });

    afterEach(() => {
      exports.systemnavigation = [];
    });

    it('uses a default title if location is not matched', () => {
      const wrapper = mount(<SimpleSystemMenu component={SystemMenu} />);

      expect(wrapper.find('NavDropdown').at(1)).toHaveProp('title', 'System');
    });

    it('uses a custom title if location is matched', () => {
      const wrapper = mount(<SimpleSystemMenu component={SystemMenu} location={{ pathname: '/system/overview' }} />);

      expect(wrapper.find('NavDropdown').at(1)).toHaveProp('title', 'System / Overview');
    });

    it('uses a custom title for a plugin route if location is matched', () => {
      const wrapper = mount(<SimpleSystemMenu component={SystemMenu} location={{ pathname: '/system/licenses' }} />);

      expect(wrapper.find('NavDropdown').at(1)).toHaveProp('title', 'System / Licenses');
    });
  });
});
