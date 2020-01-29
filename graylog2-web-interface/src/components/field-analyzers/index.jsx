import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
const pluginManifest = new PluginManifest({}, {
  fieldAnalyzers: [
  ],
});

const registeredAnalyzers = PluginStore.exports('fieldAnalyzers').map(analyzer => analyzer.refId);
if (!pluginManifest.exports.fieldAnalyzers.every(analyzer => registeredAnalyzers.includes(analyzer.refId))) {
  PluginStore.register(pluginManifest);
}
