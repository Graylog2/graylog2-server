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

const assertUnreachable = (type: string): never => {
  throw new Error(`Can't find route for type: ${type ?? '(undefined)'}`);
};

const getShowRouteForEntity = (id: string, type: string) => {
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
    default:
      return assertUnreachable(type);
  }
};

export default getShowRouteForEntity;
