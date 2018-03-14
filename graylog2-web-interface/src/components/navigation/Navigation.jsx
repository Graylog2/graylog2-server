import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { Badge, Navbar, Nav, NavItem, NavDropdown, MenuItem } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import naturalSort from 'javascript-natural-sort';

import PermissionsMixin from 'util/PermissionsMixin';
import Routes from 'routing/Routes';
import URLUtils from 'util/URLUtils';
import AppConfig from 'util/AppConfig';

import StoreProvider from 'injection/StoreProvider';
const NotificationsStore = StoreProvider.getStore('Notifications');

import { PluginStore } from 'graylog-web-plugin/plugin';

import GlobalThroughput from 'components/throughput/GlobalThroughput';
import UserMenu from 'components/navigation/UserMenu';
import HelpMenu from 'components/navigation/HelpMenu';
import { IfPermitted } from 'components/common';
import NavigationBrand from './NavigationBrand';
import InactiveNavItem from './InactiveNavItem';

import badgeStyles from 'components/bootstrap/Badge.css';

const Navigation = createReactClass({
  displayName: 'Navigation',

  propTypes: {
    requestPath: PropTypes.string.isRequired,
    loginName: PropTypes.string.isRequired,
    fullName: PropTypes.string.isRequired,
    permissions: PropTypes.arrayOf(PropTypes.string).isRequired,
  },

  mixins: [PermissionsMixin, Reflux.connect(NotificationsStore)],

  componentDidMount() {
    this.interval = setInterval(NotificationsStore.list, this.POLL_INTERVAL);
  },

  componentWillUnmount() {
    clearInterval(this.interval);
  },

  POLL_INTERVAL: 3000,

  _isActive(prefix) {
    return this.props.requestPath.indexOf(URLUtils.appPrefixed(prefix)) === 0;
  },

  _systemTitle() {
    const prefix = 'System';

    if (this._isActive('/system/overview')) {
      return `${prefix} / Overview`;
    }
    if (this._isActive('/system/nodes')) {
      return `${prefix} / Nodes`;
    }
    if (this._isActive('/system/inputs')) {
      return `${prefix} / Inputs`;
    }
    if (this._isActive('/system/outputs')) {
      return `${prefix} / Outputs`;
    }
    if (this._isActive('/system/indices')) {
      return `${prefix} / Indices`;
    }
    if (this._isActive('/system/logging')) {
      return `${prefix} / Logging`;
    }
    if (this._isActive('/system/authentication')) {
      return `${prefix} / Authentication`;
    }
    if (this._isActive('/system/contentpacks')) {
      return `${prefix} / Content Packs`;
    }
    if (this._isActive('/system/grokpatterns')) {
      return `${prefix} / Grok Patterns`;
    }
    if (this._isActive('/system/lookuptables')) {
      return `${prefix} / Lookup Tables`;
    }
    if (this._isActive('/system/configurations')) {
      return `${prefix} / Configurations`;
    }
    if (this._isActive('/system/pipelines')) {
      return `${prefix} / Pipelines`;
    }
    if (this._isActive('/system/enterprise')) {
      return `${prefix} / Enterprise`;
    }

    const pluginRoute = PluginStore.exports('systemnavigation').filter(route => this._isActive(route.path))[0];
    if (pluginRoute) {
      return `${prefix} / ${pluginRoute.description}`;
    }

    return prefix;
  },

  _shouldAddPluginRoute(pluginRoute) {
    return !pluginRoute.permissions || (pluginRoute.permissions && this.isPermitted(this.props.permissions, pluginRoute.permissions));
  },

  render() {
    let notificationBadge;

    if (this.state.total > 0) {
      notificationBadge = (
        <Nav navbar>
          <LinkContainer to={Routes.SYSTEM.OVERVIEW}>
            <InactiveNavItem className="notification-badge-link">
              <Badge className={badgeStyles.badgeDanger} id="notification-badge">{this.state.total}</Badge>
            </InactiveNavItem>
          </LinkContainer>
        </Nav>
      );
    }

    const pluginNavigations = PluginStore.exports('navigation')
      .sort((route1, route2) => naturalSort(route1.description.toLowerCase(), route2.description.toLowerCase()))
      .map((pluginRoute) => {
        if (this._shouldAddPluginRoute(pluginRoute)) {
          return (
            <LinkContainer key={pluginRoute.path} to={URLUtils.appPrefixed(pluginRoute.path)}>
              <NavItem>{pluginRoute.description}</NavItem>
            </LinkContainer>
          );
        }
        return null;
      });

    const pluginSystemNavigations = PluginStore.exports('systemnavigation')
      .sort((route1, route2) => naturalSort(route1.description.toLowerCase(), route2.description.toLowerCase()))
      .map((pluginRoute) => {
        if (this._shouldAddPluginRoute(pluginRoute)) {
          return (
            <LinkContainer key={pluginRoute.path} to={URLUtils.appPrefixed(pluginRoute.path)}>
              <MenuItem>{pluginRoute.description}</MenuItem>
            </LinkContainer>
          );
        }
        return null;
      });

    return (
      <Navbar inverse fluid fixedTop>
        <Navbar.Header>
          <Navbar.Brand>
            <LinkContainer to={Routes.STARTPAGE}>
              <NavigationBrand />
            </LinkContainer>
          </Navbar.Brand>
          <Navbar.Toggle />
        </Navbar.Header>
        <Navbar.Collapse>
          <Nav navbar>
            <IfPermitted permissions={['searches:absolute', 'searches:relative', 'searches:keyword']}>
              <LinkContainer to={Routes.SEARCH}>
                <NavItem to="search">Search</NavItem>
              </LinkContainer>
            </IfPermitted>

            <LinkContainer to={Routes.STREAMS}>
              <NavItem>Streams</NavItem>
            </LinkContainer>

            <LinkContainer to={Routes.ALERTS.LIST}>
              <NavItem>Alerts</NavItem>
            </LinkContainer>

            <LinkContainer to={Routes.DASHBOARDS}>
              <NavItem >Dashboards</NavItem>
            </LinkContainer>

            <IfPermitted permissions="sources:read">
              <LinkContainer to={Routes.SOURCES}>
                <NavItem>Sources</NavItem>
              </LinkContainer>
            </IfPermitted>

            {pluginNavigations}

            {/*
             * We cannot use IfPermitted in the dropdown unless we modify it to clone children elements and pass
             * props down to them. NavDropdown is passing some props needed in MenuItems that are being blocked
             * by IfPermitted.
             */}
            <NavDropdown title={this._systemTitle()} id="system-menu-dropdown">
              <LinkContainer to={Routes.SYSTEM.OVERVIEW}>
                <MenuItem>Overview</MenuItem>
              </LinkContainer>
              {this.isPermitted(this.props.permissions, ['clusterconfigentry:read']) &&
              <LinkContainer to={Routes.SYSTEM.CONFIGURATIONS}>
                <MenuItem>Configurations</MenuItem>
              </LinkContainer>
              }
              <LinkContainer to={Routes.SYSTEM.NODES.LIST}>
                <MenuItem>Nodes</MenuItem>
              </LinkContainer>
              {this.isPermitted(this.props.permissions, ['inputs:read']) &&
              <LinkContainer to={Routes.SYSTEM.INPUTS}>
                <MenuItem>Inputs</MenuItem>
              </LinkContainer>
              }
              {this.isPermitted(this.props.permissions, ['outputs:read']) &&
              <LinkContainer to={Routes.SYSTEM.OUTPUTS}>
                <MenuItem>Outputs</MenuItem>
              </LinkContainer>
              }
              {this.isPermitted(this.props.permissions, ['indices:read']) &&
              <LinkContainer to={Routes.SYSTEM.INDICES.LIST}>
                <MenuItem>Indices</MenuItem>
              </LinkContainer>
              }
              {this.isPermitted(this.props.permissions, ['loggers:read']) &&
              <LinkContainer to={Routes.SYSTEM.LOGGING}>
                <MenuItem>Logging</MenuItem>
              </LinkContainer>
              }
              {this.isAnyPermitted(this.props.permissions, ['users:list', 'roles:read']) &&
              <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.OVERVIEW}>
                <MenuItem>Authentication</MenuItem>
              </LinkContainer>
              }
              {this.isPermitted(this.props.permissions, ['dashboards:create', 'inputs:create', 'streams:create']) &&
              <LinkContainer to={Routes.SYSTEM.CONTENTPACKS.LIST}>
                <MenuItem>Content Packs</MenuItem>
              </LinkContainer>
              }
              {this.isPermitted(this.props.permissions, ['inputs:edit']) &&
              <LinkContainer to={Routes.SYSTEM.GROKPATTERNS}>
                <MenuItem>Grok Patterns</MenuItem>
              </LinkContainer>
              }
              {this.isPermitted(this.props.permissions, ['inputs:edit']) &&
              <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.OVERVIEW}>
                <MenuItem>Lookup Tables</MenuItem>
              </LinkContainer>
              }
              {this.isPermitted(this.props.permissions, ['inputs:create']) &&
              <LinkContainer to={Routes.SYSTEM.PIPELINES.OVERVIEW}>
                <MenuItem>Pipelines</MenuItem>
              </LinkContainer>
              }
              <LinkContainer to={Routes.SYSTEM.ENTERPRISE}>
                  <MenuItem>Enterprise</MenuItem>
              </LinkContainer>
              {pluginSystemNavigations}
            </NavDropdown>
          </Nav>

          {notificationBadge}

          <Nav navbar pullRight>
            <LinkContainer to={Routes.SYSTEM.NODES.LIST}>
              <InactiveNavItem><GlobalThroughput /></InactiveNavItem>
            </LinkContainer>
            <HelpMenu active={this._isActive(Routes.GETTING_STARTED)} />
            <UserMenu fullName={this.props.fullName} loginName={this.props.loginName} />
            {AppConfig.gl2DevMode() ?
              <NavItem className="notification-badge-link">
                <Badge className={badgeStyles.badgeDanger}>DEV</Badge>
              </NavItem>
              : null}
          </Nav>
        </Navbar.Collapse>
      </Navbar>
    );
  },
});

export default Navigation;
