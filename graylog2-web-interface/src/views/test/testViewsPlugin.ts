import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import viewsBindings from 'views/bindings';

const testViewsPlugin = new PluginManifest({}, viewsBindings);

export const loadViewsPlugin = () => PluginStore.register(testViewsPlugin);
export const unloadViewsPlugin = () => PluginStore.unregister(testViewsPlugin);
