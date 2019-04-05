import PropTypes from 'prop-types';
import React from 'react';
import { withRouter } from 'react-router';

import { Badge, Navbar, Nav, NavItem, NavDropdown } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import naturalSort from 'javascript-natural-sort';

import connect from 'stores/connect';
import StoreProvider from 'injection/StoreProvider';
import PermissionsMixin from 'util/PermissionsMixin';

import Routes from 'routing/Routes';
import URLUtils from 'util/URLUtils';
import AppConfig from 'util/AppConfig';

import { PluginStore } from 'graylog-web-plugin/plugin';

import GlobalThroughput from 'components/throughput/GlobalThroughput';
import UserMenu from 'components/navigation/UserMenu';
import HelpMenu from 'components/navigation/HelpMenu';
import { IfPermitted } from 'components/common';
import badgeStyles from 'components/bootstrap/Badge.css';

import NavigationBrand from './NavigationBrand';
import InactiveNavItem from './InactiveNavItem';
import NotificationBadge from './NotificationBadge';
import NavigationLink from './NavigationLink';
import SystemMenu from './SystemMenu';

const CurrentUserStore = StoreProvider.getStore('CurrentUser');
const { isPermitted } = PermissionsMixin;

const _isActive = (requestPath, prefix) => {
  return requestPath.indexOf(URLUtils.appPrefixed(prefix)) === 0;
};

const formatSinglePluginRoute = ({ description, path, permissions }) => {
  const link = <NavigationLink key={description} description={description} path={path} />;
  if (permissions) {
    return <IfPermitted key={description} permissions={permissions}>{link}</IfPermitted>;
  }
  return link;
};

const formatPluginRoute = (pluginRoute, permissions, location) => {
  if (pluginRoute.children) {
    const activeChild = pluginRoute.children.filter(({ path }) => (path && _isActive(location.pathname, path)));
    const title = activeChild.length > 0 ? `${pluginRoute.description} / ${activeChild[0].description}` : pluginRoute.description;
    const isEmpty = !pluginRoute.children.some(child => isPermitted(permissions, child.permissions));
    if (isEmpty) {
      return null;
    }
    return (
      <NavDropdown key={title} title={title} id="enterprise-dropdown">
        {pluginRoute.children.map(formatSinglePluginRoute)}
      </NavDropdown>
    );
  }
  return formatSinglePluginRoute(pluginRoute);
};

const Navigation = ({ permissions, fullName, location, loginName }) => {
  const pluginNavigations = PluginStore.exports('navigation')
    .sort((route1, route2) => naturalSort(route1.description.toLowerCase(), route2.description.toLowerCase()))
    .map(pluginRoute => formatPluginRoute(pluginRoute, permissions, location));

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
            <NavItem>Dashboards</NavItem>
          </LinkContainer>

          <IfPermitted permissions="sources:read">
            <LinkContainer to={Routes.SOURCES}>
              <NavItem>Sources</NavItem>
            </LinkContainer>
          </IfPermitted>

          {pluginNavigations}

          <SystemMenu />
        </Nav>

        <NotificationBadge />

        <Nav navbar pullRight>
          <LinkContainer to={Routes.SYSTEM.NODES.LIST}>
            <InactiveNavItem><GlobalThroughput /></InactiveNavItem>
          </LinkContainer>
          <HelpMenu active={_isActive(location.pathname, Routes.GETTING_STARTED)} />
          <UserMenu fullName={fullName} loginName={loginName} />
          {AppConfig.gl2DevMode()
            ? (
              <NavItem className="notification-badge-link">
                <Badge className={badgeStyles.badgeDanger}>DEV</Badge>
              </NavItem>
            )
            : null}
        </Nav>
      </Navbar.Collapse>
    </Navbar>
  );
};

Navigation.propTypes = {
  location: PropTypes.shape({
    pathname: PropTypes.string.isRequired,
  }).isRequired,
  loginName: PropTypes.string.isRequired,
  fullName: PropTypes.string.isRequired,
  permissions: PropTypes.arrayOf(PropTypes.string),
};

Navigation.defaultProps = {
  permissions: undefined,
};

export default connect(
  withRouter(Navigation),
  { currentUser: CurrentUserStore },
  ({ currentUser }) => ({ permissions: currentUser ? currentUser.currentUser.permissions : undefined }),
);
