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
import get from 'lodash/get';
import type { PluginExports } from 'graylog-web-plugin/plugin';

import type { WidgetComponentProps } from 'views/types';
import Routes from 'routing/Routes';
import App from 'routing/App';
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
import EditMessageList from 'views/components/widgets/EditMessageList';
import {
  DashboardsPage,
  ShowViewPage,
  NewSearchPage,
  NewDashboardPage,
  StreamSearchPage,
  EventDefinitionReplaySearchPage,
  EventReplaySearchPage,
} from 'views/pages';
import AddMessageCountActionHandler, { CreateMessageCount } from 'views/logic/fieldactions/AddMessageCountActionHandler';
import AddMessageTableActionHandler, { CreateMessagesWidget } from 'views/logic/fieldactions/AddMessageTableActionHandler';
import RemoveFromTableActionHandler from 'views/logic/fieldactions/RemoveFromTableActionHandler';
import RemoveFromAllTablesActionHandler from 'views/logic/fieldactions/RemoveFromAllTablesActionHandler';
import AddCustomAggregation, { CreateCustomAggregation } from 'views/logic/creatoractions/AddCustomAggregation';
import SelectExtractorType from 'views/logic/valueactions/SelectExtractorType';
import VisualizationConfig from 'views/logic/aggregationbuilder/visualizations/VisualizationConfig';
import WorldMapVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/WorldMapVisualizationConfig';
import BarVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/BarVisualizationConfig';
import ShowDocumentsHandler from 'views/logic/valueactions/ShowDocumentsHandler';
import HighlightValueHandler from 'views/logic/valueactions/HighlightValueHandler';
import FieldNameCompletion from 'views/components/searchbar/completions/FieldNameCompletion';
import FieldValueCompletion from 'views/components/searchbar/completions/FieldValueCompletion';
import OperatorCompletion from 'views/components/searchbar/completions/OperatorCompletion';
import requirementsProvided from 'views/hooks/RequirementsProvided';
import bindSearchParamsFromQuery from 'views/hooks/BindSearchParamsFromQuery';
import {
  dashboardsPath,
  dashboardsTvPath,
  extendedSearchPath,
  newDashboardsPath,
  showDashboardsPath,
  newSearchPath,
  showSearchPath,
  showViewsPath,
} from 'views/Constants';
import ShowDashboardInBigDisplayMode from 'views/pages/ShowDashboardInBigDisplayMode';
import LookupTableParameter from 'views/logic/parameters/LookupTableParameter';
import HeatmapVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/HeatmapVisualizationConfig';
import visualizationBindings from 'views/components/visualizations/bindings';
import { AggregationWizard } from 'views/components/aggregationwizard';
import { filterCloudValueActions } from 'util/conditional/filterValueActions';
import CopyValueToClipboard from 'views/logic/valueactions/CopyValueToClipboard';
import CopyFieldToClipboard from 'views/logic/fieldactions/CopyFieldToClipboard';
import DataTableVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/DataTableVisualizationConfig';
import ViewHeader from 'views/components/views/ViewHeader';
import ScatterVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/ScatterVisualizationConfig';
import ScatterVisualization from 'views/components/visualizations/scatter/ScatterVisualization';
import Icon from 'components/common/Icon';
import viewsReducers from 'views/viewsReducers';
import CreateEventDefinition from 'views/logic/valueactions/createEventDefinition/CreateEventDefinition';

import type { ActionHandlerArguments } from './components/actions/ActionHandler';
import NumberVisualizationConfig from './logic/aggregationbuilder/visualizations/NumberVisualizationConfig';
import AreaVisualization from './components/visualizations/area/AreaVisualization';
import LineVisualizationConfig from './logic/aggregationbuilder/visualizations/LineVisualizationConfig';
import AreaVisualizationConfig from './logic/aggregationbuilder/visualizations/AreaVisualizationConfig';
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
VisualizationConfig.registerSubtype(DataTable.type, DataTableVisualizationConfig);
VisualizationConfig.registerSubtype(ScatterVisualization.type, ScatterVisualizationConfig);

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
    {
      path: `${Routes.unqualified.stream_search(':streamId')}/new`,
      component: NewSearchRedirectPage,
      parentComponent: null,
    },
    { path: Routes.unqualified.stream_search(':streamId'), component: StreamSearchPage, parentComponent: App },
    { path: extendedSearchPath, component: NewSearchPage, parentComponent: App },
    { path: showViewsPath, component: ShowViewPage, parentComponent: App },
    { path: Routes.ALERTS.replay_search(':alertId'), component: EventReplaySearchPage, parentComponent: App },
    { path: Routes.ALERTS.DEFINITIONS.replay_search(':definitionId'), component: EventDefinitionReplaySearchPage, parentComponent: App },
  ],
  enterpriseWidgets: [
    {
      type: 'MESSAGES',
      displayName: 'Message List',
      defaultHeight: 5,
      reportStyle: () => ({ width: 800 }),
      defaultWidth: 6,
      // TODO: Subtyping needs to be taken into account
      visualizationComponent: MessageList as unknown as React.ComponentType<WidgetComponentProps>,
      editComponent: EditMessageList,
      hasEditSubmitButton: true,
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
      editComponent: AggregationWizard,
      hasEditSubmitButton: true,
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
          return `Aggregating ${widget.config.series.map((s) => s.effectiveName).join(', ')} by ${widget.config.rowPivots.flatMap(({ fields }) => fields).join(', ')}`;
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
      thunk: ChartActionHandler,
      isEnabled: ({ type }) => type.isNumeric(),
      resetFocus: true,
    },
    {
      type: 'aggregate',
      title: 'Show top values',
      thunk: AggregateActionHandler,
      isEnabled: (({
        field,
        type,
        contexts: { analysisDisabledFields },
      }) => (!isFunction(field) && type.isEnumerable() && !type.isDecorated() && !isAnalysisDisabled(field, analysisDisabledFields))),
      resetFocus: true,
    },
    {
      type: 'statistics',
      title: 'Statistics',
      isEnabled: (({
        field,
        type,
        contexts: { analysisDisabledFields },
      }) => (!isFunction(field) && !type.isDecorated() && !isAnalysisDisabled(field, analysisDisabledFields))),
      thunk: FieldStatisticsHandler,
      resetFocus: false,
    },
    {
      type: 'add-to-table',
      title: 'Add to table',
      thunk: AddToTableActionHandler,
      isEnabled: AddToTableActionHandler.isEnabled,
      isHidden: AddToTableActionHandler.isHidden,
      resetFocus: false,
    },
    {
      type: 'remove-from-table',
      title: 'Remove from table',
      thunk: RemoveFromTableActionHandler,
      isEnabled: RemoveFromTableActionHandler.isEnabled,
      isHidden: RemoveFromTableActionHandler.isHidden,
      resetFocus: false,
    },
    {
      type: 'add-to-all-tables',
      title: 'Add to all tables',
      thunk: AddToAllTablesActionHandler,
      isEnabled: ({ field, type }) => (!isFunction(field) && !type.isDecorated()),
      resetFocus: false,
    },
    {
      type: 'remove-from-all-tables',
      title: 'Remove from all tables',
      thunk: RemoveFromAllTablesActionHandler,
      isEnabled: ({ field, type }) => (!isFunction(field) && !type.isDecorated()),
      resetFocus: false,
    },
    {
      type: 'copy-field-to-clipboard',
      title: 'Copy field name to clipboard',
      handler: CopyFieldToClipboard,
      isEnabled: () => true,
      resetFocus: false,
    },
  ],
  valueActions: filterCloudValueActions([
    {
      type: 'exclude',
      title: 'Exclude from results',
      thunk: ExcludeFromQueryHandler,
      isEnabled: ({ field, type }: ActionHandlerArguments) => (!isFunction(field) && !type.isDecorated()),
      resetFocus: false,
    },
    {
      type: 'add-to-query',
      title: 'Add to query',
      thunk: AddToQueryHandler,
      isEnabled: ({ field, type }: ActionHandlerArguments) => (!isFunction(field) && !type.isDecorated()),
      resetFocus: false,
    },
    {
      type: 'show-bucket',
      title: 'Show documents for value',
      thunk: ShowDocumentsHandler,
      isEnabled: ShowDocumentsHandler.isEnabled,
      resetFocus: true,
    },
    {
      type: 'create-extractor',
      title: 'Create extractor',
      isEnabled: ({ type, contexts }) => (!!contexts.message && !type.isDecorated() && !!contexts.isLocalNode),
      component: SelectExtractorType,
      resetFocus: false,
    },
    {
      type: 'highlight-value',
      title: 'Highlight this value',
      thunk: HighlightValueHandler,
      isEnabled: HighlightValueHandler.isEnabled,
      resetFocus: false,
    },
    {
      type: 'copy-value-to-clipboard',
      title: 'Copy value to clipboard',
      handler: CopyValueToClipboard,
      isEnabled: () => true,
      resetFocus: false,
    },
    {
      type: 'create-event-definition-from-value',
      title: 'Create event definition',
      isEnabled: () => true,
      resetFocus: false,
      component: CreateEventDefinition,
    },
  ], ['create-extractor']),
  visualizationTypes: visualizationBindings,
  widgetCreators: [{
    title: 'Message Count',
    func: CreateMessageCount,
    icon: () => <Icon name="hashtag" />,
  }, {
    title: 'Message Table',
    func: CreateMessagesWidget,
    icon: () => <Icon name="list" />,
  }, {
    title: 'Custom Aggregation',
    func: CreateCustomAggregation,
    icon: () => <Icon name="chart-column" />,
  }],
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
      func: AddCustomAggregation,
    },
  ],
  'views.completers': [
    new FieldNameCompletion(),
    new FieldValueCompletion(),
    new OperatorCompletion(),
  ],
  'views.hooks.loadingView': [
    requirementsProvided,
    bindSearchParamsFromQuery,
  ],
  'views.elements.header': [
    () => <IfSearch><MigrateFieldCharts /></IfSearch>,
    ViewHeader,
  ],
  'views.export.formats': [
    {
      type: 'csv',
      displayName: () => 'Comma-Separated Values (CSV)',
      mimeType: 'text/csv',
      fileExtension: 'csv',
    },
  ],
  'views.components.widgets.messageTable.previewOptions': [
    {
      title: 'Show message in new row',
      isChecked: (config) => config.showMessageRow,
      isDisabled: () => false,
      onChange: (config, onConfigChange) => {
        const willShowRowMessage = !config.showMessageRow;
        const willShowSummary = !willShowRowMessage ? false : config.showSummary;
        const newConfig = config.toBuilder().showMessageRow(willShowRowMessage).showSummary(willShowSummary).build();

        return onConfigChange(newConfig);
      },
      sort: 1,
    },
  ],
  'views.reducers': viewsReducers,
};

export default exports;
