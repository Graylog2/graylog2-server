// eslint-disable-next-line no-unused-vars
import webpackEntry from 'webpack-entry';

import packageJson from '../../package.json';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import MapVisualization from 'components/MapVisualization';
import FieldAnalyzerMapComponent from 'components/FieldAnalyzerMapComponent';
import GeoIpResolverConfig from 'components/GeoIpResolverConfig';

PluginStore.register(new PluginManifest(packageJson, {
  widgets: [
    {
      type: 'org.graylog.plugins.map.widget.strategy.MapWidgetStrategy',
      displayName: 'Map',
      defaultHeight: 2,
      defaultWidth: 2,
      visualizationComponent: MapVisualization,
    },
  ],
  fieldAnalyzers: [
    {
      refId: 'fieldAnalyzerMapComponent',
      displayName: 'World Map',
      component: FieldAnalyzerMapComponent,
      displayPriority: 100,
    },
  ],
  systemConfigurations: [
    {
      component: GeoIpResolverConfig,
      configType: 'org.graylog.plugins.map.config.GeoIpResolverConfig',
    },
  ],
}));
