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

const getFirstMatchingEntityRouteFromPlugin = (
  entityRouteResolver: Array<(id: string, type: string) => string | null>,
  id: string,
  type: string,
) => {
  if (!entityRouteResolver?.length) {
    return undefined;
  }

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

  return getFirstMatchingEntityRouteFromPlugin(pluginEntityRoutesResolver, id, type);
};

const getBaseEntityRoute = (id: string, type: string) => {
  switch (type?.toLowerCase()) {
    case 'user':
      return Routes.SYSTEM.USERS.show(id);
    case 'team':
      return Routes.getPluginRoute('SYSTEM_TEAMS_TEAMID')(id);
    case 'team_users':
      return Routes.getPluginRoute('SYSTEM_TEAMS_TEAMID')(id);
    case 'dashboard':
      return Routes.dashboard_show(id);
    case 'event_definition':
      return Routes.ALERTS.DEFINITIONS.show(id);
    case 'event_procedure':
      return Routes.getPluginRoute('ALERTS_EVENT-PROCEDURES_PROCEDURES');
    case 'event_procedure_step':
      return Routes.getPluginRoute('ALERTS_EVENT-PROCEDURES_STEPS');
    case 'notification':
      return Routes.ALERTS.NOTIFICATIONS.show(id);
    case 'search':
      return Routes.getPluginRoute('SEARCH_VIEWID')(id);
    case 'stream':
      return Routes.stream_search(id);
    case 'search_filter':
      return Routes.getPluginRoute('MY-FILTERS_DETAILS_FILTERID')?.(id);
    case 'sigma_rule':
      return Routes.getPluginRoute('SECURITY_SIGMA');
    case 'report':
      return Routes.getPluginRoute('REPORTS_REPORTID_ARCHIVE')?.(id);
    case 'role':
      return Routes.SYSTEM.AUTHZROLES.show(id);
    case 'output':
      return Routes.SYSTEM.OUTPUTS;
    case 'collection':
      return Routes.getPluginRoute('COLLECTIONS_COLLECTIONID')?.(id);
    case 'collection_entities':
      return Routes.getPluginRoute('COLLECTIONS_COLLECTIONID')?.(id);
    case 'index_set':
      return Routes.SYSTEM.INDEX_SETS.SHOW(id);
    case 'content_pack':
      return Routes.SYSTEM.CONTENTPACKS.show(id);
    case 'lookup_table':
      return Routes.SYSTEM.LOOKUPTABLES.show(id);
    case 'lookup_table_cache':
      return Routes.SYSTEM.LOOKUPTABLES.CACHES.show(id);
    case 'lookup_table_data_adapter':
      return Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.show(id);
    case 'pipeline_rule':
      return Routes.SYSTEM.PIPELINES.RULE(id);
    case 'pipeline':
      return Routes.SYSTEM.PIPELINES.PIPELINE(id);
    case 'input':
      return Routes.SYSTEM.INPUT_DIAGNOSIS(id);
    case 'event_notification':
      return Routes.ALERTS.NOTIFICATIONS.show(id);
    default:
      return assertUnreachable((type as never) ?? '(undefined)', "Can't find route for type");
  }
}

export const getEntityRoute = (id: string, type: string, entityRouteResolver: Array<(id: string, type: string) => string | null>) => {
  const entityRouteFromPlugin = getFirstMatchingEntityRouteFromPlugin(entityRouteResolver, id, type)

  if (entityRouteFromPlugin) {
    return entityRouteFromPlugin;
  }

  return getBaseEntityRoute(id, type);
}

const useShowRouteForEntity = (id: string, type: string) => {
  const entityRouteFromPlugin = useEntityRouteFromPlugin(id, type);

  if (entityRouteFromPlugin) {
    return entityRouteFromPlugin;
  }

  return getBaseEntityRoute(id, type);
};

export default useShowRouteForEntity;
