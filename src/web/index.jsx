import packageJson from '../../package.json';
import {} from '../../config.js';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import MapPage from 'pages/MapPage';

PluginStore.register(new PluginManifest(packageJson, {
  routes: [
    { path: '/maps', component: MapPage },
  ],
}));
