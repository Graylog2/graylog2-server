import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import {
  GraphVisualization,
  HistogramVisualization,
  NumericVisualization,
  QuickValuesVisualization,
  StackedGraphVisualization } from 'components/visualizations';
import {
  CountWidgetConfiguration,
  FieldChartWidgetConfiguration,
  QuickValuesWidgetConfiguration,
  StackedChartWidgetConfiguration,
  StatisticalCountWidgetConfiguration } from 'components/widgets/configurations';

PluginStore.register(new PluginManifest({}, {
  widgets: [
    {
      type: 'SEARCH_RESULT_COUNT',
      readable_type: 'Search result count',
      defaultHeight: 1,
      defaultWidth: 1,
      visualization: NumericVisualization,
      configuration: CountWidgetConfiguration,
    },
    {
      type: 'STREAM_SEARCH_RESULT_COUNT',
      readable_type: 'Stream search result count',
      defaultHeight: 1,
      defaultWidth: 1,
      visualization: NumericVisualization,
      configuration: CountWidgetConfiguration,
    },
    {
      type: 'STATS_COUNT',
      readable_type: 'Statistical value',
      defaultHeight: 1,
      defaultWidth: 1,
      visualization: NumericVisualization,
      configuration: StatisticalCountWidgetConfiguration,
    },
    {
      type: 'SEARCH_RESULT_CHART',
      readable_type: 'Search result graph',
      defaultHeight: 1,
      defaultWidth: 1,
      visualization: HistogramVisualization,
    },
    {
      type: 'QUICKVALUES',
      readable_type: 'Quick values',
      defaultHeight: 1,
      defaultWidth: 2,
      visualization: QuickValuesVisualization,
      configuration: QuickValuesWidgetConfiguration,
    },
    {
      type: 'FIELD_CHART',
      readable_type: 'Field graph',
      defaultHeight: 1,
      defaultWidth: 2,
      visualization: GraphVisualization,
      configuration: FieldChartWidgetConfiguration,
    },
    {
      type: 'STACKED_CHART',
      readable_type: 'Stacked graph',
      defaultHeight: 1,
      defaultWidth: 2,
      visualization: StackedGraphVisualization,
      configuration: StackedChartWidgetConfiguration,
    },
  ],
}));
