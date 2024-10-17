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

import type React from 'react';

import type { SearchBarControl } from 'views/types';
import type User from 'logic/users/User';
import type { EventDefinition } from 'components/event-definitions/event-definitions-types';

export interface EventDefinitionValidation {
  errors: {
    config?: unknown,
    title?: string,
    event_limit?: string,
    query_parameters?: string[],
    search_within_ms?: string[],
    cron_expression?: string,
    execute_every_ms?: string[],
  }
}
export interface EventDefinitionType {
  type: string,
  displayName: string,
  sortOrder: number,
  description: string,
  defaultConfig: EventDefinition['config'],
  formComponent: React.ComponentType<{
    eventDefinition: EventDefinition,
    entityTypes: {
      aggregation_functions: Array<{}>
    },
    currentUser: User,
    validation: EventDefinitionValidation,
    onChange: (name: string, newConfig: EventDefinition['config']) => void,
    action: string,
  }>,
  summaryComponent: React.ComponentType<{
    currentUser: User,
    config: EventDefinition['config'],
    definitionId?: string,
  }>
}
declare module 'graylog-web-plugin/plugin' {
  interface PluginExports {
    'eventDefinitionTypes'?: Array<EventDefinitionType>;
    'eventDefinitions.components.searchForm'?: Array<() => SearchBarControl | null>
    'eventDefinitions.components.editSigmaModal'?: Array<{ component: React.FC, key: string }>
  }
}
