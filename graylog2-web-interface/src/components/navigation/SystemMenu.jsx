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
import React from 'react';
import PropTypes from 'prop-types';
import { PluginStore } from 'graylog-web-plugin/plugin';
import naturalSort from 'javascript-natural-sort';

import withLocation from 'routing/withLocation';
import { NavDropdown } from 'components/graylog';
import IfPermitted from 'components/common/IfPermitted';
import Routes from 'routing/Routes';
import { appPrefixed } from 'util/URLUtils';
import HideOnCloud from 'util/conditional/HideOnCloud';

import NavigationLink from './NavigationLink';

const _isActive = (requestPath, prefix) => {
  return requestPath.indexOf(appPrefixed(prefix)) === 0;
};

const _systemTitle = (pathname) => {
  const prefix = 'System';

  if (_isActive(pathname, '/system/overview')) {
    return `${prefix} / Overview`;
  }

  if (_isActive(pathname, '/system/nodes')) {
    return `${prefix} / Nodes`;
  }

  if (_isActive(pathname, '/system/inputs')) {
    return `${prefix} / Inputs`;
  }

  if (_isActive(pathname, '/system/outputs')) {
    return `${prefix} / Outputs`;
  }

  if (_isActive(pathname, '/system/indices')) {
    return `${prefix} / Indices`;
  }

  if (_isActive(pathname, '/system/logging')) {
    return `${prefix} / Logging`;
  }

  if (_isActive(pathname, '/system/authentication')) {
    return `${prefix} / Authentication`;
  }

  if (_isActive(pathname, '/system/contentpacks')) {
    return `${prefix} / Content Packs`;
  }

  if (_isActive(pathname, '/system/grokpatterns')) {
    return `${prefix} / Grok Patterns`;
  }

  if (_isActive(pathname, '/system/lookuptables')) {
    return `${prefix} / Lookup Tables`;
  }

  if (_isActive(pathname, '/system/configurations')) {
    return `${prefix} / Configurations`;
  }

  if (_isActive(pathname, '/system/pipelines')) {
    return `${prefix} / Pipelines`;
  }

  if (_isActive(pathname, '/system/sidecars')) {
    return `${prefix} / Sidecars`;
  }

  const pluginRoute = PluginStore.exports('systemnavigation').filter((route) => _isActive(pathname, route.path))[0];

  if (pluginRoute) {
    return `${prefix} / ${pluginRoute.description}`;
  }

  return prefix;
};

const SystemMenu = ({ location }) => {
  const pluginSystemNavigations = PluginStore.exports('systemnavigation')
    .sort((route1, route2) => naturalSort(route1.description.toLowerCase(), route2.description.toLowerCase()))
    .map(({ description, path, permissions }) => {
      const prefixedPath = appPrefixed(path);
      const link = <NavigationLink description={description} path={prefixedPath} />;

      if (permissions) {
        return <IfPermitted key={description} permissions={permissions}>{link}</IfPermitted>;
      }

      return <NavigationLink key={description} path={prefixedPath} description={description} />;
    });

  return (
    <NavDropdown title={_systemTitle(location.pathname)} id="system-menu-dropdown">
      <NavigationLink path={Routes.SYSTEM.OVERVIEW} description="Overview" />
      <IfPermitted permissions={['clusterconfigentry:read']}>
        <NavigationLink path={Routes.SYSTEM.CONFIGURATIONS} description="Configurations" />
      </IfPermitted>
      <NavigationLink path={Routes.SYSTEM.NODES.LIST} description="Nodes" />
      <IfPermitted permissions={['inputs:read']}>
        <NavigationLink path={Routes.SYSTEM.INPUTS} description="Inputs" />
      </IfPermitted>
      <HideOnCloud>
        <IfPermitted permissions={['outputs:read']}>
          <NavigationLink path={Routes.SYSTEM.OUTPUTS} description="Outputs" />
        </IfPermitted>
        <IfPermitted permissions={['indices:read']}>
          <NavigationLink path={Routes.SYSTEM.INDICES.LIST} description="Indices" />
        </IfPermitted>
      </HideOnCloud>
      <IfPermitted permissions={['loggers:read']}>
        <NavigationLink path={Routes.SYSTEM.LOGGING} description="Logging" />
      </IfPermitted>
      <IfPermitted permissions={['users:list']} anyPermissions>
        <NavigationLink path={Routes.SYSTEM.USERS.OVERVIEW} description="Users and Teams" />
      </IfPermitted>
      <IfPermitted permissions={['roles:read']} anyPermissions>
        <NavigationLink path={Routes.SYSTEM.AUTHZROLES.OVERVIEW} description="Roles" />
      </IfPermitted>
      <IfPermitted permissions={['authentication:edit']} anyPermissions>
        <NavigationLink path={Routes.SYSTEM.AUTHENTICATION.BACKENDS.ACTIVE} description="Authentication" />
      </IfPermitted>
      <IfPermitted permissions={['dashboards:create', 'inputs:create', 'streams:create']}>
        <NavigationLink path={Routes.SYSTEM.CONTENTPACKS.LIST} description="Content Packs" />
      </IfPermitted>
      <IfPermitted permissions={['inputs:read']}>
        <NavigationLink path={Routes.SYSTEM.GROKPATTERNS} description="Grok Patterns" />
      </IfPermitted>
      <IfPermitted permissions={['inputs:edit']}>
        <NavigationLink path={Routes.SYSTEM.LOOKUPTABLES.OVERVIEW} description="Lookup Tables" />
      </IfPermitted>
      <IfPermitted permissions={['inputs:create']}>
        <NavigationLink path={Routes.SYSTEM.PIPELINES.OVERVIEW} description="Pipelines" />
      </IfPermitted>
      <IfPermitted permissions={['inputs:edit']}>
        <NavigationLink path={Routes.SYSTEM.SIDECARS.OVERVIEW} description="Sidecars" />
      </IfPermitted>
      {pluginSystemNavigations}
    </NavDropdown>
  );
};

SystemMenu.propTypes = {
  location: PropTypes.shape({
    pathname: PropTypes.string.isRequired,
  }).isRequired,
};

export default withLocation(SystemMenu);
