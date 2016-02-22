import packageJson from '../../package.json';
import {} from '../../config.js';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import MapVisualization from 'components/MapVisualization';
import MapPage from 'pages/MapPage';

PluginStore.register(new PluginManifest(packageJson, {
  routes: [
    { path: '/maps', component: MapPage },
  ],
  widgets: [
    {
      type: 'org.graylog.plugins.map.widget.strategy.MapWidgetStrategy',
      defaultHeight: 2,
      defaultWidth: 2,
      visualization: MapVisualization,
    },
  ],
}));
