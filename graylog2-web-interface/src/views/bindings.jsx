// @flow strict
import Routes from 'routing/Routes';
import * as Permissions from 'views/Permissions';

import { MessageListHandler } from 'views/logic/searchtypes';
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

import PivotConfigGenerator from 'views/logic/searchtypes/aggregation/PivotConfigGenerator';
import PivotHandler from 'views/logic/searchtypes/pivot/PivotHandler';
import PivotTransformer from 'views/logic/searchresulttransformers/PivotTransformer';

import Widget from 'views/logic/widgets/Widget';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import MessagesWidget from 'views/logic/widgets/MessagesWidget';
import DataTable from 'views/components/datatable/DataTable';
import FieldStatisticsHandler from 'views/logic/fieldactions/FieldStatisticsHandler';
import ExcludeFromQueryHandler from 'views/logic/valueactions/ExcludeFromQueryHandler';
import { isFunction } from 'views/logic/aggregationbuilder/Series';
import AggregationControls from 'views/components/aggregationbuilder/AggregationControls';
import EditMessageList from 'views/components/widgets/EditMessageList';
import { DashboardsPage, ShowViewPage, NewSearchPage, ViewManagementPage } from 'views/pages';

import AddMessageCountActionHandler from 'views/logic/fieldactions/AddMessageCountActionHandler';
import AddMessageTableActionHandler from 'views/logic/fieldactions/AddMessageTableActionHandler';
import RemoveFromTableActionHandler from 'views/logic/fieldactions/RemoveFromTableActionHandler';
import RemoveFromAllTablesActionHandler from 'views/logic/fieldactions/RemoveFromAllTablesActionHandler';
import CreateCustomAggregation from 'views/logic/creatoractions/CreateCustomAggregation';
import SelectExtractorType from 'views/logic/valueactions/SelectExtractorType';

import VisualizationConfig from 'views/logic/aggregationbuilder/visualizations/VisualizationConfig';
import WorldMapVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/WorldMapVisualizationConfig';
import BarVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/BarVisualizationConfig';

import ViewSharing from 'views/logic/views/sharing/ViewSharing';
import AllUsersOfInstance from 'views/logic/views/sharing/AllUsersOfInstance';
import SpecificRoles from 'views/logic/views/sharing/SpecificRoles';
import SpecificUsers from 'views/logic/views/sharing/SpecificUsers';

import UseInNewQueryHandler from 'views/logic/valueactions/UseInNewQueryHandler';
import ShowDocumentsHandler from 'views/logic/valueactions/ShowDocumentsHandler';
import HighlightValueHandler from 'views/logic/valueactions/HighlightValueHandler';
import FieldNameCompletion from 'views/components/searchbar/completions/FieldNameCompletion';
import OperatorCompletion from 'views/components/searchbar/completions/OperatorCompletion';
import requirementsProvided from 'views/hooks/RequirementsProvided';
import type { ValueActionHandlerConditionProps } from 'views/logic/valueactions/ValueActionHandler';
import type { FieldActionHandlerConditionProps } from 'views/logic/fieldactions/FieldActionHandler';
import { dashboardsPath, extendedSearchPath, newDashboardsPath, showViewsPath, viewsPath } from 'views/Constants';
import NewDashboardPage from 'views/pages/NewDashboardPage';
import StreamSearchPage from './pages/StreamSearchPage';

Widget.registerSubtype(AggregationWidget.type, AggregationWidget);
Widget.registerSubtype(MessagesWidget.type, MessagesWidget);
// $FlowFixMe: type is not undefined in this case.
VisualizationConfig.registerSubtype(WorldMapVisualization.type, WorldMapVisualizationConfig);
// $FlowFixMe: type is not undefined in this case.
VisualizationConfig.registerSubtype(BarVisualization.type, BarVisualizationConfig);

ViewSharing.registerSubtype(AllUsersOfInstance.Type, AllUsersOfInstance);
ViewSharing.registerSubtype(SpecificRoles.Type, SpecificRoles);
ViewSharing.registerSubtype(SpecificUsers.Type, SpecificUsers);

export default {
  pages: {
    search: { component: NewSearchPage },
  },
  routes: [
    { path: extendedSearchPath, component: NewSearchPage, permissions: Permissions.ExtendedSearch.Use },
    { path: viewsPath, component: ViewManagementPage, permissions: Permissions.View.Use },
    { path: newDashboardsPath, component: NewDashboardPage },
    { path: showViewsPath, component: ShowViewPage },
    { path: Routes.stream_search(':streamId'), component: StreamSearchPage },
    { path: dashboardsPath, component: DashboardsPage },
  ],
  enterpriseWidgets: [
    {
      type: 'MESSAGES',
      displayName: 'Message List',
      defaultHeight: 5,
      defaultWidth: 6,
      visualizationComponent: MessageList,
      editComponent: EditMessageList,
      searchResultTransformer: (data: Array<*>) => data[0],
      searchTypes: () => [{ type: 'messages' }],
      titleGenerator: () => 'Untitled Message Table',
    },
    {
      type: 'AGGREGATION',
      displayName: 'Results',
      defaultHeight: 4,
      defaultWidth: 4,
      visualizationComponent: AggregationBuilder,
      editComponent: AggregationControls,
      searchResultTransformer: PivotTransformer,
      searchTypes: PivotConfigGenerator,
      titleGenerator: (widget: Widget) => {
        if (widget.config.rowPivots.length > 0) {
          return `Aggregating ${widget.config.series.map(s => s.effectiveName)} by ${widget.config.rowPivots.map(({ field }) => field).join(', ')}`;
        }
        if (widget.config.series.length > 0) {
          return `Aggregating ${widget.config.series.map(s => s.effectiveName)}`;
        }
        return 'Untitled Aggregation';
      },
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
  ],
  fieldActions: [
    {
      type: 'chart',
      title: 'Chart',
      handler: ChartActionHandler,
      condition: ({ type }: FieldActionHandlerConditionProps) => type.isNumeric(),
    },
    {
      type: 'aggregate',
      title: 'Aggregate',
      handler: AggregateActionHandler,
      condition: ({ type }: FieldActionHandlerConditionProps) => !type.isCompound(),
    },
    {
      type: 'statistics',
      title: 'Statistics',
      handler: FieldStatisticsHandler,
    },
    {
      type: 'add-to-table',
      title: 'Add to table',
      handler: AddToTableActionHandler,
      condition: AddToTableActionHandler.condition,
      hide: AddToTableActionHandler.hide,
    },
    {
      type: 'remove-to-table',
      title: 'Remove from table',
      handler: RemoveFromTableActionHandler,
      condition: RemoveFromTableActionHandler.condition,
      hide: RemoveFromTableActionHandler.hide,
    },
    {
      type: 'add-to-all-tables',
      title: 'Add to all tables',
      handler: AddToAllTablesActionHandler,
    },
    {
      type: 'remove-from-all-tables',
      title: 'Remove from all tables',
      handler: RemoveFromAllTablesActionHandler,
    },
  ],
  valueActions: [
    {
      type: 'exclude',
      title: 'Exclude from results',
      handler: new ExcludeFromQueryHandler().handle,
      condition: ({ field }: ValueActionHandlerConditionProps) => !isFunction(field),
    },
    {
      type: 'add-to-query',
      title: 'Add to query',
      handler: new AddToQueryHandler().handle,
      condition: ({ field }: ValueActionHandlerConditionProps) => !isFunction(field),
    },
    {
      type: 'new-query',
      title: 'Use in new query',
      handler: UseInNewQueryHandler,
    },
    {
      type: 'show-bucket',
      title: 'Show documents for value',
      handler: ShowDocumentsHandler,
      condition: ShowDocumentsHandler.isEnabled,
    },
    {
      type: 'create-extractor',
      title: 'Create extractor',
      condition: ({ type }: ValueActionHandlerConditionProps) => type.type === 'string',
      component: SelectExtractorType,
    },
    {
      type: 'highlight-value',
      title: 'Highlight this value',
      handler: HighlightValueHandler,
      condition: HighlightValueHandler.condition,
    },
  ],
  visualizationTypes: [
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
      title: 'Custom Aggregation',
      func: CreateCustomAggregation,
    },
  ],
  'views.completers': [
    new FieldNameCompletion(),
    new OperatorCompletion(),
  ],
  'views.hooks.loadingView': [
    requirementsProvided,
  ],
};
