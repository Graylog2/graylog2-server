import PropTypes from 'prop-types';
import React from 'react';
import { withRouter } from 'react-router';
import { LinkContainer } from 'react-router-bootstrap';
import naturalSort from 'javascript-natural-sort';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { Badge, Navbar, Nav, NavItem, NavDropdown } from 'components/graylog';
import connect from 'stores/connect';
import StoreProvider from 'injection/StoreProvider';
import { isPermitted } from 'util/PermissionsMixin';
import Routes from 'routing/Routes';
import URLUtils from 'util/URLUtils';
import AppConfig from 'util/AppConfig';
import GlobalThroughput from 'components/throughput/GlobalThroughput';
import { IfPermitted } from 'components/common';

import UserMenu from './UserMenu';
import HelpMenu from './HelpMenu';
import NavigationBrand from './NavigationBrand';
import NotificationBadge from './NotificationBadge';
import NavigationLink from './NavigationLink';
import SystemMenu from './SystemMenu';
import InactiveNavItem from './InactiveNavItem';
import ScratchpadToggle from './ScratchpadToggle';
import StyledNavbar from './Navigation.styles';

const CurrentUserStore = StoreProvider.getStore('CurrentUser');

const _isActive = (requestPath, prefix) => {
  // eslint-disable-next-line import/no-named-as-default-member
  return requestPath.indexOf(URLUtils.appPrefixed(prefix)) === 0;
};

const formatSinglePluginRoute = ({ description, path, permissions }, topLevel = false) => {
  // eslint-disable-next-line import/no-named-as-default-member
  const link = <NavigationLink key={description} description={description} path={URLUtils.appPrefixed(path)} topLevel={topLevel} />;

  if (permissions) {
    return <IfPermitted key={description} permissions={permissions}>{link}</IfPermitted>;
  }

  return link;
};

const formatPluginRoute = (pluginRoute, permissions, location) => {
  if (pluginRoute.children) {
    const activeChild = pluginRoute.children.filter(({ path }) => (path && _isActive(location.pathname, path)));
    const title = activeChild.length > 0 ? `${pluginRoute.description} / ${activeChild[0].description}` : pluginRoute.description;
    const isEmpty = !pluginRoute.children.some((child) => isPermitted(permissions, child.permissions));

    if (isEmpty) {
      return null;
    }

    return (
      <NavDropdown key={title} title={title} id="enterprise-dropdown">
        {pluginRoute.children.map((child) => formatSinglePluginRoute(child, false))}
      </NavDropdown>
    );
  }

  return formatSinglePluginRoute(pluginRoute, true);
};

const Navigation = ({ permissions, fullName, location, loginName }) => {
  const pluginExports = PluginStore.exports('navigation');

  const enterpriseMenuIsMissing = !pluginExports.find((value) => value.description.toLowerCase() === 'enterprise');
  const isPermittedToEnterprise = isPermitted(permissions, ['licenseinfos:read']);

  if (enterpriseMenuIsMissing && isPermittedToEnterprise) {
    // no enterprise plugin menu, so we will add one
    pluginExports.push({
      path: Routes.SYSTEM.ENTERPRISE,
      description: 'Enterprise',
    });
  }

  const pluginNavigations = pluginExports
    .sort((route1, route2) => naturalSort(route1.description.toLowerCase(), route2.description.toLowerCase()))
    .map((pluginRoute) => formatPluginRoute(pluginRoute, permissions, location));

  return (
    <StyledNavbar inverse fluid fixedTop>
      <Navbar.Header>
        <Navbar.Brand>
          <LinkContainer to={Routes.STARTPAGE}>
            <NavigationBrand />
          </LinkContainer>
        </Navbar.Brand>
        <Navbar.Toggle />

        {
        AppConfig.gl2DevMode()
          && <Badge bsStyle="danger" className="small-scrn-badge dev-badge">DEV</Badge>
        }
      </Navbar.Header>

      <Navbar.Collapse>
        <Nav navbar>
          <LinkContainer to={Routes.SEARCH}>
            <NavItem to="search">Search</NavItem>
          </LinkContainer>

          <LinkContainer to={Routes.STREAMS}>
            <NavItem>Streams</NavItem>
          </LinkContainer>

          <LinkContainer to={Routes.ALERTS.LIST}>
            <NavItem>Alerts</NavItem>
          </LinkContainer>

          <LinkContainer to={Routes.DASHBOARDS}>
            <NavItem>Dashboards</NavItem>
          </LinkContainer>

          {pluginNavigations}

          <SystemMenu />
        </Nav>

        <NotificationBadge />

        <Nav navbar pullRight className="header-meta-nav">
          {
          AppConfig.gl2DevMode()
            && (
              <InactiveNavItem className="dev-badge-wrap">
                <Badge bsStyle="danger" className="dev-badge">DEV</Badge>
              </InactiveNavItem>
            )
          }

          <LinkContainer to={Routes.SYSTEM.NODES.LIST}>
            <GlobalThroughput />
          </LinkContainer>
          <ScratchpadToggle />
          <HelpMenu active={_isActive(location.pathname, Routes.GETTING_STARTED)} />
          <UserMenu fullName={fullName} loginName={loginName} />
        </Nav>
      </Navbar.Collapse>
    </StyledNavbar>
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
