import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import {
  GraphVisualization,
  HistogramVisualization,
  NumericVisualization,
  QuickValuesVisualization,
  StackedGraphVisualization } from 'components/visualizations';

PluginStore.register(new PluginManifest({}, {
  widgets: [
    {
      type: 'SEARCH_RESULT_COUNT',
      defaultHeight: 1,
      defaultWidth: 1,
      visualization: NumericVisualization,
    },
    {
      type: 'STREAM_SEARCH_RESULT_COUNT',
      defaultHeight: 1,
      defaultWidth: 1,
      visualization: NumericVisualization,
    },
    {
      type: 'STATS_COUNT',
      defaultHeight: 1,
      defaultWidth: 1,
      visualization: NumericVisualization,
    },
    {
      type: 'SEARCH_RESULT_CHART',
      defaultHeight: 1,
      defaultWidth: 1,
      visualization: HistogramVisualization,
    },
    {
      type: 'QUICKVALUES',
      defaultHeight: 1,
      defaultWidth: 2,
      visualization: QuickValuesVisualization,
    },
    {
      type: 'FIELD_CHART',
      defaultHeight: 1,
      defaultWidth: 2,
      visualization: GraphVisualization,
    },
    {
      type: 'STACKED_CHART',
      defaultHeight: 1,
      defaultWidth: 2,
      visualization: StackedGraphVisualization,
    },
  ],
}));
