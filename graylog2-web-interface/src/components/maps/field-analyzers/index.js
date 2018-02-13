import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import FieldAnalyzerMapComponent from 'components/maps/field-analyzers/FieldAnalyzerMapComponent';

PluginStore.register(new PluginManifest({}, {
  fieldAnalyzers: [
    {
      refId: 'fieldAnalyzerMapComponent',
      displayName: 'World Map',
      component: FieldAnalyzerMapComponent,
      displayPriority: 100,
    },
  ],
}));

