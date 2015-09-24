/* global jsRoutes */

import React from 'react';
import { Navbar, CollapsibleNav, Nav, NavBrand, NavItem, DropdownButton, MenuItem } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import GlobalThroughput from 'components/throughput/GlobalThroughput';
import UserMenu from './UserMenu';
import Routes from 'routing/Routes';

const Navigation = React.createClass({
  render() {
    const logoUrl = jsRoutes.controllers.Assets.at('images/toplogo.png').url;
    const homeUrl = jsRoutes.controllers.StartpageController.redirect().url;
    const brand = (<a href={homeUrl}>
      <img src={logoUrl}/>
    </a>);
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

            <NavItem href={jsRoutes.controllers.DashboardsController.index().url}
                     active={this._isActive('/dashboards')}>Dashboards</NavItem>

            {this._isPermitted(['SOURCES_READ']) &&
              <NavItem href={jsRoutes.controllers.SourcesController.list().url}
                       active={this._isActive("/sources")}>Sources</NavItem>
            }
            <DropdownButton title={this._systemTitle()} active={this._isActive("/system")}>
              <MenuItem href={jsRoutes.controllers.SystemController.index(0).url}>Overview</MenuItem>
              <MenuItem href={jsRoutes.controllers.NodesController.nodes().url}>Nodes</MenuItem>
              { this._isPermitted(['INPUTS_READ']) && <MenuItem href={jsRoutes.controllers.InputsController.index().url}>Inputs</MenuItem> }
              { this._isPermitted(['OUTPUTS_READ']) && <MenuItem href={jsRoutes.controllers.OutputsController.index().url}>Outputs</MenuItem> }
              { this._isPermitted(['COLLECTORS_READ']) && <MenuItem href={jsRoutes.controllers.CollectorsController.index().url}>Collectors</MenuItem> }
              { this._isPermitted(['INDICES_READ']) && <MenuItem href={jsRoutes.controllers.IndicesController.index().url}>Indices</MenuItem> }
              { this._isPermitted(['LOGGERS_READ']) && <MenuItem href={jsRoutes.controllers.LoggingController.index().url}>Logging</MenuItem> }
              { this._isPermitted(['USERS_EDIT']) && <MenuItem href={jsRoutes.controllers.UsersController.index().url}>Users</MenuItem> }
              { this._isPermitted(['ROLES_EDIT']) && <MenuItem href={jsRoutes.controllers.UsersController.rolesPage().url}>Roles</MenuItem> }
              { this._isPermitted(['DASHBOARDS_CREATE', 'INPUTS_CREATE', 'STREAMS_CREATE']) && <MenuItem href={jsRoutes.controllers.BundlesController.index().url}>Content Packs</MenuItem> }
              { this._isPermitted(['INPUTS_EDIT']) && <MenuItem href={jsRoutes.controllers.GrokPatternsController.index().url}>Grok Patterns</MenuItem> }
            </DropdownButton>
          </Nav>

          <Nav navbar>
            <NavItem href={jsRoutes.controllers.SystemController.index(0).url} className="notification-badge-link">
              <span className="badge" style={{backgroundColor: '#ff3b00'}} id="notification-badge"></span>
            </NavItem>
          </Nav>

          <Nav navbar right>
            <NavItem href={jsRoutes.controllers.NodesController.nodes().url}>
              <GlobalThroughput />
            </NavItem>
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
