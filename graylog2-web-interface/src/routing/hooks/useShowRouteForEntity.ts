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
import Routes from 'routing/Routes';
import assertUnreachable from 'logic/assertUnreachable';
import usePluginEntities from 'hooks/usePluginEntities';

const getFirstMatchingEntityRoute = (
  entityRouteResolver: Array<(id: string, type: string) => string | null>,
  id: string,
  type: string,
) => {
  for (let i = 0; i < entityRouteResolver.length; i += 1) {
    const entityRoute = entityRouteResolver[i](id, type);

    if (entityRoute) {
      return entityRoute;
    }
  }

  return undefined;
};

const useEntityRouteFromPlugin = (id: string, type: string) => {
  const pluginEntityRoutesResolver = usePluginEntities('entityRoutes');

  if (!pluginEntityRoutesResolver?.length) {
    return null;
  }

  return getFirstMatchingEntityRoute(pluginEntityRoutesResolver, id, type);
};

const useShowRouteForEntity = (id: string, type: string) => {
  const entityRouteFromPlugin = useEntityRouteFromPlugin(id, type);

  if (entityRouteFromPlugin) {
    return entityRouteFromPlugin;
  }

  switch (type?.toLowerCase()) {
    case 'user':
      return Routes.SYSTEM.USERS.show(id);
    case 'team':
      return Routes.getPluginRoute('SYSTEM_TEAMS_TEAMID')(id);
    case 'dashboard':
      return Routes.dashboard_show(id);
    case 'event_definition':
      return Routes.ALERTS.DEFINITIONS.show(id);
    case 'notification':
      return Routes.ALERTS.NOTIFICATIONS.show(id);
    case 'search':
      return Routes.getPluginRoute('SEARCH_VIEWID')(id);
    case 'stream':
      return Routes.stream_search(id);
    case 'search_filter':
      return Routes.getPluginRoute('MY-FILTERS_DETAILS_FILTERID')(id);
    case 'report':
      return Routes.getPluginRoute('REPORTS_REPORTID_CONFIGURATION')?.(id);
    case 'role':
      return Routes.SYSTEM.AUTHZROLES.show(id);
    case 'output':
      return Routes.SYSTEM.OUTPUTS;
    default:
      return assertUnreachable(type as never ?? '(undefined)', 'Can\'t find route for type');
  }
};

export default useShowRouteForEntity;
