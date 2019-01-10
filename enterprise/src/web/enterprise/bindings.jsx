import { MessageListHandler } from 'enterprise/logic/searchtypes';

import AddToTableActionHandler from 'enterprise/logic/fieldactions/AddToTableActionHandler';
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

import { MessageList } from 'enterprise/components/widgets';
import Widget from 'enterprise/logic/widgets/Widget';
import AggregationWidget from 'enterprise/logic/aggregationbuilder/AggregationWidget';
import MessagesWidget from 'enterprise/logic/widgets/MessagesWidget';
import DataTable from 'enterprise/components/datatable/DataTable';
import FieldStatisticsHandler from 'enterprise/logic/fieldactions/FieldStatisticsHandler';
import ExcludeFromQueryHandler from 'enterprise/logic/valueactions/ExcludeFromQueryHandler';
import { isFunction } from 'enterprise/components/visualizations/Series';
import AggregationControls from 'enterprise/components/aggregationbuilder/AggregationControls';
import EditMessageList from 'enterprise/components/widgets/EditMessageList';
import {
  ShowViewPage,
  NewSearchPage,
  ViewManagementPage,
} from 'enterprise/pages';


import ViewsLicenseCheck from 'enterprise/components/common/ViewsLicenseCheck';

import AddMessageCountActionHandler from 'enterprise/logic/fieldactions/AddMessageCountActionHandler';
import AddMessageTableActionHandler from 'enterprise/logic/fieldactions/AddMessageTableActionHandler';
import CreateParameterDialog from 'enterprise/logic/creatoractions/CreateParameterDialog';
import CreateCustomAggregation from 'enterprise/logic/creatoractions/CreateCustomAggregation';
import ExecuteViewWithValue from 'enterprise/components/views/ExecuteViewWithValue';

import VisualizationConfig from 'enterprise/logic/aggregationbuilder/visualizations/VisualizationConfig';
import WorldMapVisualizationConfig from 'enterprise/logic/aggregationbuilder/visualizations/WorldMapVisualizationConfig';

import ViewSharing from 'enterprise/logic/views/sharing/ViewSharing';
import AllUsersOfInstance from 'enterprise/logic/views/sharing/AllUsersOfInstance';
import SpecificRoles from 'enterprise/logic/views/sharing/SpecificRoles';
import SpecificUsers from 'enterprise/logic/views/sharing/SpecificUsers';

import SortConfig from 'enterprise/logic/aggregationbuilder/SortConfig';
import PivotSortConfig from 'enterprise/logic/aggregationbuilder/PivotSortConfig';
import SeriesSortConfig from 'enterprise/logic/aggregationbuilder/SeriesSortConfig';

import * as Permissions from './Permissions';

const extendedSearchPath = '/extendedsearch';
const viewsPath = '/views';
const showViewsPath = `${viewsPath}/:viewId`;

Widget.registerSubtype(AggregationWidget.type, AggregationWidget);
Widget.registerSubtype(MessagesWidget.type, MessagesWidget);
VisualizationConfig.registerSubtype(WorldMapVisualization.type, WorldMapVisualizationConfig);

ViewSharing.registerSubtype(AllUsersOfInstance.Type, AllUsersOfInstance);
ViewSharing.registerSubtype(SpecificRoles.Type, SpecificRoles);
ViewSharing.registerSubtype(SpecificUsers.Type, SpecificUsers);

SortConfig.registerSubtype(PivotSortConfig.type, PivotSortConfig);
SortConfig.registerSubtype(SeriesSortConfig.type, SeriesSortConfig);

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
      searchResultTransformer: data => data[0],
      searchTypes: () => [{ type: 'messages' }],
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
      titleGenerator: (widget) => {
        if (widget.config.rowPivots.length > 0) {
          return `Aggregating ${widget.config.series.map(s => s.effectiveName)} by ${widget.config.rowPivots.map(({ field }) => field).join(', ')}`;
        }
        if (widget.config.series.length > 0) {
          return `Aggregating ${widget.config.series.map(s => s.effectiveName)}`;
        }
        return 'Empty Aggregation';
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
      condition: ({ type }) => type.isNumeric(),
    },
    {
      type: 'aggregate',
      title: 'Aggregate',
      handler: AggregateActionHandler,
      condition: ({ type }) => !type.isCompound(),
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
    },
    {
      type: 'new-query',
      title: 'Use in new query',
    },
  ],
  valueActions: [
    {
      type: 'exclude',
      title: 'Exclude from results',
      handler: new ExcludeFromQueryHandler().handle,
      condition: ({ field }) => !isFunction(field),
    },
    {
      type: 'add-to-query',
      title: 'Add to query',
      handler: new AddToQueryHandler().handle,
      condition: ({ field }) => !isFunction(field),
    },
    {
      type: 'execute-view-with-value',
      title: 'Insert into view',
      component: ExecuteViewWithValue,
    }
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
    {
      type: 'generic',
      title: 'Parameter',
      component: CreateParameterDialog,
    },
  ],
};
