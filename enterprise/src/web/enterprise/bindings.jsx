import { MessageListHandler } from 'enterprise/logic/searchtypes';

import AddToTableActionHandler from 'enterprise/logic/fieldactions/AddToTableActionHandler';
import AddToQueryHandler from 'enterprise/logic/valueactions/AddToQueryHandler';
import AggregateActionHandler from 'enterprise/logic/fieldactions/AggregateActionHandler';
import ChartActionHandler from 'enterprise/logic/fieldactions/ChartActionHandler';

import AggregationBuilder from 'enterprise/components/aggregationbuilder/AggregationBuilder';

import BarVisualization from 'enterprise/components/visualizations/bar/BarVisualization';
import LineVisualization from 'enterprise/components/visualizations/line/LineVisualization';
import PieVisualization from 'enterprise/components/visualizations/pie/PieVisualization';

import ShowViewPage from 'enterprise/ShowViewPage';
import NewSearchPage from 'enterprise/NewSearchPage';
import PivotConfigGenerator from './logic/searchtypes/aggregation/PivotConfigGenerator';
import PivotHandler from './logic/searchtypes/pivot/PivotHandler';
import PivotTransformer from './logic/searchresulttransformers/PivotTransformer';
import ViewManagementPage from 'enterprise/ViewManagementPage';

import { AlertStatus, MessageList } from 'enterprise/components/widgets';
import Widget from 'enterprise/logic/widgets/Widget';
import AggregationWidget from 'enterprise/logic/aggregationbuilder/AggregationWidget';
import MessagesWidget from 'enterprise/logic/widgets/MessagesWidget';
import DataTable from 'enterprise/components/datatable/DataTable';
import WorldMapVisualization from './components/visualizations/worldmap/WorldMapVisualization';
import FieldStatisticsHandler from './logic/fieldactions/FieldStatisticsHandler';
import NumberVisualization from './components/visualizations/number/NumberVisualization';
import ExcludeFromQueryHandler from './logic/valueactions/ExcludeFromQueryHandler';
import { isFunction } from './components/visualizations/Series';
import * as Permissions from './Permissions';

const extendedSearchPath = '/extendedsearch';
const viewsPath = '/views';
const showViewsPath = `${viewsPath}/:viewId`;

Widget.registerSubtype(AggregationWidget.type, AggregationWidget);
Widget.registerSubtype(MessagesWidget.type, MessagesWidget);

export default {
  pages: {
    //search: { component: ExtendedSearchPage },

  },
  routes: [
    { path: extendedSearchPath, component: NewSearchPage, permissions: Permissions.ExtendedSearch.Use },
    { path: viewsPath, component: ViewManagementPage, permissions: Permissions.View.Use },
    { path: showViewsPath, component: ShowViewPage },
  ],
  navigation: [
    // Disabling navigation for extended search for now to avoid confusing alpha testers.
    // TODO: Disable Views and ExtendedSearch menu items again for the next alpha release!
    { path: extendedSearchPath, description: 'Extended Search', permissions: Permissions.ExtendedSearch.Use },
    { path: viewsPath, description: 'Views', permissions: Permissions.View.Use },
  ],
  enterpriseWidgets: [
    {
      type: 'MESSAGES',
      displayName: 'Message List',
      defaultHeight: 5,
      defaultWidth: 6,
      visualizationComponent: MessageList,
      searchResultTransformer: data => data[0],
      searchTypes: () => [{ type: 'messages' }],
    },
    {
      type: 'AGGREGATION',
      displayName: 'Results',
      defaultHeight: 4,
      defaultWidth: 4,
      visualizationComponent: AggregationBuilder,
      searchResultTransformer: PivotTransformer,
      searchTypes: PivotConfigGenerator,
      titleGenerator: (widget) => {
        if (widget.config.rowPivots.length > 0) {
          return `Aggregating ${widget.config.series} by ${widget.config.rowPivots.map(({ field }) => field).join(', ')}`;
        }
        return `Aggregating ${widget.config.series}`;
      },
    },
    {
      type: 'ALERT_STATUS',
      displayName: 'Alert Status',
      defaultHeight: 2,
      defaultWidth: 2,
      visualizationComponent: AlertStatus,
      searchResultTransformer: data => data,
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
  ],
};
