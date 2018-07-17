import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import MapVisualization from 'components/maps/widgets/MapVisualization';

PluginStore.register(new PluginManifest({}, {
  widgets: [
    {
      type: 'org.graylog.plugins.map.widget.strategy.MapWidgetStrategy',
      displayName: 'Map',
      defaultHeight: 5,
      defaultWidth: 3,
      visualizationComponent: MapVisualization,
    },
  ],
}));

