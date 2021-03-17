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
import { get } from 'lodash';
import { PluginExports } from 'graylog-web-plugin/plugin';

import Routes from 'routing/Routes';
import App from 'routing/App';
import * as Permissions from 'views/Permissions';
import { MessageListHandler } from 'views/logic/searchtypes/messages';
import { MessageList } from 'views/components/widgets';
import AddToTableActionHandler from 'views/logic/fieldactions/AddToTableActionHandler';
import AddToAllTablesActionHandler from 'views/logic/fieldactions/AddToAllTablesActionHandler';
import AddToQueryHandler from 'views/logic/valueactions/AddToQueryHandler';
import AggregateActionHandler from 'views/logic/fieldactions/AggregateActionHandler';
import ChartActionHandler from 'views/logic/fieldactions/ChartActionHandler';
import AggregationBuilder from 'views/components/aggregationbuilder/AggregationBuilder';
import BarVisualization from 'views/components/visualizations/bar/BarVisualization';
import LineVisualization from 'views/components/visualizations/line/LineVisualization';
import NumberVisualization from 'views/components/visualizations/number/NumberVisualization';
import PieVisualization from 'views/components/visualizations/pie/PieVisualization';
import ScatterVisualization from 'views/components/visualizations/scatter/ScatterVisualization';
import WorldMapVisualization from 'views/components/visualizations/worldmap/WorldMapVisualization';
import HeatmapVisualization from 'views/components/visualizations/heatmap/HeatmapVisualization';
import MigrateFieldCharts from 'views/components/MigrateFieldCharts';
import IfSearch from 'views/components/search/IfSearch';
import PivotConfigGenerator from 'views/logic/searchtypes/aggregation/PivotConfigGenerator';
import PivotHandler from 'views/logic/searchtypes/pivot/PivotHandler';
import PivotTransformer from 'views/logic/searchresulttransformers/PivotTransformer';
import EventHandler from 'views/logic/searchtypes/events/EventHandler';
import Widget from 'views/logic/widgets/Widget';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import MessagesWidget from 'views/logic/widgets/MessagesWidget';
import DataTable from 'views/components/datatable/DataTable';
import FieldStatisticsHandler from 'views/logic/fieldactions/FieldStatisticsHandler';
import ExcludeFromQueryHandler from 'views/logic/valueactions/ExcludeFromQueryHandler';
import { isFunction } from 'views/logic/aggregationbuilder/Series';
import AggregationControls from 'views/components/aggregationbuilder/AggregationControls';
import EditMessageList from 'views/components/widgets/EditMessageList';
import { DashboardsPage, ShowViewPage, NewSearchPage, ViewManagementPage, NewDashboardPage, StreamSearchPage } from 'views/pages';
import AddMessageCountActionHandler from 'views/logic/fieldactions/AddMessageCountActionHandler';
import AddMessageTableActionHandler from 'views/logic/fieldactions/AddMessageTableActionHandler';
import RemoveFromTableActionHandler from 'views/logic/fieldactions/RemoveFromTableActionHandler';
import RemoveFromAllTablesActionHandler from 'views/logic/fieldactions/RemoveFromAllTablesActionHandler';
import CreateCustomAggregation from 'views/logic/creatoractions/CreateCustomAggregation';
import SelectExtractorType from 'views/logic/valueactions/SelectExtractorType';
import VisualizationConfig from 'views/logic/aggregationbuilder/visualizations/VisualizationConfig';
import WorldMapVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/WorldMapVisualizationConfig';
import BarVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/BarVisualizationConfig';
import ShowDocumentsHandler from 'views/logic/valueactions/ShowDocumentsHandler';
import HighlightValueHandler from 'views/logic/valueactions/HighlightValueHandler';
import FieldNameCompletion from 'views/components/searchbar/completions/FieldNameCompletion';
import OperatorCompletion from 'views/components/searchbar/completions/OperatorCompletion';
import requirementsProvided from 'views/hooks/RequirementsProvided';
import bindSearchParamsFromQuery from 'views/hooks/BindSearchParamsFromQuery';
import {
  dashboardsPath,
  dashboardsTvPath,
  extendedSearchPath,
  newDashboardsPath,
  showDashboardsPath,
  showViewsPath,
  newSearchPath,
  showSearchPath,
  viewsPath,
} from 'views/Constants';
import ShowDashboardInBigDisplayMode from 'views/pages/ShowDashboardInBigDisplayMode';
import LookupTableParameter from 'views/logic/parameters/LookupTableParameter';
import HeatmapVisualizationConfiguration from 'views/components/aggregationbuilder/HeatmapVisualizationConfiguration';
import HeatmapVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/HeatmapVisualizationConfig';

import type { ActionHandlerArguments } from './components/actions/ActionHandler';
import NumberVisualizationConfig from './logic/aggregationbuilder/visualizations/NumberVisualizationConfig';
import BarVisualizationConfiguration from './components/aggregationbuilder/BarVisualizationConfiguration';
import NumberVisualizationConfiguration from './components/aggregationbuilder/NumberVisualizationConfiguration';
import AreaVisualization from './components/visualizations/area/AreaVisualization';
import LineVisualizationConfig from './logic/aggregationbuilder/visualizations/LineVisualizationConfig';
import AreaVisualizationConfig from './logic/aggregationbuilder/visualizations/AreaVisualizationConfig';
import LineVisualizationConfiguration from './components/aggregationbuilder/LineVisualizationConfiguration';
import AreaVisualizationConfiguration from './components/aggregationbuilder/AreaVisualizationConfiguration';
import Parameter from './logic/parameters/Parameter';
import ValueParameter from './logic/parameters/ValueParameter';
import MessageConfigGenerator from './logic/searchtypes/messages/MessageConfigGenerator';
import UnknownWidget from './components/widgets/UnknownWidget';
import NewSearchRedirectPage from './pages/NewSearchRedirectPage';

Widget.registerSubtype(AggregationWidget.type, AggregationWidget);
Widget.registerSubtype(MessagesWidget.type, MessagesWidget);
VisualizationConfig.registerSubtype(WorldMapVisualization.type, WorldMapVisualizationConfig);
VisualizationConfig.registerSubtype(BarVisualization.type, BarVisualizationConfig);
VisualizationConfig.registerSubtype(NumberVisualization.type, NumberVisualizationConfig);
VisualizationConfig.registerSubtype(LineVisualization.type, LineVisualizationConfig);
VisualizationConfig.registerSubtype(AreaVisualization.type, AreaVisualizationConfig);
VisualizationConfig.registerSubtype(HeatmapVisualization.type, HeatmapVisualizationConfig);

Parameter.registerSubtype(ValueParameter.type, ValueParameter);
Parameter.registerSubtype(LookupTableParameter.type, LookupTableParameter);

const isAnalysisDisabled = (field: string, analysisDisabledFields: string[] = []) => analysisDisabledFields.includes(field);

const exports: PluginExports = {
  pages: {
    search: { component: NewSearchPage },
  },
  routes: [
    { path: newDashboardsPath, component: NewDashboardPage, parentComponent: App },
    { path: dashboardsTvPath, component: ShowDashboardInBigDisplayMode, parentComponent: null },
    { path: dashboardsPath, component: DashboardsPage },
    { path: showDashboardsPath, component: ShowViewPage, parentComponent: App },

    { path: newSearchPath, component: NewSearchRedirectPage, parentComponent: null },
    { path: showSearchPath, component: ShowViewPage, parentComponent: App },
    { path: `${Routes.unqualified.stream_search(':streamId')}/new`, component: NewSearchRedirectPage, parentComponent: null },
    { path: Routes.unqualified.stream_search(':streamId'), component: StreamSearchPage, parentComponent: App },
    { path: extendedSearchPath, component: NewSearchPage, permissions: Permissions.ExtendedSearch.Use, parentComponent: App },

    { path: viewsPath, component: ViewManagementPage, permissions: Permissions.View.Use },
    { path: showViewsPath, component: ShowViewPage, parentComponent: App },
  ],
  enterpriseWidgets: [
    {
      type: 'MESSAGES',
      displayName: 'Message List',
      defaultHeight: 5,
      reportStyle: () => ({ width: 800 }),
      defaultWidth: 6,
      visualizationComponent: MessageList,
      editComponent: EditMessageList,
      needsControlledHeight: () => false,
      searchResultTransformer: (data: Array<unknown>) => data[0],
      searchTypes: MessageConfigGenerator,
      titleGenerator: () => MessagesWidget.defaultTitle,
    },
    {
      type: 'AGGREGATION',
      displayName: 'Results',
      defaultHeight: 4,
      defaultWidth: 4,
      reportStyle: () => ({ width: 600 }),
      visualizationComponent: AggregationBuilder,
      editComponent: AggregationControls,
      needsControlledHeight: (widget: Widget) => {
        const widgetVisualization = get(widget, 'config.visualization');
        const flexibleHeightWidgets = [
          DataTable.type,
        ];

        return !flexibleHeightWidgets.find((visualization) => visualization === widgetVisualization);
      },
      searchResultTransformer: PivotTransformer,
      searchTypes: PivotConfigGenerator,
      titleGenerator: (widget: Widget) => {
        if (widget.config.rowPivots.length > 0) {
          return `Aggregating ${widget.config.series.map((s) => s.effectiveName).join(', ')} by ${widget.config.rowPivots.map(({ field }) => field).join(', ')}`;
        }

        if (widget.config.series.length > 0) {
          return `Aggregating ${widget.config.series.map((s) => s.effectiveName).join(', ')}`;
        }

        return AggregationWidget.defaultTitle;
      },
    },
    {
      type: 'default',
      visualizationComponent: UnknownWidget,
      needsControlledHeight: () => true,
      editComponent: UnknownWidget,
      searchTypes: () => [],
    },
  ],
  searchTypes: [
    {
      type: 'messages',
      handler: MessageListHandler,
      defaults: {
        limit: 150,
        offset: 0,
      },
    },
    {
      type: 'pivot',
      handler: PivotHandler,
      defaults: {},
    },
    {
      type: 'events',
      handler: EventHandler,
      defaults: {},
    },
  ],
  fieldActions: [
    {
      type: 'chart',
      title: 'Chart',
      handler: ChartActionHandler,
      isEnabled: ({ type }) => type.isNumeric(),
      resetFocus: true,
    },
    {
      type: 'aggregate',
      title: 'Show top values',
      handler: AggregateActionHandler,
      isEnabled: (({ field, type, contexts: { analysisDisabledFields } }) => (!isFunction(field) && !type.isCompound() && !type.isDecorated() && !isAnalysisDisabled(field, analysisDisabledFields))),
      resetFocus: true,
    },
    {
      type: 'statistics',
      title: 'Statistics',
      isEnabled: (({ field, type, contexts: { analysisDisabledFields } }) => (!isFunction(field) && !type.isDecorated() && !isAnalysisDisabled(field, analysisDisabledFields))),
      handler: FieldStatisticsHandler,
      resetFocus: false,
    },
    {
      type: 'add-to-table',
      title: 'Add to table',
      handler: AddToTableActionHandler,
      isEnabled: AddToTableActionHandler.isEnabled,
      isHidden: AddToTableActionHandler.isHidden,
      resetFocus: false,
    },
    {
      type: 'remove-from-table',
      title: 'Remove from table',
      handler: RemoveFromTableActionHandler,
      isEnabled: RemoveFromTableActionHandler.isEnabled,
      isHidden: RemoveFromTableActionHandler.isHidden,
      resetFocus: false,
    },
    {
      type: 'add-to-all-tables',
      title: 'Add to all tables',
      handler: AddToAllTablesActionHandler,
      isEnabled: ({ field, type }) => (!isFunction(field) && !type.isDecorated()),
      resetFocus: false,
    },
    {
      type: 'remove-from-all-tables',
      title: 'Remove from all tables',
      handler: RemoveFromAllTablesActionHandler,
      isEnabled: ({ field, type }) => (!isFunction(field) && !type.isDecorated()),
      resetFocus: false,
    },
  ],
  valueActions: [
    {
      type: 'exclude',
      title: 'Exclude from results',
      handler: new ExcludeFromQueryHandler().handle,
      isEnabled: ({ field, type }: ActionHandlerArguments) => (!isFunction(field) && !type.isDecorated()),
      resetFocus: false,
    },
    {
      type: 'add-to-query',
      title: 'Add to query',
      handler: new AddToQueryHandler().handle,
      isEnabled: ({ field, type }: ActionHandlerArguments) => (!isFunction(field) && !type.isDecorated()),
      resetFocus: false,
    },
    {
      type: 'show-bucket',
      title: 'Show documents for value',
      handler: ShowDocumentsHandler,
      isEnabled: ShowDocumentsHandler.isEnabled,
      resetFocus: true,
    },
    {
      type: 'create-extractor',
      title: 'Create extractor',
      isEnabled: ({ type, contexts }) => (!!contexts.message && !type.isDecorated()),
      component: SelectExtractorType,
      resetFocus: false,
    },
    {
      type: 'highlight-value',
      title: 'Highlight this value',
      handler: HighlightValueHandler,
      isEnabled: HighlightValueHandler.isEnabled,
      resetFocus: false,
    },
  ],
  visualizationTypes: [
    {
      type: AreaVisualization.type,
      displayName: 'Area Chart',
      component: AreaVisualization,
    },
    {
      type: BarVisualization.type,
      displayName: 'Bar Chart',
      component: BarVisualization,
    },
    {
      type: LineVisualization.type,
      displayName: 'Line Chart',
      component: LineVisualization,
    },
    {
      type: WorldMapVisualization.type,
      displayName: 'World Map',
      component: WorldMapVisualization,
    },
    {
      type: PieVisualization.type,
      displayName: 'Pie Chart',
      component: PieVisualization,
    },
    {
      type: DataTable.type,
      displayName: 'Data Table',
      component: DataTable,
    },
    {
      type: NumberVisualization.type,
      displayName: 'Single Number',
      component: NumberVisualization,
    },
    {
      type: ScatterVisualization.type,
      displayName: 'Scatter Plot',
      component: ScatterVisualization,
    },
    {
      type: HeatmapVisualization.type,
      displayName: 'Heatmap',
      component: HeatmapVisualization,
    },
  ],
  visualizationConfigTypes: [
    {
      type: AreaVisualization.type,
      component: AreaVisualizationConfiguration,
    },
    {
      type: BarVisualization.type,
      component: BarVisualizationConfiguration,
    },
    {
      type: LineVisualization.type,
      component: LineVisualizationConfiguration,
    },
    {
      type: NumberVisualization.type,
      component: NumberVisualizationConfiguration,
    },
    {
      type: HeatmapVisualization.type,
      component: HeatmapVisualizationConfiguration,
    },
  ],
  creators: [
    {
      type: 'preset',
      title: 'Message Count',
      func: AddMessageCountActionHandler,
    },
    {
      type: 'preset',
      title: 'Message Table',
      func: AddMessageTableActionHandler,
    },
    {
      type: 'generic',
      title: 'Aggregation',
      func: CreateCustomAggregation,
    },
  ],
  'views.completers': [
    new FieldNameCompletion(),
    new OperatorCompletion(),
  ],
  'views.hooks.loadingView': [
    requirementsProvided,
    bindSearchParamsFromQuery,
  ],
  'views.elements.header': [
    () => <IfSearch><MigrateFieldCharts /></IfSearch>,
  ],
  'views.export.formats': [
    {
      type: 'csv',
      displayName: () => 'Comma-Separated Values (CSV)',
      mimeType: 'text/csv',
      fileExtension: 'csv',
    },
  ],
};

export default exports;
