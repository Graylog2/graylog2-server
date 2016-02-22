export { default as FieldGraphs } from './FieldGraphs';
export { default as FieldQuickValues } from './FieldQuickValues';
export { default as FieldStatistics } from './FieldStatistics';
export { default as LegacyFieldGraph } from './LegacyFieldGraph';

import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import FieldStatistics from './FieldStatistics';
import FieldQuickValues from './FieldQuickValues';
import FieldGraphs from './FieldGraphs';

PluginStore.register(new PluginManifest({}, {
  fieldAnalyzers: [
    {
      refId: 'fieldStatisticsComponent',
      displayName: 'Statistics',
      component: FieldStatistics,
      widgetType: 'STATS_COUNT',
      displayPriority: 100,
    },
    {
      refId: 'fieldQuickValuesComponent',
      displayName: 'Quick values',
      component: FieldQuickValues,
      widgetType: 'QUICKVALUES',
      displayPriority: 100,
    },
    {
      refId: 'fieldGraphsComponent',
      displayName: 'Generate chart',
      component: FieldGraphs,
      widgetType: null,
      displayPriority: 50,
    },
  ],
}));
