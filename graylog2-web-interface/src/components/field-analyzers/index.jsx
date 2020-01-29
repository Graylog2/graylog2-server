import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import FieldAnalyzerMapComponent from 'components/maps/field-analyzers/FieldAnalyzerMapComponent';
import FieldStatistics from './FieldStatistics';

export { default as FieldStatistics } from './FieldStatistics';

const pluginManifest = new PluginManifest({}, {
  fieldAnalyzers: [
    {
      refId: 'fieldStatisticsComponent',
      displayName: 'Statistics',
      component: FieldStatistics,
      displayPriority: 2,
    },
    {
      refId: 'fieldAnalyzerMapComponent',
      displayName: 'World Map',
      component: FieldAnalyzerMapComponent,
      displayPriority: 100,
    },
  ],
});

const registeredAnalyzers = PluginStore.exports('fieldAnalyzers').map(analyzer => analyzer.refId);
if (!pluginManifest.exports.fieldAnalyzers.every(analyzer => registeredAnalyzers.includes(analyzer.refId))) {
  PluginStore.register(pluginManifest);
}
