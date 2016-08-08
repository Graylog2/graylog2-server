import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import {
  GraphVisualization,
  HistogramVisualization,
  NumericVisualization,
  QuickValuesVisualization,
  StackedGraphVisualization } from 'components/visualizations';
import {
  CountWidgetCreateConfiguration,
  CountWidgetEditConfiguration,
  FieldChartWidgetConfiguration,
  QuickValuesWidgetCreateConfiguration,
  QuickValuesWidgetEditConfiguration,
  StackedChartWidgetConfiguration,
  StatisticalCountWidgetCreateConfiguration,
  StatisticalCountWidgetEditConfiguration } from 'components/widgets/configurations';

PluginStore.register(new PluginManifest({}, {
  widgets: [
    {
      type: 'SEARCH_RESULT_COUNT',
      displayName: 'Search result count',
      defaultHeight: 1,
      defaultWidth: 1,
      visualizationComponent: NumericVisualization,
      configurationCreateComponent: CountWidgetCreateConfiguration,
      configurationEditComponent: CountWidgetEditConfiguration,
    },
    {
      type: 'STREAM_SEARCH_RESULT_COUNT',
      displayName: 'Stream search result count',
      defaultHeight: 1,
      defaultWidth: 1,
      visualizationComponent: NumericVisualization,
      configurationCreateComponent: CountWidgetCreateConfiguration,
      configurationEditComponent: CountWidgetEditConfiguration,
    },
    {
      type: 'STATS_COUNT',
      displayName: 'Statistical value',
      defaultHeight: 1,
      defaultWidth: 1,
      visualizationComponent: NumericVisualization,
      configurationCreateComponent: StatisticalCountWidgetCreateConfiguration,
      configurationEditComponent: StatisticalCountWidgetEditConfiguration,
    },
    {
      type: 'SEARCH_RESULT_CHART',
      displayName: 'Search result graph',
      defaultHeight: 1,
      defaultWidth: 2,
      visualizationComponent: HistogramVisualization,
    },
    {
      type: 'QUICKVALUES',
      displayName: 'Quick values',
      defaultHeight: 3,
      defaultWidth: 1,
      visualizationComponent: QuickValuesVisualization,
      configurationCreateComponent: QuickValuesWidgetCreateConfiguration,
      configurationEditComponent: QuickValuesWidgetEditConfiguration,
    },
    {
      type: 'FIELD_CHART',
      displayName: 'Field graph',
      defaultHeight: 1,
      defaultWidth: 2,
      visualizationComponent: GraphVisualization,
      configurationEditComponent: FieldChartWidgetConfiguration,
    },
    {
      type: 'STACKED_CHART',
      displayName: 'Stacked graph',
      defaultHeight: 1,
      defaultWidth: 2,
      visualizationComponent: StackedGraphVisualization,
      configurationEditComponent: StackedChartWidgetConfiguration,
    },
  ],
}));
