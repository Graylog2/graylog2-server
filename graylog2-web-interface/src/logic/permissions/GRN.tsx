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
// @flow strict
// eslint-disable-next-line import/prefer-default-export
import Routes from 'routing/Routes';

const _convertEmptyString = (value: string) => (value === '' ? undefined : value);

export const createGRN = (type: string, id: string) => `grn::::${type}:${id}`;

export const getValuesFromGRN = (grn: string) => {
  const grnValues = grn.split(':');
  const [resourceNameType, cluster, tenent, scope, type, id] = grnValues.map(_convertEmptyString);

  return { resourceNameType, cluster, tenent, scope, type, id };
};

export const getShowRouteFromGRN = (grn: string) => {
  const { id, type } = getValuesFromGRN(grn);

  switch (type) {
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
    default:
      throw new Error(`Can't find route for grn ${grn} of type: ${type ?? '(undefined)'}`);
  }
};
