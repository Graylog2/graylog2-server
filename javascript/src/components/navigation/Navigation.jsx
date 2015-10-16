/* global jsRoutes */

import React from 'react';
import { Navbar, CollapsibleNav, Nav, NavBrand, NavItem, NavDropdown, MenuItem } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import GlobalThroughput from 'components/throughput/GlobalThroughput';
import UserMenu from './UserMenu';
import Routes from 'routing/Routes';

const Navigation = React.createClass({
  render() {
    const logoUrl = require('images/toplogo.png');
    const brand = (
      <LinkContainer to={Routes.HOME}>
        <img src={logoUrl}/>
      </LinkContainer>);
    return (
      <Navbar inverse fluid fixedTop toggleNavKey={0}>
        <NavBrand>{brand}</NavBrand>
        <CollapsibleNav eventKey={0}>
          <Nav navbar>
            {this._isPermitted(['SEARCHES_ABSOLUTE', 'SEARCHES_RELATIVE', 'SEARCHES_KEYWORD']) &&
              <NavItem to="search">Search</NavItem>
            }
            <LinkContainer to={Routes.STREAMS}>
              <NavItem>Streams</NavItem>
            </LinkContainer>

            <LinkContainer to={Routes.DASHBOARDS}>
              <NavItem >Dashboards</NavItem>
            </LinkContainer>

            {this._isPermitted(['SOURCES_READ']) &&
              <LinkContainer to={Routes.SOURCES}>
                <NavItem>Sources</NavItem>
              </LinkContainer>
            }
            <NavDropdown navItem title="System" id="system-menu-dropdown">
              <LinkContainer to={Routes.SYSTEM.COLLECTORS}>
                <MenuItem>Collectors</MenuItem>
              </LinkContainer>
              <LinkContainer to={Routes.SYSTEM.NODES}>
                <MenuItem>Nodes</MenuItem>
              </LinkContainer>
              <LinkContainer to={Routes.SYSTEM.OVERVIEW}>
                <MenuItem>Overview</MenuItem>
              </LinkContainer>
              <LinkContainer to={Routes.SYSTEM.OUTPUTS}>
                <MenuItem>Outputs</MenuItem>
              </LinkContainer>
              <LinkContainer to={Routes.SYSTEM.ROLES}>
                <MenuItem>Roles</MenuItem>
              </LinkContainer>
            </NavDropdown>
          </Nav>

          <Nav navbar>
            <LinkContainer to={Routes.SYSTEM.OVERVIEW}>
              <NavItem className="notification-badge-link">
                <span className="badge" style={{backgroundColor: '#ff3b00'}} id="notification-badge"></span>
              </NavItem>
            </LinkContainer>
          </Nav>

          <Nav navbar right>
            <LinkContainer to={Routes.SYSTEM.NODES}>
              <NavItem>
                <GlobalThroughput />
              </NavItem>
            </LinkContainer>
            <UserMenu fullName={this.props.fullName} loginName={this.props.loginName}/>
          </Nav>
        </CollapsibleNav>
      </Navbar>
    );
  },

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
  _isPermitted(permissions) {
    //return this.props.permissions.every((p) => this.state.permissions[p]);
    return true;
  },
});

export default Navigation;
