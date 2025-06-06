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
import type { EntityPermissionsMapper } from 'logic/permissions/EntityPermissionsMapper';

const supportedTypes = new Set([
  'user',
  'team',
  'dashboard',
  'event_definition',
  'notification',
  'search',
  'stream',
  'search_filter',
  'role',
  'output',
]);

const typePrefixCornerCasesMap = {
  event_definition: 'eventdefinitions:',
  notification: 'eventnotifications:',
  search: 'view:',
};

const standardEntityPermissionsMapper: EntityPermissionsMapper = {
  mapForIdAndType(_id: string, _type: string): string | undefined {
    return undefined;
  },
  mapForType(type: string): string | undefined {
    if (supportedTypes.has(type)) return typePrefixCornerCasesMap[type] ?? `${type}s:`;

    return undefined;
  },
};

export default standardEntityPermissionsMapper;
