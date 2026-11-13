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
import lowerCase from 'lodash/lowerCase';

import usePluginEntities from 'hooks/usePluginEntities';

const coreEntityTypes = new Set([
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
  'sigma_rule',
  'event_procedure',
  'event_procedure_step',
  'collection',
  'collection_entities',
]);

const useEntityTypeTitle = (type: string): string | undefined => {
  const pluginTitle = usePluginEntities('entityTypeTitles').find((entry) => entry.type === type)?.title;

  return coreEntityTypes.has(type) ? lowerCase(type) : pluginTitle;
};

export default useEntityTypeTitle;
