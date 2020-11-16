import React from 'react';
import { mount } from 'wrappedEnzyme';
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

describe('SystemMenu', () => {
  let currentUser;
  let exports;
  beforeEach(() => {
    currentUser = { permissions: [] };
    const CurrentUserStore = {
      getInitialState: jest.fn(() => ({ currentUser })),
      listen: jest.fn(),
    };
    jest.doMock('injection/StoreProvider', () => ({ getStore: () => CurrentUserStore }));
    exports = {};
    const PluginStore = { exports: jest.fn((key) => exports[key] || []) };
    jest.doMock('graylog-web-plugin/plugin', () => ({ PluginStore }));
    AppConfig.gl2AppPathPrefix = jest.fn(() => '');
  });
  describe('uses correct permissions:', () => {
    let SystemMenu;
    beforeEach(() => {
      // eslint-disable-next-line global-require
      SystemMenu = require('./SystemMenu');
    });
    const verifyPermissions = ({ permissions, count, links }) => {
      currentUser.permissions = permissions;
      const wrapper = mount(<SystemMenu location={{ pathname: '/' }} />);
      const navigationLinks = wrapper.find('NavigationLink');
      expect(navigationLinks).toHaveLength(count);
      containsAllLinks(navigationLinks, links);
    };
    it.each`
    permissions                    | count | links
    ${[]}                          | ${2}  | ${['Overview', 'Nodes']}
    ${['clusterconfigentry:read']} | ${3}  | ${['Configurations']}
    ${['inputs:read']}             | ${3}  | ${['Inputs']}
    ${['outputs:read']}            | ${3}  | ${['Outputs']}
    ${['indices:read']}            | ${3}  | ${['Indices']}
    ${['loggers:read']}            | ${3}  | ${['Logging']}
    ${['users:list']}              | ${3}  | ${['Authentication']}
    ${['roles:read']}              | ${3}  | ${['Authentication']}
    ${['dashboards:create', 'inputs:create', 'streams:create']} | ${4}  | ${['Content Packs']}
    ${['inputs:edit']}             | ${5}  | ${['Grok Patterns', 'Lookup Tables', 'Sidecars']}
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
      SystemMenu = require('./SystemMenu');
    });
    afterEach(() => {
      exports.systemnavigation = [];
    });
    it('includes plugin item in system navigation', () => {
      const wrapper = mount(<SystemMenu location={{ pathname: '/' }} />);
      containsLink(wrapper, 'Audit Log');
      expect(findLink(wrapper, 'Audit Log')).toHaveProp('path', '/system/auditlog');
      expect(wrapper.find('NavigationLink[description="Licenses"]')).not.toExist();
    });
    it('includes plugin item in system navigation if required permissions are present', () => {
      currentUser.permissions = ['inputs:create'];
      const wrapper = mount(<SystemMenu location={{ pathname: '/' }} />);
      containsLink(wrapper, 'Audit Log');
      containsLink(wrapper, 'Licenses');
    });
    it('does not include plugin item in system navigation if required permissions are not present', () => {
      currentUser.permissions = [];
      const wrapper = mount(<SystemMenu location={{ pathname: '/' }} />);
      expect(findLink(wrapper, 'Licenses')).not.toExist();
    });
    it('prefixes plugin path with current application path prefix', () => {
      AppConfig.gl2AppPathPrefix = jest.fn(() => '/my/fancy/prefix');
      const wrapper = mount(<SystemMenu location={{ pathname: '/' }} />);
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
      SystemMenu = require('./SystemMenu');
    });
    afterEach(() => {
      exports.systemnavigation = [];
    });
    it('uses a default title if location is not matched', () => {
      const wrapper = mount(<SystemMenu location={{ pathname: '/' }} />);
      expect(wrapper.find('NavDropdown').at(1)).toHaveProp('title', 'System');
    });
    it('uses a custom title if location is matched', () => {
      const wrapper = mount(<SystemMenu location={{ pathname: '/system/overview' }} />);
      expect(wrapper.find('NavDropdown').at(1)).toHaveProp('title', 'System / Overview');
    });
    it('uses a custom title for a plugin route if location is matched', () => {
      const wrapper = mount(<SystemMenu location={{ pathname: '/system/licenses' }} />);
      expect(wrapper.find('NavDropdown').at(1)).toHaveProp('title', 'System / Licenses');
    });
  });
});
