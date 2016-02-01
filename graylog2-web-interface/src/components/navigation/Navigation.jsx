import React from 'react';
import Reflux from 'reflux';
import { Navbar, Nav, NavItem, NavDropdown, MenuItem } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import PermissionsMixin from 'util/PermissionsMixin';
import Routes from 'routing/Routes';

import NotificationsStore from 'stores/notifications/NotificationsStore';
import { PluginStore } from 'graylog-web-plugin/plugin';

import GlobalThroughput from 'components/throughput/GlobalThroughput';
import UserMenu from 'components/navigation/UserMenu';
import HelpMenu from 'components/navigation/HelpMenu';

const Navigation = React.createClass({
  propTypes: {
    requestPath: React.PropTypes.string.isRequired,
    loginName: React.PropTypes.string.isRequired,
    fullName: React.PropTypes.string.isRequired,
    permissions: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,
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
    return this.props.requestPath.indexOf(prefix) === 0;
  },

  _systemTitle() {
    let suffix = '';

    if (this._isActive('/system') || this._isActive('/system?page=')) {
      suffix = ' / Overview';
    }
    if (this._isActive('/system/nodes') || this._isActive('/system/radios')) {
      suffix = ' / Nodes';
    }
    if (this._isActive('/system/inputs')) {
      suffix = ' / Inputs';
    }
    if (this._isActive('/system/outputs')) {
      suffix = ' / Outputs';
    }
    if (this._isActive('/system/indices')) {
      suffix = ' / Indices';
    }
    if (this._isActive('/system/logging')) {
      suffix = ' / Logging';
    }
    if (this._isActive('/system/users')) {
      suffix = ' / Users';
    }
    if (this._isActive('/system/roles')) {
      suffix = ' / Roles';
    }
    if (this._isActive('/system/contentpacks')) {
      suffix = ' / Content Packs';
    }
    if (this._isActive('/system/grokpatterns')) {
      suffix = ' / Grok Patterns';
    }
    if (this._isActive('/system/collectors')) {
      suffix = ' / Collectors';
    }

    return 'System' + suffix;
  },

  render() {
    const logoUrl = require('images/toplogo.png');
    const brand = (
      <LinkContainer to={Routes.STARTPAGE}>
        <a><img src={logoUrl}/></a>
      </LinkContainer>);
    // TODO: fix permission names

    let notificationBadge;

    if (this.state.total > 0) {
      notificationBadge = (
        <LinkContainer to={Routes.SYSTEM.OVERVIEW}>
          <span className="badge" style={{backgroundColor: '#ff3b00'}} id="notification-badge">{this.state.total}</span>
        </LinkContainer>
      );
    }

    const pluginNavigations = PluginStore.exports('navigation')
      .map((pluginRoute) => {
        return (
          <LinkContainer key={pluginRoute.path} to={pluginRoute.path}>
            <NavItem>{pluginRoute.description}</NavItem>
          </LinkContainer>
        );
      });

    const pluginSystemNavigations = PluginStore.exports('systemnavigation')
      .map((pluginRoute) => {
        return (
          <LinkContainer key={pluginRoute.path} to={pluginRoute.path}>
            <NavItem>{pluginRoute.description}</NavItem>
          </LinkContainer>
        );
      });

    return (
      <Navbar inverse fluid fixedTop>
        <Navbar.Header>
          <Navbar.Brand>{brand}</Navbar.Brand>
        </Navbar.Header>
        <Navbar.Collapse eventKey={0}>
          <Nav navbar>
            {this.isPermitted(this.props.permissions, ['SEARCHES_ABSOLUTE', 'SEARCHES_RELATIVE', 'SEARCHES_KEYWORD']) &&
              <LinkContainer to={Routes.SEARCH}>
                <NavItem to="search">Search</NavItem>
              </LinkContainer>
            }
            <LinkContainer to={Routes.STREAMS}>
              <NavItem>Streams</NavItem>
            </LinkContainer>

            <LinkContainer to={Routes.DASHBOARDS}>
              <NavItem >Dashboards</NavItem>
            </LinkContainer>

            {this.isPermitted(this.props.permissions, ['SOURCES_READ']) &&
              <LinkContainer to={Routes.SOURCES}>
                <NavItem>Sources</NavItem>
              </LinkContainer>
            }

            {pluginNavigations}

            <NavDropdown navItem title={this._systemTitle()} id="system-menu-dropdown">
              <LinkContainer to={Routes.SYSTEM.OVERVIEW}>
                <MenuItem>Overview</MenuItem>
              </LinkContainer>
              <LinkContainer to={Routes.SYSTEM.NODES.LIST}>
                <MenuItem>Nodes</MenuItem>
              </LinkContainer>
              {this.isPermitted(this.props.permissions, ['INPUTS_READ']) &&
                <LinkContainer to={Routes.SYSTEM.INPUTS}>
                  <MenuItem>Inputs</MenuItem>
                </LinkContainer>
              }
              {this.isPermitted(this.props.permissions, ['OUTPUTS_READ']) &&
                <LinkContainer to={Routes.SYSTEM.OUTPUTS}>
                  <MenuItem>Outputs</MenuItem>
                </LinkContainer>
              }
              {this.isPermitted(this.props.permissions, ['COLLECTORS_READ']) &&
                <LinkContainer to={Routes.SYSTEM.COLLECTORS}>
                  <MenuItem>Collectors</MenuItem>
                </LinkContainer>
              }
              {this.isPermitted(this.props.permissions, ['INDICES_READ']) &&
                <LinkContainer to={Routes.SYSTEM.INDICES.LIST}>
                  <MenuItem>Indices</MenuItem>
                </LinkContainer>
              }
              {this.isPermitted(this.props.permissions, ['LOGGERS_READ']) &&
                <LinkContainer to={Routes.SYSTEM.LOGGING}>
                  <MenuItem>Logging</MenuItem>
                </LinkContainer>
              }
              {this.isPermitted(this.props.permissions, ['USERS_READ']) &&
                <LinkContainer to={Routes.SYSTEM.USERS.LIST}>
                  <MenuItem>Users</MenuItem>
                </LinkContainer>
              }
              {this.isPermitted(this.props.permissions, ['ROLES_READ']) &&
                <LinkContainer to={Routes.SYSTEM.ROLES}>
                  <MenuItem>Roles</MenuItem>
                </LinkContainer>
              }
              {this.isPermitted(this.props.permissions, ['DASHBOARDS_CREATE', 'INPUTS_CREATE', 'STREAMS_CREATE']) &&
              <LinkContainer to={Routes.SYSTEM.CONTENTPACKS.LIST}>
                <MenuItem>Content Packs</MenuItem>
              </LinkContainer>
              }
              {this.isPermitted(this.props.permissions, ['INPUTS_EDIT']) &&
              <LinkContainer to={Routes.SYSTEM.GROKPATTERNS}>
                <MenuItem>Grok Patterns</MenuItem>
              </LinkContainer>
              }
              {pluginSystemNavigations}
            </NavDropdown>
          </Nav>

          <Nav navbar>
            <NavItem className="notification-badge-link">
              {notificationBadge}
            </NavItem>
          </Nav>

          <Nav navbar pullRight>
            {/* Needed to replace NavItem with `li` and `a` elements to avoid LinkContainer setting NavItem as active */}
            {/* More information here: https://github.com/react-bootstrap/react-router-bootstrap/issues/134 */}
            <li role="presentation" className="">
              <LinkContainer to={Routes.SYSTEM.NODES.LIST}>
                <a>
                  <GlobalThroughput />
                </a>
              </LinkContainer>
            </li>
            <HelpMenu active={this._isActive(Routes.GETTING_STARTED)}/>
            <UserMenu fullName={this.props.fullName} loginName={this.props.loginName}/>
            {typeof(DEVELOPMENT) !== 'undefined' && DEVELOPMENT ?
              <NavItem className="notification-badge-link">
              <span className="badge" style={{backgroundColor: '#ff3b00'}}>DEV</span>
              </NavItem>
              : null}
          </Nav>
        </Navbar.Collapse>
      </Navbar>
    );
  },
});

export default Navigation;
