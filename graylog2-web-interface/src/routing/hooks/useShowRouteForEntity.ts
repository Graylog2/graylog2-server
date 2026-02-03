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
import { useMemo } from 'react';
import type { PluginExports } from 'graylog-web-plugin/plugin';

import type { QualifiedUrl } from 'routing/Routes';
import Routes from 'routing/Routes';
import assertUnreachable from 'logic/assertUnreachable';
import usePluginEntities from 'hooks/usePluginEntities';

const getFirstMatchingEntityRouteFromPlugin = (
  entityRouteResolver: PluginExports['entityRoutes'],
  id: string,
  type: string,
) => {
  if (!entityRouteResolver?.length) {
    return undefined;
  }

  for (const resolver of entityRouteResolver) {
    const entityRoute = resolver(id, type);

    if (entityRoute) {
      return entityRoute;
    }
  }

  return undefined;
};

const getBaseEntityRoute = (id: string, type: string): QualifiedUrl<string> | undefined => {
  switch (type?.toLowerCase()) {
    case 'user':
      return Routes.SYSTEM.USERS.show(id);
    case 'dashboard':
      return Routes.dashboard_show(id);
    case 'event_definition':
      return Routes.ALERTS.DEFINITIONS.show(id);
    case 'notification':
      return Routes.ALERTS.NOTIFICATIONS.show(id);
    case 'search':
      return Routes.show_saved_search(id);
    case 'stream':
      return Routes.stream_search(id);
    case 'role':
      return Routes.SYSTEM.AUTHZROLES.show(id);
    case 'output':
      return Routes.SYSTEM.OUTPUTS;
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
    case 'node':
      return Routes.SYSTEM.CLUSTER.NODE_SHOW(id);
  }

  return undefined;
};

export const getEntityRoute = (
  id: string,
  type: string,
  entityRouteResolver: PluginExports['entityRoutes'],
  entityTypeGenerators: { [type: string]: PluginExports['entityTypeRoute'][number]['route'] },
): QualifiedUrl<string> => {
  const entityRouteFromPlugin = getFirstMatchingEntityRouteFromPlugin(entityRouteResolver, id, type);

  if (entityRouteFromPlugin) {
    return entityRouteFromPlugin;
  }

  const baseRoute = getBaseEntityRoute(id, type);

  if (baseRoute) {
    return baseRoute;
  }

  const pluginRoute = entityTypeGenerators[type];
  if (pluginRoute) {
    return pluginRoute(id);
  }

  return assertUnreachable((type as never) ?? '(undefined)', "Can't find route for type");
};

export const usePluginEntityTypeGenerators = () => {
  const pluginEntityTypeGenerators = usePluginEntities('entityTypeRoute');

  return useMemo(
    () => Object.fromEntries(pluginEntityTypeGenerators.map((e) => [e.type, e.route])),
    [pluginEntityTypeGenerators],
  );
};

const useShowRouteForEntity = (id: string, type: string) => {
  const pluginEntityRoutesResolver = usePluginEntities('entityRoutes');
  const entityTypeGenerators = usePluginEntityTypeGenerators();

  return getEntityRoute(id, type, pluginEntityRoutesResolver, entityTypeGenerators);
};

export default useShowRouteForEntity;
