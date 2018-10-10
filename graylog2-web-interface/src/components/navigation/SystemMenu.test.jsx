import React from 'react';
import { mount } from 'enzyme';

const findLink = (wrapper, title) => wrapper.find(`NavigationLink[description="${title}"]`);
const containsLink = (wrapper, title) => expect(findLink(wrapper, title)).toHaveLength(1);
const containsAllLinks = (wrapper, titles) => titles.forEach(title => containsLink(wrapper, title));

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
    ${['inputs:read']}             | ${4}  | ${['Inputs']}
    ${['outputs:read']}            | ${4}  | ${['Outputs']}
    ${['indices:read']}            | ${4}  | ${['Indices']}
    ${['loggers:read']}            | ${4}  | ${['Logging']}
    ${['users:list']}              | ${4}  | ${['Authentication']}
    ${['roles:read']}              | ${4}  | ${['Authentication']}
    ${['dashboards:create', 'inputs:create', 'streams:create']} | ${5}  | ${['Content Packs']}
    ${['inputs:edit']}             | ${6}  | ${['Grok Patterns', 'Lookup Tables', 'Sidecars']}
    ${['inputs:create']}           | ${4}  | ${['Pipelines']}
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
      expect(wrapper.find('NavDropdown')).toHaveProp('title', 'System');
    });
    it('uses a custom title if location is matched', () => {
      const wrapper = mount(<SystemMenu location={{ pathname: '/system/overview' }} />);
      expect(wrapper.find('NavDropdown')).toHaveProp('title', 'System / Overview');
    });
    it('uses a custom title for a plugin route if location is matched', () => {
      const wrapper = mount(<SystemMenu location={{ pathname: '/system/licenses' }} />);
      expect(wrapper.find('NavDropdown')).toHaveProp('title', 'System / Licenses');
    });
  });
});
