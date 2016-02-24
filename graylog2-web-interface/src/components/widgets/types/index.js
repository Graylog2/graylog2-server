import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import {
  GraphVisualization,
  HistogramVisualization,
  NumericVisualization,
  QuickValuesVisualization,
  StackedGraphVisualization } from 'components/visualizations';
import {
  CountWidgetEditConfiguration,
  FieldChartWidgetConfiguration,
  QuickValuesWidgetEditConfiguration,
  StackedChartWidgetConfiguration,
  StatisticalCountWidgetEditConfiguration } from 'components/widgets/configurations';

PluginStore.register(new PluginManifest({}, {
  widgets: [
    {
      type: 'SEARCH_RESULT_COUNT',
      readableType: 'Search result count',
      defaultHeight: 1,
      defaultWidth: 1,
      visualization: NumericVisualization,
      editConfiguration: CountWidgetEditConfiguration,
    },
    {
      type: 'STREAM_SEARCH_RESULT_COUNT',
      readableType: 'Stream search result count',
      defaultHeight: 1,
      defaultWidth: 1,
      visualization: NumericVisualization,
      editConfiguration: CountWidgetEditConfiguration,
    },
    {
      type: 'STATS_COUNT',
      readableType: 'Statistical value',
      defaultHeight: 1,
      defaultWidth: 1,
      visualization: NumericVisualization,
      editConfiguration: StatisticalCountWidgetEditConfiguration,
    },
    {
      type: 'SEARCH_RESULT_CHART',
      readableType: 'Search result graph',
      defaultHeight: 1,
      defaultWidth: 1,
      visualization: HistogramVisualization,
    },
    {
      type: 'QUICKVALUES',
      readableType: 'Quick values',
      defaultHeight: 3,
      defaultWidth: 1,
      visualization: QuickValuesVisualization,
      editConfiguration: QuickValuesWidgetEditConfiguration,
    },
    {
      type: 'FIELD_CHART',
      readableType: 'Field graph',
      defaultHeight: 1,
      defaultWidth: 2,
      visualization: GraphVisualization,
      editConfiguration: FieldChartWidgetConfiguration,
    },
    {
      type: 'STACKED_CHART',
      readableType: 'Stacked graph',
      defaultHeight: 1,
      defaultWidth: 2,
      visualization: StackedGraphVisualization,
      editConfiguration: StackedChartWidgetConfiguration,
    },
  ],
}));
