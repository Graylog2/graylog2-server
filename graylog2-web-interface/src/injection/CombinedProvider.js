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
class CombinedProvider {
  constructor() {
    this.actions = {};
    this.stores = {};
  }

  registerAction(key, func) {
    if (this.actions[key]) {
      throw new Error(`Unable to register actions for '${key}', already registered: ${this.actions[key]}`);
    }

    this.actions[key] = func;
  }

  registerStore(key, func) {
    if (this.stores[key]) {
      throw new Error(`Unable to register store for '${key}', already registered: ${this.stores[key]}`);
    }

    this.stores[key] = func;
  }

  get(name) {
    const result = {};

    if (this.stores[name]) {
      const _result = this.stores[name]();

      result[`${name}Store`] = _result && _result.default ? _result.default : _result;
    }

    if (this.actions[name]) {
      const _result = this.actions[name]();

      result[`${name}Actions`] = _result && _result.default ? _result.default : _result;
    }

    return result;
  }
}

if (typeof window.combinedProvider === 'undefined') {
  window.combinedProvider = new CombinedProvider();
}

export default window.combinedProvider;
