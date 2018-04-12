import ExtendedSearchPage from 'enterprise/ExtendedSearchPage';
import ViewManagementPage from 'enterprise/ViewManagementPage';
import { AlertStatus, Histogram, MessageList, SearchSidebarWidget } from 'enterprise/components/widgets';
import GroupByHandler from 'enterprise/logic/searchtypes/GroupByHandler';
import { DateHistogramHandler, MessageListHandler } from 'enterprise/logic/searchtypes';
import DataTable from 'enterprise/components/datatable/DataTable';
import ChartActionHandler from 'enterprise/logic/fieldactions/ChartActionHandler';
import AggregateActionHandler from 'enterprise/logic/fieldactions/AggregateActionHandler';
import FieldHistogramTransformer from 'enterprise/logic/searchresulttransformers/FieldHistogramTransformer';
import AggregationHandler from 'enterprise/logic/searchtypes/AggregationHandler';
import AggregationTransformer from 'enterprise/logic/searchresulttransformers/AggregationTransformer';
import AggregationBuilder from 'enterprise/components/aggregationbuilder/AggregationBuilder';
import AggregationConfigGenerator from 'enterprise/logic/searchtypes/aggregation/AggregationConfigGenerator';
import BarVisualization from 'enterprise/components/visualizations/bar/BarVisualization';
import LineVisualization from 'enterprise/components/visualizations/line/LineVisualization';
import PieVisualization from 'enterprise/components/visualizations/pie/PieVisualization';
import ShowViewPage from 'enterprise/ShowViewPage';
import AddToTableActionHandler from 'enterprise/logic/fieldactions/AddToTableActionHandler';
import NewSearchPage from 'enterprise/NewSearchPage';

const extendedSearchPath = '/extendedsearch';
const viewsPath = '/views';
const showViewsPath = `${viewsPath}/:viewId`;

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
      searchResultTransformer: AggregationTransformer,
      searchTypes: AggregationConfigGenerator,
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
      type: 'date_histogram',
      handler: DateHistogramHandler,
      defaults: {
        interval: 'MINUTE',
      },
    },
    {
      type: 'group_by',
      handler: GroupByHandler,
      defaults: {
        limit: 150,
        operation: 'COUNT',
        order: 'ASC',
      },
    },
    {
      type: 'aggregation',
      handler: AggregationHandler,
      defaults: {
        groups: [],
      },
    },
  ],
  fieldActions: [
    {
      type: 'chart',
      title: 'Chart',
      handler: ChartActionHandler,
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
