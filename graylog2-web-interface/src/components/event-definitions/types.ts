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
import type { Attribute } from 'stores/PaginationTypes';

export type AlertType = 'alert' | 'event' | 'event_definition';

export interface EventDefinitionValidation {
  errors: {
    config?: unknown;
    title?: string;
    event_limit?: string;
    query_parameters?: string[];
    search_within_ms?: string[];
    cron_expression?: string;
    execute_every_ms?: string[];
  };
}
export interface EventDefinitionType {
  type: string;
  displayName: string;
  sortOrder: number;
  description: string;
  defaultConfig: EventDefinition['config'];
  formComponent: React.ComponentType<{
    eventDefinition: EventDefinition;
    entityTypes: {
      aggregation_functions: Array<{}>;
    };
    currentUser: User;
    validation: EventDefinitionValidation;
    onChange: (name: string, newConfig: EventDefinition['config']) => void;
    action: string;
  }>;
  summaryComponent: React.ComponentType<{
    currentUser: User;
    config: EventDefinition['config'];
    definitionId?: string;
    entityScope?: string;
  }>;
  useCondition: () => boolean;
  hideFieldsStep?: boolean;
  // Hides the type from the create wizard's dropdown while still listing it as a filter option.
  hideFromCreation?: boolean;
}
export type TacticsTechniquesEditorPlugin = {
  component: React.ComponentType<{
    value: ReadonlyArray<string>;
    onChange: (next: string[]) => void;
    disabled?: boolean;
    error?: React.ReactNode;
  }>;
};

export type TacticsTechniquesColumnPlugin = {
  attribute: Attribute;
  component: React.ComponentType<{ entity: EventDefinition }>;
  // Hook evaluated inside EventDefinitionsContainer to decide whether to render the column.
  // Lets the plugin gate on license/feature state without OSS knowing about it.
  useCondition?: () => boolean;
};

export type TacticsTechniquesDetailRowPlugin = {
  // Renders a Tactics/Techniques row inside the event details expanded section.
  // Plugin owns the dual-read between the new tactics_techniques field and legacy
  // sigma_rule_tag_* shape so callers don't have to know about either.
  component: React.ComponentType<{ entity: { tactics_techniques?: string[]; fields?: Record<string, unknown> } }>;
  useCondition?: () => boolean;
};

export type TacticsTechniquesSummaryPlugin = {
  // Renders a Tactics/Techniques row on the event definition summary view.
  component: React.ComponentType<{ eventDefinition: EventDefinition }>;
  useCondition?: () => boolean;
};

declare module 'graylog-web-plugin/plugin' {
  interface PluginExports {
    'eventDefinitionTypes'?: Array<EventDefinitionType>;
    'eventDefinitions.components.searchForm'?: Array<() => SearchBarControl | null>;
    'eventDefinitions.components.editSigmaModal'?: Array<{ component: React.FC; key: string }>;
    'eventDefinitions.components.overviewPageSections'?: Array<{
      key: string;
      component: React.ComponentType;
      order?: number;
    }>;
    'eventDefinitions.components.detailPageSections'?: Array<{
      key: string;
      component: React.ComponentType<{ eventDefinition: EventDefinition }>;
      order?: number;
    }>;
    'eventDefinitions.components.tacticsTechniquesEditor'?: Array<TacticsTechniquesEditorPlugin>;
    'eventDefinitions.components.tacticsTechniquesColumn'?: Array<TacticsTechniquesColumnPlugin>;
    'eventDefinitions.components.tacticsTechniquesSummary'?: Array<TacticsTechniquesSummaryPlugin>;
    'events.components.tacticsTechniquesDetailRow'?: Array<TacticsTechniquesDetailRowPlugin>;
    'eventDefinitions.components.sigmaGitImport'?: Array<{ component: React.FC; key: string }>;
    'eventDefinitions.components.sigmaFileUpload'?: Array<{ component: React.FC; key: string }>;
    'eventDefinitions.components.sigmaOptions'?: Array<{ component: React.FC; key: string }>;
  }
}
