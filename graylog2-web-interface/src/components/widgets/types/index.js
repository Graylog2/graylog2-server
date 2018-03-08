import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import {
  GraphVisualization,
  HistogramVisualization,
  NumericVisualization,
  QuickValuesVisualization,
  QuickValuesHistogramVisualization,
  StackedGraphVisualization } from 'components/visualizations';
import {
  CountWidgetCreateConfiguration,
  CountWidgetEditConfiguration,
  FieldChartWidgetConfiguration,
  QuickValuesWidgetCreateConfiguration,
  QuickValuesWidgetEditConfiguration,
  QuickValuesHistogramWidgetCreateConfiguration,
  QuickValuesHistogramWidgetEditConfiguration,
  StackedChartWidgetConfiguration,
  StatisticalCountWidgetCreateConfiguration,
  StatisticalCountWidgetEditConfiguration } from 'components/widgets/configurations';
import {} from 'components/maps/widgets';

PluginStore.register(new PluginManifest({}, {
  widgets: [
    {
      type: 'SEARCH_RESULT_COUNT',
      displayName: 'Search result count',
      defaultHeight: 2,
      defaultWidth: 2,
      visualizationComponent: NumericVisualization,
      configurationCreateComponent: CountWidgetCreateConfiguration,
      configurationEditComponent: CountWidgetEditConfiguration,
    },
    {
      type: 'STREAM_SEARCH_RESULT_COUNT',
      displayName: 'Stream search result count',
      defaultHeight: 2,
      defaultWidth: 2,
      visualizationComponent: NumericVisualization,
      configurationCreateComponent: CountWidgetCreateConfiguration,
      configurationEditComponent: CountWidgetEditConfiguration,
    },
    {
      type: 'STATS_COUNT',
      displayName: 'Statistical value',
      defaultHeight: 2,
      defaultWidth: 2,
      visualizationComponent: NumericVisualization,
      configurationCreateComponent: StatisticalCountWidgetCreateConfiguration,
      configurationEditComponent: StatisticalCountWidgetEditConfiguration,
    },
    {
      type: 'SEARCH_RESULT_CHART',
      displayName: 'Search result graph',
      defaultHeight: 2,
      defaultWidth: 4,
      visualizationComponent: HistogramVisualization,
    },
    {
      type: 'QUICKVALUES',
      displayName: 'Quick values',
      defaultHeight: 6,
      defaultWidth: 2,
      visualizationComponent: QuickValuesVisualization,
      configurationCreateComponent: QuickValuesWidgetCreateConfiguration,
      configurationEditComponent: QuickValuesWidgetEditConfiguration,
    },
    {
      type: 'QUICKVALUES_HISTOGRAM',
      displayName: 'Quick values histogram',
      defaultHeight: 2,
      defaultWidth: 4,
      visualizationComponent: QuickValuesHistogramVisualization,
      configurationCreateComponent: QuickValuesHistogramWidgetCreateConfiguration,
      configurationEditComponent: QuickValuesHistogramWidgetEditConfiguration,
    },
    {
      type: 'FIELD_CHART',
      displayName: 'Field graph',
      defaultHeight: 2,
      defaultWidth: 4,
      visualizationComponent: GraphVisualization,
      configurationEditComponent: FieldChartWidgetConfiguration,
    },
    {
      type: 'STACKED_CHART',
      displayName: 'Stacked graph',
      defaultHeight: 2,
      defaultWidth: 4,
      visualizationComponent: StackedGraphVisualization,
      configurationEditComponent: StackedChartWidgetConfiguration,
    },
  ],
}));
