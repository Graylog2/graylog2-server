import ViewManagementPage from 'enterprise/ViewManagementPage';
import { AlertStatus, MessageList } from 'enterprise/components/widgets';
import { MessageListHandler } from 'enterprise/logic/searchtypes';
import DataTable from 'enterprise/components/datatable/DataTable';
import ChartActionHandler from 'enterprise/logic/fieldactions/ChartActionHandler';
import AggregateActionHandler from 'enterprise/logic/fieldactions/AggregateActionHandler';
import AggregationBuilder from 'enterprise/components/aggregationbuilder/AggregationBuilder';
import BarVisualization from 'enterprise/components/visualizations/bar/BarVisualization';
import LineVisualization from 'enterprise/components/visualizations/line/LineVisualization';
import PieVisualization from 'enterprise/components/visualizations/pie/PieVisualization';
import ShowViewPage from 'enterprise/ShowViewPage';
import AddToTableActionHandler from 'enterprise/logic/fieldactions/AddToTableActionHandler';
import NewSearchPage from 'enterprise/NewSearchPage';
import Widget from './logic/widgets/Widget';
import AggregationWidget from './logic/aggregationbuilder/AggregationWidget';
import MessagesWidget from './logic/widgets/MessagesWidget';
import PivotConfigGenerator from './logic/searchtypes/aggregation/PivotConfigGenerator';
import PivotHandler from './logic/searchtypes/pivot/PivotHandler';
import PivotTransformer from './logic/searchresulttransformers/PivotTransformer';

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
    { path: extendedSearchPath, component: NewSearchPage },
    { path: viewsPath, component: ViewManagementPage },
    { path: showViewsPath, component: ShowViewPage },
  ],
  navigation: [
    // Disabling navigation for extended search for now to avoid confusing alpha testers.
    // TODO: Disable Views and ExtendedSearch menu items again for the next alpha release!
    { path: extendedSearchPath, description: 'Extended Search' },
    { path: viewsPath, description: 'Views' },
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
      titleGenerator: widget => `Aggregating ${widget.config.series} by ${widget.config.rowPivots.map(({ field }) => field).join(', ')}`,
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
    },
    {
      type: 'search',
      title: 'Search for values like this',
    },
  ],
  visualizationTypes: [
    {
      type: 'bar',
      displayName: 'Bar Chart',
      component: BarVisualization,
    },
    {
      type: 'line',
      displayName: 'Line Chart',
      component: LineVisualization,
    },
    {
      type: 'pie',
      displayName: 'Pie Chart',
      component: PieVisualization,
    },
    {
      type: 'table',
      displayName: 'Data Table',
      component: DataTable,
    },
  ],
};
