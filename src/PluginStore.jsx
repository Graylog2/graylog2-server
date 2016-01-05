class PluginStore {
  static register(plugin) {
    if (!window.plugins) {
      window.plugins = [];
    }
    window.plugins.push(plugin);
  }

  static get() {
    if (!window.plugins) {
      window.plugins = [];
    }
    return window.plugins;
  }
}

export default PluginStore;
