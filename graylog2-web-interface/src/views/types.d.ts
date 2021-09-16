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
import React from 'react';
import * as Immutable from 'immutable';
import { FormikErrors } from 'formik';

import Widget from 'views/logic/widgets/Widget';
import { ActionDefinition } from 'views/components/actions/ActionHandler';
import { SearchRefreshCondition } from 'views/logic/hooks/SearchRefreshCondition';
import { VisualizationComponent } from 'views/components/aggregationbuilder/AggregationBuilder';
import { WidgetActionType } from 'views/components/widgets/Types';
import { Creator } from 'views/components/sidebar/create/AddWidgetButton';
import { ViewHook } from 'views/logic/hooks/ViewHook';
import WidgetConfig from 'views/logic/widgets/WidgetConfig';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import { Completer } from 'views/components/searchbar/SearchBarAutocompletions';
import { Result } from 'views/components/widgets/Widget';
import { Widgets } from 'views/stores/WidgetStore';
import { OverrideProps } from 'views/components/WidgetOverrideElements';
import {
  VisualizationConfigDefinition,
  VisualizationConfigFormValues,
  VisualizationFormValues,
  WidgetConfigFormValues,
} from 'views/components/aggregationwizard';
import VisualizationConfig from 'views/logic/aggregationbuilder/visualizations/VisualizationConfig';
import { TimeRange } from 'views/logic/queries/Query';
import { CopyWidgetToDashboardHook } from 'views/logic/views/CopyWidgetToDashboard';
import View from 'views/logic/views/View';
import User from 'logic/users/User';
import { Message } from 'views/components/messagelist/Types';
import { ValuePath } from 'views/logic/valueactions/ValueActionHandler';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';

type BackendWidgetPosition = {
  id: string,
  col: number,
  row: number,
  height: number,
  width: number,
};

export type WidgetPositions = { [widgetId: string]: WidgetPosition };

interface EditWidgetComponentProps<Config extends WidgetConfig = WidgetConfig> {
  children: React.ReactNode,
  config: Config,
  editing: boolean;
  id: string;
  type: string;
  fields: Immutable.List<FieldTypeMapping>,
  onChange: (newConfig: Config) => void,
}

interface WidgetResults {
 [key: string]: Result,
}

interface WidgetComponentProps<Config extends WidgetConfig = WidgetConfig, Result = WidgetResults> {
  config: Config;
  data: Result;
  editing: boolean;
  fields: Immutable.List<FieldTypeMapping>;
  filter: string;
  height: number;
  width: number;
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
  needsControlledHeight: (widget: Widget) => boolean;
  searchResultTransformer?: (data: Array<unknown>, widget: Widget) => unknown;
  searchTypes: (widget: Widget) => Array<any>;
  titleGenerator?: (widget: Widget) => string;
  reportStyle?: () => { width: React.CSSProperties['width'] };
  exportComponent?: React.ComponentType<{ widget: Widget }>;
}

interface VisualizationConfigProps {
  config: WidgetConfig;
  onChange: (newConfig: WidgetConfig) => void;
}

interface VisualizationConfigType {
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

type ConfigurationField = SelectField | BooleanField | NumericField;

export interface VisualizationCapabilities {
  'event-annotations': undefined,
}

export type VisualizationCapability = keyof VisualizationCapabilities;

interface VisualizationType<ConfigType extends VisualizationConfig = VisualizationConfig, ConfigFormValuesType extends VisualizationConfigFormValues = VisualizationConfigFormValues> {
  type: string;
  displayName: string;
  component: VisualizationComponent;
  config?: VisualizationConfigDefinition<ConfigType, ConfigFormValuesType>;
  capabilities?: Array<VisualizationCapability>;
  validate?: (formValues: WidgetConfigFormValues) => FormikErrors<VisualizationFormValues>;
}

interface ResultHandler<T, R> {
  convert: (result: T) => R;
}
interface SearchType {
  type: string;
  handler: ResultHandler;
  defaults: {};
}

interface ExportFormat {
  type: string;
  displayName: () => string;
  disabled?: () => boolean;
  mimeType: string;
  fileExtension: string;
}

interface SystemConfiguration {
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

declare module 'graylog-web-plugin/plugin' {
  export interface PluginExports {
    creators?: Array<Creator>;
    enterpriseWidgets?: Array<WidgetExport>;
    externalValueActions?: Array<ActionDefinition>;
    fieldActions?: Array<ActionDefinition>;
    messageAugmentations?: Array<MessageAugmentation>;
    searchTypes?: Array<SearchType>;
    systemConfigurations?: Array<SystemConfiguration>;
    valueActions?: Array<ActionDefinition>;
    'views.completers'?: Array<Completer>;
    'views.components.widgets.messageTable.previewOptions'?: Array<MessagePreviewOption>;
    'views.components.widgets.messageTable.messageRowOverride'?: Array<React.ComponentType<MessageRowOverrideProps>>;
    'views.components.widgets.messageDetails.contextProviders'?: Array<React.ComponentType<MessageDetailContextProviderProps>>,
    'views.elements.header'?: Array<React.ComponentType>;
    'views.elements.queryBar'?: Array<React.ComponentType>;
    'views.export.formats'?: Array<ExportFormat>;
    'views.hooks.executingView'?: Array<ViewHook>;
    'views.hooks.loadingView'?: Array<ViewHook>;
    'views.hooks.searchRefresh'?: Array<SearchRefreshCondition>;
    'views.hooks.copyWidgetToDashboard'?: Array<CopyWidgetToDashboardHook>;
    'views.overrides.widgetEdit'?: Array<React.ComponentType<OverrideProps>>;
    'views.widgets.actions'?: Array<WidgetActionType>;
    'views.requires.provided'?: Array<string>;
    visualizationConfigTypes?: Array<VisualizationConfigType>;
    visualizationTypes?: Array<VisualizationType>;
  }
}
