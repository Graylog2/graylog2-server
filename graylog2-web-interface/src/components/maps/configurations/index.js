import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import GeoIpResolverConfig from 'components/maps/configurations/GeoIpResolverConfig';

PluginStore.register(new PluginManifest({}, {
  systemConfigurations: [
    {
      component: GeoIpResolverConfig,
      configType: 'org.graylog.plugins.map.config.GeoIpResolverConfig',
    },
  ],
}));

