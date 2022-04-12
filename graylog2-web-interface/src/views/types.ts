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
import type * as Immutable from 'immutable';
import type { FormikErrors } from 'formik';

import type Widget from 'views/logic/widgets/Widget';
import type { ActionDefinition } from 'views/components/actions/ActionHandler';
import type { SearchRefreshCondition } from 'views/logic/hooks/SearchRefreshCondition';
import type { VisualizationComponent } from 'views/components/aggregationbuilder/AggregationBuilder';
import type { WidgetActionType } from 'views/components/widgets/Types';
import type { Creator } from 'views/components/sidebar/create/AddWidgetButton';
import type { ViewHook } from 'views/logic/hooks/ViewHook';
import type WidgetConfig from 'views/logic/widgets/WidgetConfig';
import type FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import type { Completer } from 'views/components/searchbar/SearchBarAutocompletions';
import type { Result } from 'views/components/widgets/Widget';
import type { Widgets } from 'views/stores/WidgetStore';
import type { OverrideProps } from 'views/components/WidgetOverrideElements';
import type {
  VisualizationConfigDefinition,
  VisualizationConfigFormValues,
  VisualizationFormValues,
  WidgetConfigFormValues,
} from 'views/components/aggregationwizard';
import type VisualizationConfig from 'views/logic/aggregationbuilder/visualizations/VisualizationConfig';
import type { TimeRange } from 'views/logic/queries/Query';
import type View from 'views/logic/views/View';
import type User from 'logic/users/User';
import type { Message } from 'views/components/messagelist/Types';
import type { ValuePath } from 'views/logic/valueactions/ValueActionHandler';
import type WidgetPosition from 'views/logic/widgets/WidgetPosition';
import type MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import type { QueryValidationState } from 'views/components/searchbar/queryvalidation/types';

export type BackendWidgetPosition = {
  id: string,
  col: number,
  row: number,
  height: number,
  width: number,
};

export type WidgetPositions = { [widgetId: string]: WidgetPosition };

export interface EditWidgetComponentProps<Config extends WidgetConfig = WidgetConfig> {
  children: React.ReactNode,
  config: Config,
  editing: boolean;
  id: string;
  type: string;
  fields: Immutable.List<FieldTypeMapping>,
  onChange: (newConfig: Config) => void,
}

export interface WidgetResults {
 [key: string]: Result,
}

export interface WidgetComponentProps<Config extends WidgetConfig = WidgetConfig, Results = WidgetResults> {
  config: Config;
  data: Results;
  editing: boolean;
  fields: Immutable.List<FieldTypeMapping>;
  filter: string;
  queryId: string;
  onConfigChange: (newConfig: Config) => Promise<Widgets>;
  setLoadingState: (loading: boolean) => void;
  title: string;
  toggleEdit: () => void;
  type: string;
  id: string;
}

export interface WidgetExport {
  type: string;
  displayName?: string;
  defaultHeight?: number;
  defaultWidth?: number;
  visualizationComponent: React.ComponentType<WidgetComponentProps>;
  editComponent: React.ComponentType<EditWidgetComponentProps>;
  needsControlledHeight: (widget: { config: Widget['config'] }) => boolean;
  searchResultTransformer?: (data: Array<unknown>) => unknown;
  searchTypes: (widget: Widget) => Array<any>;
  titleGenerator?: (widget: Widget) => string;
  reportStyle?: () => { width: React.CSSProperties['width'] };
  exportComponent?: React.ComponentType<{ widget: Widget }>;
}

export interface VisualizationConfigProps {
  config: WidgetConfig;
  onChange: (newConfig: WidgetConfig) => void;
}

export interface VisualizationConfigType {
  type: string;
  component: React.ComponentType<VisualizationConfigProps>;
}

type BaseField = {
  title: string,
  name: string,
  helpComponent?: React.ComponentType,
  description?: string,
  isShown?: (formValues: VisualizationConfigFormValues) => boolean,
};

type BaseRequiredField = BaseField & {
  required: boolean,
};

type SelectField = BaseRequiredField & {
  type: 'select',
  options: ReadonlyArray<string | [string, any]>,
};

type BooleanField = BaseField & {
  type: 'boolean',
};

export type NumericField = BaseRequiredField & {
  type: 'numeric',
  step?: string,
};

export type ConfigurationField = SelectField | BooleanField | NumericField;

export interface VisualizationCapabilities {
  'event-annotations': undefined,
}

export type VisualizationCapability = keyof VisualizationCapabilities;

export interface VisualizationType<Type extends string, ConfigType extends VisualizationConfig = VisualizationConfig, ConfigFormValuesType extends VisualizationConfigFormValues = VisualizationConfigFormValues> {
  type: string;
  displayName: string;
  component: VisualizationComponent<Type>;
  config?: VisualizationConfigDefinition<ConfigType, ConfigFormValuesType>;
  capabilities?: Array<VisualizationCapability>;
  validate?: (formValues: WidgetConfigFormValues) => FormikErrors<VisualizationFormValues>;
}

interface ResultHandler<T, R> {
  convert: (result: T) => R;
}

interface SearchType<T, R> {
  type: string;
  handler: ResultHandler<T, R>;
  defaults: {};
}

interface ExportFormat {
  type: string;
  displayName: () => string;
  disabled?: () => boolean;
  mimeType: string;
  fileExtension: string;
}

export interface SystemConfiguration {
  configType: string;
  component: React.ComponentType<{
    config: any,
    updateConfig: (newConfig: any) => any,
  }>;
}

export type SearchTypeResult = {
  type: string,
  effective_timerange: TimeRange,
};

export type MessageResult = {
  type: 'messages',
  total: number,
};

export interface SearchTypeResultTypes {
  generic: SearchTypeResult,
  messages: MessageResult,
}

export interface ActionContexts {
  view: View,
  analysisDisabledFields: Array<string>,
  currentUser: User,
  widget: Widget,
  message: Message,
  valuePath: ValuePath,
  isLocalNode: boolean,
}

export type SearchTypeResults = { [id: string]: SearchTypeResultTypes[keyof SearchTypeResultTypes] };

export type MessagePreviewOption = {
  title: string,
  isChecked: (config: MessagesWidgetConfig) => boolean,
  isDisabled: (config: MessagesWidgetConfig) => boolean,
  help?: string,
  onChange: (config: MessagesWidgetConfig, onConfigChange: (config: MessagesWidgetConfig) => void) => void
  sort: number,
}

type MessageAugmentation = {
  id: string,
  component: React.ComponentType<{ message: Message }>,
}

type MessageDetailContextProviderProps = {
  message: Message,
}

export type CopyWidgetToDashboardHook = (widgetId: string, search: View, dashboard: View) => View;

type RemovingWidgetHook = (widgetId: string, dashboardId: string) => boolean;

interface MessageRowOverrideProps {
  messageFields: Message['fields'],
  config: MessagesWidgetConfig,
  renderMessageRow: () => React.ReactNode,
}

export interface SearchBarControl {
  component: React.ComponentType;
  id: string;
  placement: 'left' | 'right';
}

declare module 'graylog-web-plugin/plugin' {
  export interface PluginExports {
    creators?: Array<Creator>;
    enterpriseWidgets?: Array<WidgetExport>;
    externalValueActions?: Array<ActionDefinition>;
    fieldActions?: Array<ActionDefinition>;
    messageAugmentations?: Array<MessageAugmentation>;
    searchTypes?: Array<SearchType<any, any>>;
    systemConfigurations?: Array<SystemConfiguration>;
    valueActions?: Array<ActionDefinition>;
    'views.completers'?: Array<Completer>;
    'views.components.widgets.messageTable.previewOptions'?: Array<MessagePreviewOption>;
    'views.components.widgets.messageTable.messageRowOverride'?: Array<React.ComponentType<MessageRowOverrideProps>>;
    'views.components.widgets.messageDetails.contextProviders'?: Array<React.ComponentType<MessageDetailContextProviderProps>>;
    'views.components.searchBar'?: Array<() => SearchBarControl | null>;
    'views.elements.header'?: Array<React.ComponentType>;
    'views.elements.queryBar'?: Array<React.ComponentType>;
    'views.elements.validationErrorExplanation'?: Array<React.ComponentType<{ validationState: QueryValidationState }>>;
    'views.export.formats'?: Array<ExportFormat>;
    'views.hooks.confirmDeletingDashboard'?: Array<(view: View) => Promise<boolean | null>>,
    'views.hooks.confirmDeletingDashboardPage'?: Array<(dashboardId: string, queryId: string, widgetIds: { [queryId: string]: Array<string> }) => Promise<boolean | null>>,
    'views.hooks.confirmDeletingWidget'?: Array<(widget: Widget, view: View, title: string) => Promise<boolean | null>>,
    'views.hooks.executingView'?: Array<ViewHook>;
    'views.hooks.loadingView'?: Array<ViewHook>;
    'views.hooks.searchRefresh'?: Array<SearchRefreshCondition>;
    'views.hooks.copyWidgetToDashboard'?: Array<CopyWidgetToDashboardHook>;
    'views.hooks.removingWidget'?: Array<RemovingWidgetHook>;
    'views.overrides.widgetEdit'?: Array<React.ComponentType<OverrideProps>>;
    'views.widgets.actions'?: Array<WidgetActionType>;
    'views.requires.provided'?: Array<string>;
    visualizationConfigTypes?: Array<VisualizationConfigType>;
    visualizationTypes?: Array<VisualizationType<any>>;
  }
}
