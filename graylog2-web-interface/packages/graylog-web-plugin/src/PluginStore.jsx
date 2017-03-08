class PluginStore {
  static register(plugin) {
    if (!window.plugins) {
      window.plugins = [];
    }
    window.plugins.push(plugin);
  }

  static unregister(plugin) {
    if (!window.plugins) {
      return;
    }

    window.plugins.forEach((item, idx) => {
      if (item.metadata && plugin.metadata && item.metadata.name === plugin.metadata.name) {
        window.plugins.splice(idx, 1);
      }
    });
  }

  static get() {
    if (!window.plugins) {
      window.plugins = [];
    }
    return window.plugins;
  }

  static exports(entity) {
    return [].concat.apply([], this.get()
      .map((plugin) => (plugin.exports && plugin.exports[entity] ? plugin.exports[entity] : []))
    );
  }
}

export default PluginStore;
