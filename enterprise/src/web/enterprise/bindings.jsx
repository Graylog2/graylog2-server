import ExtendedSearchPage from 'enterprise/ExtendedSearchPage';
import ViewManagementPage from 'enterprise/ViewManagementPage';
import { Histogram, MessageList, SearchSidebarWidget } from 'enterprise/components/widgets';
import GroupByHandler from 'enterprise/logic/searchtypes/GroupByHandler';
import { DateHistogramHandler, MessageListHandler } from 'enterprise/logic/searchtypes';
import DataTable from 'enterprise/components/datatable/DataTable';
import ChartActionHandler from 'enterprise/logic/fieldactions/ChartActionHandler';
import AggregateActionHandler from 'enterprise/logic/fieldactions/AggregateActionHandler';
import FieldHistogramTransformer from 'enterprise/logic/searchresulttransformers/FieldHistogramTransformer';
import AggregationHandler from './logic/searchtypes/AggregationHandler';
import AggregationTransformer from './logic/searchresulttransformers/AggregationTransformer';
import AggregationBuilder from './components/aggregationbuilder/AggregationBuilder';
import AggregationConfigGenerator from './logic/searchtypes/aggregation/AggregationConfigGenerator';
import BarVisualization from './components/visualizations/bar/BarVisualization';
import LineVisualization from './components/visualizations/line/LineVisualization';
import PieVisualization from './components/visualizations/pie/PieVisualization';

const extendedSearchPath = '/extendedsearch';
const viewsPath = '/views';

export default {
  pages: {
    // search: { component: ExtendedSearchPage },
  },
  routes: [
    { path: extendedSearchPath, component: ExtendedSearchPage },
    { path: viewsPath, component: ViewManagementPage },
  ],
  navigation: [
    // Disabling navigation for extended search for now to avoid confusing alpha testers.
    // { path: extendedSearchPath, description: 'Extended Search' },
    // { path: viewsPath, description: 'Views' },
  ],
  enterpriseWidgets: [
    {
      type: 'MESSAGES',
      displayName: 'Message List',
      defaultHeight: 4,
      defaultWidth: 4,
      visualizationComponent: MessageList,
      searchResultTransformer: data => data[0],
      searchTypes: () => [{ type: 'messages' }],
    },
    {
      type: 'SEARCH_RESULT_CHART2',
      displayName: 'Search result graph',
      defaultHeight: 2,
      defaultWidth: 4,
      visualizationComponent: Histogram,
      searchResultTransformer: data => data.find(d => d && d.type && d.type.toLocaleUpperCase() === 'DATE_HISTOGRAM'),
      searchTypes: () => [{ type: 'messages' }, { type: 'date_histogram' }],
    },
    {
      type: 'SEARCH_SIDEBAR',
      displayName: 'Search result',
      defaultHeight: 3,
      defaultWidth: 1,
      visualizationComponent: SearchSidebarWidget,
    },
    {
      type: 'FIELD_HISTOGRAM',
      displayName: 'Field Histogram',
      defaultHeight: 1,
      defaultWidth: 2,
      visualizationComponent: Histogram,
      searchResultTransformer: FieldHistogramTransformer,
      searchTypes: config => [{
        type: 'aggregation',
        config: {
          groups: [{
            type: 'time',
            field: 'timestamp',
            interval: '1m',
            metrics: [{
              type: 'sum',
              field: config.field,
            }],
          }],
        },
      }],
    },
    {
      type: 'DATATABLE',
      displayName: 'Results',
      defaultHeight: 3,
      defaultWidth: 2,
      visualizationComponent: DataTable,
      searchResultTransformer: data => data,
      searchTypes: () => [],
    },
    {
      type: 'AGGREGATION',
      displayName: 'Results',
      defaultHeight: 4,
      defaultWidth: 4,
      visualizationComponent: AggregationBuilder,
      searchResultTransformer: AggregationTransformer,
      searchTypes: AggregationConfigGenerator,
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
