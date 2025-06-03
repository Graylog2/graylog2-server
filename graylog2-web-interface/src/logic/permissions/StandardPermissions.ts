import type { PluginExports } from 'graylog-web-plugin/plugin';

import type { PermissionChecker } from 'logic/permissions/PermissionsBinder';
import PermissionsBinder from 'logic/permissions/PermissionsBinder';

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

const permissions: PermissionChecker = {
  checkWithID(_type: string, _id: string): string | undefined {
    return undefined;
  },
  check(type: string): string | undefined {
    if (supportedTypes.has(type)) return typePrefixCornerCasesMap[type] ?? `${type}s:`;

    return undefined;
  },
};

PermissionsBinder.register(permissions);

const standardPermissions: PluginExports = {
  entityPermissionChecker: permissions,
};

export default standardPermissions;
