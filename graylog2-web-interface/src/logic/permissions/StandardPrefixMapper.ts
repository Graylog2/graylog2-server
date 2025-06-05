import type { PluginExports } from 'graylog-web-plugin/plugin';

import type { PrefixMapper } from 'logic/permissions/PrefixMapperBinder';
import PrefixMapperBinder from 'logic/permissions/PrefixMapperBinder';

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

const mapper: PrefixMapper = {
  mapForIdAndType(_id: string, _type: string): string | undefined {
    return undefined;
  },
  mapForType(type: string): string | undefined {
    if (supportedTypes.has(type)) return typePrefixCornerCasesMap[type] ?? `${type}s:`;

    return undefined;
  },
};

PrefixMapperBinder.register(mapper);

const standardPrefixMapper: PluginExports = {
  prefixMapper: mapper,
};

export default standardPrefixMapper;
