// @flow strict
import * as Permissions from 'enterprise/Permissions';

import { MessageListHandler } from 'enterprise/logic/searchtypes';
import { MessageList } from 'enterprise/components/widgets';

import AddToTableActionHandler from 'enterprise/logic/fieldactions/AddToTableActionHandler';
import AddToAllTablesActionHandler from 'enterprise/logic/fieldactions/AddToAllTablesActionHandler';
import AddToQueryHandler from 'enterprise/logic/valueactions/AddToQueryHandler';
import AggregateActionHandler from 'enterprise/logic/fieldactions/AggregateActionHandler';
import ChartActionHandler from 'enterprise/logic/fieldactions/ChartActionHandler';

import AggregationBuilder from 'enterprise/components/aggregationbuilder/AggregationBuilder';

import BarVisualization from 'enterprise/components/visualizations/bar/BarVisualization';
import LineVisualization from 'enterprise/components/visualizations/line/LineVisualization';
import NumberVisualization from 'enterprise/components/visualizations/number/NumberVisualization';
import PieVisualization from 'enterprise/components/visualizations/pie/PieVisualization';
import ScatterVisualization from 'enterprise/components/visualizations/scatter/ScatterVisualization';
import WorldMapVisualization from 'enterprise/components/visualizations/worldmap/WorldMapVisualization';

import PivotConfigGenerator from 'enterprise/logic/searchtypes/aggregation/PivotConfigGenerator';
import PivotHandler from 'enterprise/logic/searchtypes/pivot/PivotHandler';
import PivotTransformer from 'enterprise/logic/searchresulttransformers/PivotTransformer';

import Widget from 'enterprise/logic/widgets/Widget';
import AggregationWidget from 'enterprise/logic/aggregationbuilder/AggregationWidget';
import MessagesWidget from 'enterprise/logic/widgets/MessagesWidget';
import DataTable from 'enterprise/components/datatable/DataTable';
import FieldStatisticsHandler from 'enterprise/logic/fieldactions/FieldStatisticsHandler';
import ExcludeFromQueryHandler from 'enterprise/logic/valueactions/ExcludeFromQueryHandler';
import { isFunction } from 'enterprise/logic/aggregationbuilder/Series';
import AggregationControls from 'enterprise/components/aggregationbuilder/AggregationControls';
import EditMessageList from 'enterprise/components/widgets/EditMessageList';
import { ShowViewPage, NewSearchPage, ViewManagementPage } from 'enterprise/pages';

import ViewsLicenseCheck from 'enterprise/components/common/ViewsLicenseCheck';

import AddMessageCountActionHandler from 'enterprise/logic/fieldactions/AddMessageCountActionHandler';
import AddMessageTableActionHandler from 'enterprise/logic/fieldactions/AddMessageTableActionHandler';
import RemoveFromTableActionHandler from 'enterprise/logic/fieldactions/RemoveFromTableActionHandler';
import RemoveFromAllTablesActionHandler from 'enterprise/logic/fieldactions/RemoveFromAllTablesActionHandler';
import CreateCustomAggregation from 'enterprise/logic/creatoractions/CreateCustomAggregation';
import SelectExtractorType from 'enterprise/logic/valueactions/SelectExtractorType';

import VisualizationConfig from 'enterprise/logic/aggregationbuilder/visualizations/VisualizationConfig';
import WorldMapVisualizationConfig from 'enterprise/logic/aggregationbuilder/visualizations/WorldMapVisualizationConfig';
import BarVisualizationConfig from 'enterprise/logic/aggregationbuilder/visualizations/BarVisualizationConfig';

import ViewSharing from 'enterprise/logic/views/sharing/ViewSharing';
import AllUsersOfInstance from 'enterprise/logic/views/sharing/AllUsersOfInstance';
import SpecificRoles from 'enterprise/logic/views/sharing/SpecificRoles';
import SpecificUsers from 'enterprise/logic/views/sharing/SpecificUsers';

import UseInNewQueryHandler from 'enterprise/logic/valueactions/UseInNewQueryHandler';
import ShowDocumentsHandler from 'enterprise/logic/valueactions/ShowDocumentsHandler';
import HighlightValueHandler from 'enterprise/logic/valueactions/HighlightValueHandler';
import FieldNameCompletion from './components/searchbar/completions/FieldNameCompletion';
import OperatorCompletion from './components/searchbar/completions/OperatorCompletion';
import requirementsProvided from './hooks/RequirementsProvided';
import type { ValueActionHandlerConditionProps } from './logic/valueactions/ValueActionHandler';
import type { FieldActionHandlerConditionProps } from './logic/fieldactions/FieldActionHandler';

export const extendedSearchPath = '/extendedsearch';
export const viewsPath = '/views';
export const showViewsPath = `${viewsPath}/:viewId`;

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
    // search: { component: ExtendedSearchPage },
  },
  routes: [
    { path: extendedSearchPath, component: ViewsLicenseCheck(NewSearchPage), permissions: Permissions.ExtendedSearch.Use },
    { path: viewsPath, component: ViewsLicenseCheck(ViewManagementPage), permissions: Permissions.View.Use },
    { path: showViewsPath, component: ViewsLicenseCheck(ShowViewPage) },
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
