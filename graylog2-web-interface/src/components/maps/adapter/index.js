import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import MaxmindAdapterFieldSet from 'components/maps/adapter/MaxmindAdapterFieldSet';
import MaxmindAdapterSummary from 'components/maps/adapter/MaxmindAdapterSummary';
import MaxmindAdapterDocumentation from 'components/maps/adapter/MaxmindAdapterDocumentation';

PluginStore.register(new PluginManifest({}, {
  lookupTableAdapters: [
    {
      type: 'maxmind_geoip',
      displayName: 'Geo IP - MaxMind\u2122 Databases',
      formComponent: MaxmindAdapterFieldSet,
      summaryComponent: MaxmindAdapterSummary,
      documentationComponent: MaxmindAdapterDocumentation,
    },
  ],
}));

