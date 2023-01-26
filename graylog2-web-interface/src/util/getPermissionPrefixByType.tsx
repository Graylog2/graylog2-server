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

const assertUnreachable = (type: string): never => {
  throw new Error(`Can't find prefix for type: ${type ?? '(undefined)'}`);
};

const supportedTypes = new Set([
  'user',
  'team',
  'dashboard',
  'event_definition',
  'notification',
  'search',
  'stream',
  'search_filter',
  'report',
  'role',
  'output',
]);

const typePrefixCornerCasesMap = {
  event_definition: 'eventdefinitions',
  notification: 'eventnotifications',
  search: 'view',
  report: 'report',
};

const getPermissionPrefixByType = (type: string, throwErrorOnUnknown = true) => {
  if (supportedTypes.has(type)) return typePrefixCornerCasesMap[type] ?? `${type}s`;

  return throwErrorOnUnknown ? assertUnreachable(type) : `(unsupported type ${type})`;
};

export default getPermissionPrefixByType;
