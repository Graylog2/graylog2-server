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
import { PluginStore } from 'graylog-web-plugin/plugin';
import { useLocation } from 'react-router-dom';

import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import { NavDropdown } from 'components/bootstrap';
import HideOnCloud from 'util/conditional/HideOnCloud';
import IfPermitted from 'components/common/IfPermitted';
import Routes from 'routing/Routes';
import { appPrefixed } from 'util/URLUtils';

import NavigationLink from './NavigationLink';

const TITLE_PREFIX = 'System';

const _isActive = (requestPath, prefix) => {
  return requestPath.indexOf(appPrefixed(prefix)) === 0;
};

const _systemTitle = (pathname) => {
  if (_isActive(pathname, '/system/overview')) {
    return `${TITLE_PREFIX} / Overview`;
  }

  if (_isActive(pathname, '/system/nodes')) {
    return `${TITLE_PREFIX} / Nodes`;
  }

  if (_isActive(pathname, '/system/inputs')) {
    return `${TITLE_PREFIX} / Inputs`;
  }

  if (_isActive(pathname, '/system/outputs')) {
    return `${TITLE_PREFIX} / Outputs`;
  }

  if (_isActive(pathname, '/system/indices')) {
    return `${TITLE_PREFIX} / Indices`;
  }

  if (_isActive(pathname, '/system/logging')) {
    return `${TITLE_PREFIX} / Logging`;
  }

  if (_isActive(pathname, '/system/authentication')) {
    return `${TITLE_PREFIX} / Authentication`;
  }

  if (_isActive(pathname, '/system/contentpacks')) {
    return `${TITLE_PREFIX} / Content Packs`;
  }

  if (_isActive(pathname, '/system/grokpatterns')) {
    return `${TITLE_PREFIX} / Grok Patterns`;
  }

  if (_isActive(pathname, '/system/lookuptables')) {
    return `${TITLE_PREFIX} / Lookup Tables`;
  }

  if (_isActive(pathname, '/system/configurations')) {
    return `${TITLE_PREFIX} / Configurations`;
  }

  if (_isActive(pathname, '/system/pipelines')) {
    return `${TITLE_PREFIX} / Pipelines`;
  }

  if (_isActive(pathname, '/system/sidecars')) {
    return `${TITLE_PREFIX} / Sidecars`;
  }

  const pluginRoute = PluginStore.exports('systemnavigation').filter((route) => _isActive(pathname, route.path))[0];

  if (pluginRoute) {
    return `${TITLE_PREFIX} / ${pluginRoute.description}`;
  }

  return TITLE_PREFIX;
};

const SystemMenu = () => {
  const location = useLocation();
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

  const dropdownTitle = _systemTitle(location.pathname);

  return (
    <NavDropdown title={dropdownTitle} id="system-menu-dropdown" inactiveTitle={TITLE_PREFIX}>
      <NavigationLink path={Routes.SYSTEM.OVERVIEW} description="Overview" />
      <IfPermitted permissions={['clusterconfigentry:read']}>
        <NavigationLink path={Routes.SYSTEM.CONFIGURATIONS} description="Configurations" />
      </IfPermitted>
      <HideOnCloud>
        <NavigationLink path={Routes.SYSTEM.NODES.LIST} description="Nodes" />
      </HideOnCloud>
      <HideOnCloud>
        <IfPermitted permissions={['inputs:read']}>
          <NavigationLink path={Routes.SYSTEM.INPUTS} description="Inputs" />
        </IfPermitted>
        <IfPermitted permissions={['outputs:read']}>
          <NavigationLink path={Routes.SYSTEM.OUTPUTS} description="Outputs" />
        </IfPermitted>
      </HideOnCloud>
      <IfPermitted permissions={['indices:read']}>
        <NavigationLink path={Routes.SYSTEM.INDICES.LIST} description="Indices" />
      </IfPermitted>
      <HideOnCloud>
        <IfPermitted permissions={['loggers:read']}>
          <NavigationLink path={Routes.SYSTEM.LOGGING} description="Logging" />
        </IfPermitted>
      </HideOnCloud>
      <IfPermitted permissions={['users:list']} anyPermissions>
        <NavigationLink path={Routes.SYSTEM.USERS.OVERVIEW} description="Users and Teams" />
      </IfPermitted>
      <IfPermitted permissions={['roles:read']} anyPermissions>
        <NavigationLink path={Routes.SYSTEM.AUTHZROLES.OVERVIEW} description="Roles" />
      </IfPermitted>
      <HideOnCloud>
        <IfPermitted permissions={['authentication:edit']} anyPermissions>
          <NavigationLink path={Routes.SYSTEM.AUTHENTICATION.BACKENDS.ACTIVE} description="Authentication" />
        </IfPermitted>
      </HideOnCloud>
      <IfPermitted permissions={['contentpack:read']}>
        <NavigationLink path={Routes.SYSTEM.CONTENTPACKS.LIST} description="Content Packs" />
      </IfPermitted>
      <IfPermitted permissions={['grok_pattern:read']}>
        <NavigationLink path={Routes.SYSTEM.GROKPATTERNS} description="Grok Patterns" />
      </IfPermitted>
      <IfPermitted permissions={['lookuptables:read']}>
        <NavigationLink path={Routes.SYSTEM.LOOKUPTABLES.OVERVIEW} description="Lookup Tables" />
      </IfPermitted>
      <IfPermitted permissions={['pipeline:read', 'pipeline_connection:read']}>
        <NavigationLink path={Routes.SYSTEM.PIPELINES.OVERVIEW} description="Pipelines" />
      </IfPermitted>
      <IfPermitted permissions={['sidecars:read']}>
        <NavigationLink path={Routes.SYSTEM.SIDECARS.OVERVIEW} description="Sidecars" />
      </IfPermitted>
      {pluginSystemNavigations}
    </NavDropdown>
  );
};

export default SystemMenu;
