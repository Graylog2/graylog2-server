export { default as FieldGraphs } from './FieldGraphs';
export { default as FieldQuickValues } from './FieldQuickValues';
export { default as FieldStatistics } from './FieldStatistics';
export { default as LegacyFieldGraph } from './LegacyFieldGraph';

import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import FieldStatistics from './FieldStatistics';
import FieldQuickValues from './FieldQuickValues';
import FieldGraphs from './FieldGraphs';

const pluginManifest = new PluginManifest({}, {
  fieldAnalyzers: [
    {
      refId: 'fieldStatisticsComponent',
      displayName: 'Statistics',
      component: FieldStatistics,
      displayPriority: 2,
    },
    {
      refId: 'fieldQuickValuesComponent',
      displayName: 'Quick values',
      component: FieldQuickValues,
      displayPriority: 1,
    },
    {
      refId: 'fieldGraphsComponent',
      displayName: 'Generate chart',
      component: FieldGraphs,
      displayPriority: 0,
    },
  ],
});

const registeredAnalyzers = PluginStore.exports('fieldAnalyzers').map(analyzer => analyzer.refId);
if (!pluginManifest.exports.fieldAnalyzers.every(analyzer => registeredAnalyzers.includes(analyzer.refId))) {
  PluginStore.register(pluginManifest);
}
