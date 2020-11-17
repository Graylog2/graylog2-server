/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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
