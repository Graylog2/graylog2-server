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

import type { SearchBarControl } from 'views/types';

interface EventDefinitionType {
  type: string,
  displayName: string,
  sortOrder: number,
  description: string,
  defaultConfig: EventDefinition['config'],
  formComponent: React.ComponentType<React.ComponentProps<{
    eventDefinition: EventDefinition,
    currentUser: UserJSON,
    validation: { errors: { [key: string]: Array<string> } },
    onChange: (name: string, newConfig: EventDefinition['config']) => void,
    action: string,
  }>>
}
declare module 'graylog-web-plugin/plugin' {
  interface PluginExports {
    'eventDefinitionTypes'?: Array<EventDefinitionType>;
    'eventDefinitions.components.searchForm'?: Array<() => SearchBarControl | null>
  }
}
