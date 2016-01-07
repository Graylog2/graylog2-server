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
  
  static exports(export) {
    return [].concat.apply([], get()
      .map((plugin) => plugin.exports && plugin.exports[export] ? plugin.exports[export] : []);
  }
}

export default PluginStore;
