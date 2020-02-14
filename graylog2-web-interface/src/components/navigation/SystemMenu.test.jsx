import React from 'react';
import { mount } from 'wrappedEnzyme';
import AppConfig from '../../util/AppConfig';

const findLink = (wrapper, title) => wrapper.find(`NavigationLink[description="${title}"]`);
const containsLink = (wrapper, title, length = 1) => expect(findLink(wrapper, title)).toHaveLength(length);
const containsAllLinks = (wrapper, titles) => titles.forEach(title => containsLink(wrapper, title));

jest.mock('util/AppConfig', () => ({
  gl2AppPathPrefix: jest.fn(() => ''),
  gl2ServerUrl: jest.fn(() => undefined),
  gl2DevMode: jest.fn(() => false),
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
    const PluginStore = { exports: jest.fn(key => exports[key] || []) };
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
    ${[]}                          | ${3}  | ${['Overview', 'Nodes', 'Enterprise']}
    ${['clusterconfigentry:read']} | ${4}  | ${['Configurations']}
    ${['inputs:read']}             | ${5}  | ${['Inputs']}
    ${['outputs:read']}            | ${4}  | ${['Outputs']}
    ${['indices:read']}            | ${4}  | ${['Indices']}
    ${['loggers:read']}            | ${4}  | ${['Logging']}
    ${['users:list']}              | ${4}  | ${['Authentication']}
    ${['roles:read']}              | ${4}  | ${['Authentication']}
    ${['contentpack:read']}        | ${4}  | ${['Content Packs']}
    ${['inputs:read']}             | ${5}  | ${['Grok Patterns']}
    ${['lookuptables:read']}       | ${4}  | ${['Lookup Tables']}
    ${['sidecars:read', 'sidecar_collectors:read', 'sidecar_collector_configurations:read']}>       | ${4}  | ${['Sidecars']}
    ${['pipeline:read', 'pipeline_rule:read']} | ${4}  | ${['Pipelines']}
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
