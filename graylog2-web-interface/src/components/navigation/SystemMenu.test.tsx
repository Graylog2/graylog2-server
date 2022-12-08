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
import * as Immutable from 'immutable';
import { mount } from 'wrappedEnzyme';
import { useLocation } from 'react-router-dom';
import type { Location } from 'history';
import type { ReactWrapper } from 'enzyme';

import { asMock } from 'helpers/mocking';
import useCurrentUser from 'hooks/useCurrentUser';
import { adminUser } from 'fixtures/users';

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

jest.mock('hooks/useCurrentUser');

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useLocation: jest.fn(),
}));

describe('SystemMenu', () => {
  let exports;

  beforeEach(() => {
    exports = {};

    const PluginStore = { exports: jest.fn((key) => exports[key] || []) };

    jest.doMock('graylog-web-plugin/plugin', () => ({ PluginStore }));
    AppConfig.gl2AppPathPrefix = jest.fn(() => '');
    asMock(useLocation).mockReturnValue({ pathname: '/' } as Location<{ pathname: string }>);
    asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder().permissions(Immutable.List([])).build());
  });

  describe('uses correct permissions:', () => {
    let SystemMenu;

    beforeEach(() => {
      // eslint-disable-next-line global-require
      SystemMenu = require('./SystemMenu').default;
    });

    it.each`
    permissions                    | count | links
    ${[]}                          | ${2}  | ${['Overview', 'Nodes']}
    ${['clusterconfigentry:read']} | ${3}  | ${['Configurations']}
    ${['inputs:read']}             | ${3}  | ${['Inputs']}
    ${['grok_pattern:read']}       | ${3}  | ${['Grok Patterns']}
    ${['outputs:read']}            | ${3}  | ${['Outputs']}
    ${['indices:read']}            | ${3}  | ${['Indices']}
    ${['loggers:read']}            | ${3}  | ${['Logging']}
    ${['authentication:edit']}     | ${3}  | ${['Authentication']}
    ${['users:list']}              | ${3}  | ${['Users and Teams']}
    ${['roles:read']}              | ${3}  | ${['Roles']}
    ${['contentpack:read']}        | ${3}  | ${['Content Packs']}
    ${['lookuptables:read']}       | ${3}  | ${['Lookup Tables']}
    ${['sidecars:read']}           | ${3}  | ${['Sidecars']}
    ${['pipeline:read', 'pipeline_connection:read']} | ${3}  | ${['Pipelines']}
  `('shows $links for user with $permissions permissions', ({ permissions, count, links }) => {
      asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder()
        .permissions(Immutable.List(permissions))
        .build());

      const wrapper = mount(<SystemMenu />);
      const navigationLinks = wrapper.find('NavigationLink');

      expect(navigationLinks).toHaveLength(count);

      containsAllLinks(navigationLinks, links);
    });
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
      const wrapper = mount(<SystemMenu />);
      containsLink(wrapper, 'Audit Log');

      expect(findLink(wrapper, 'Audit Log')).toHaveProp('path', '/system/auditlog');
      expect(wrapper.find('NavigationLink[description="Licenses"]')).not.toExist();
    });

    it('includes plugin item in system navigation if required permissions are present', () => {
      asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder()
        .permissions(Immutable.List(['inputs:create']))
        .build());

      const wrapper = mount(<SystemMenu />);

      expect(findLink(wrapper, 'Audit Log')).toHaveLength(1);
      expect(findLink(wrapper, 'Licenses')).toHaveLength(1);
    });

    it('does not include plugin item in system navigation if required permissions are not present', () => {
      asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder()
        .permissions(Immutable.List([]))
        .build());

      const wrapper = mount(<SystemMenu />);

      expect(findLink(wrapper, 'Licenses')).not.toExist();
    });

    it('prefixes plugin path with current application path prefix', () => {
      AppConfig.gl2AppPathPrefix = jest.fn(() => '/my/fancy/prefix');
      const wrapper = mount(<SystemMenu />);

      expect(findLink(wrapper, 'Audit Log')).toHaveProp('path', '/my/fancy/prefix/system/auditlog');
    });
  });

  describe('sets a location-specific title for the dropdown', () => {
    const getDropdownTitle = (wrapper: ReactWrapper) => wrapper.find('NavDropdown')
      .at(1)
      .prop<React.ReactElement>('title')
      .props.children;
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
      const wrapper = mount(<SystemMenu />);

      expect(getDropdownTitle(wrapper)).toBe('System');
    });

    it('uses a custom title if location is matched', () => {
      asMock(useLocation).mockReturnValue({ pathname: '/system/overview' } as Location<{ pathname: string }>);
      const wrapper = mount(<SystemMenu />);

      expect(getDropdownTitle(wrapper)).toBe('System / Overview');
    });

    it('uses a custom title for a plugin route if location is matched', () => {
      asMock(useLocation).mockReturnValue({ pathname: '/system/licenses' } as Location<{ pathname: string }>);
      const wrapper = mount(<SystemMenu />);

      expect(getDropdownTitle(wrapper)).toBe('System / Licenses');
    });
  });
});
