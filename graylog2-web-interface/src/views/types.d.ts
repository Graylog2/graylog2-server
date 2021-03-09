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

interface EditWidgetComponentProps<Config extends WidgetConfig = WidgetConfig> {
  children: React.ReactNode,
  config: Config,
  editing: boolean;
  id: string;
  type: string;
  fields: Immutable.List<FieldTypeMapping>,
  onChange: (newConfig: Config) => void,
}

interface WidgetComponentProps<Config extends WidgetConfig = WidgetConfig> {
  config: Config;
  data: { [key: string]: Result };
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
}

interface VisualizationConfigProps {
  config: WidgetConfig;
  onChange: (newConfig: WidgetConfig) => void;
}

interface VisualizationConfigType {
  type: string;
  component: React.ComponentType<VisualizationConfigProps>;
}

interface VisualizationType {
  type: string;
  displayName: string;
  component: VisualizationComponent;
}

interface ResultHandler<T, R> {
  convert: (result: T) => R;
}
interface SearchType {
  type: string;
  handler: ResultHandler;
  defaults: {};
}

declare module 'graylog-web-plugin/plugin' {
  export interface PluginExports {
    creators?: Array<Creator>;
    enterpriseWidgets?: Array<WidgetExport>;
    fieldActions?: Array<ActionDefinition>;
    searchTypes?: Array<SearchType>;
    valueActions?: Array<ActionDefinition>;
    'views.completers'?: Array<Completer>;
    'views.elements.header'?: Array<React.ComponentType>;
    'views.elements.queryBar'?: Array<React.ComponentType>;
    'views.hooks.executingView'?: Array<ViewHook>,
    'views.hooks.loadingView'?: Array<ViewHook>,
    'views.hooks.searchRefresh'?: Array<SearchRefreshCondition>;
    'views.overrides.widgetEdit'?: Array<React.ComponentType<OverrideProps>>;
    'views.widgets.actions'?: Array<WidgetActionType>;
    'views.requires.provided'?: Array<string>;
    visualizationConfigTypes?: Array<VisualizationConfigType>;
    visualizationTypes?: Array<VisualizationType>;
  }
}
