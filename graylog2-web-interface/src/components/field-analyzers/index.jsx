import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import FieldAnalyzerMapComponent from 'components/maps/field-analyzers/FieldAnalyzerMapComponent';
const pluginManifest = new PluginManifest({}, {
  fieldAnalyzers: [
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
