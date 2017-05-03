// eslint-disable-next-line no-unused-vars
import webpackEntry from 'webpack-entry';

import packageJson from '../../package.json';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import MapVisualization from 'components/MapVisualization';
import FieldAnalyzerMapComponent from 'components/FieldAnalyzerMapComponent';
import GeoIpResolverConfig from 'components/GeoIpResolverConfig';
import MaxmindAdapterFieldSet from 'components/adapter/MaxmindAdapterFieldSet';
import MaxmindAdapterSummary from 'components/adapter/MaxmindAdapterSummary';
import MaxmindAdapterDocumentation from 'components/adapter/MaxmindAdapterDocumentation';

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
  lookupTableAdapters: [
    {
      type: 'maxmind_geoip',
      displayName: 'Geo IP - MaxMind\u2122 Databases',
      formComponent: MaxmindAdapterFieldSet,
      summaryComponent: MaxmindAdapterSummary,
      documentationComponent: MaxmindAdapterDocumentation,
    },
  ]
}));
