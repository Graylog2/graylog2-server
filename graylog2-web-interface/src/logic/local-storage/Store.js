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

const Store = {
  set(key, value, storage = window.localStorage) {
    storage.setItem(key, JSON.stringify(value));
  },

  get(key, storage = window.localStorage) {
    const value = storage.getItem(key);

    if (value === undefined || value === null) {
      return undefined;
    }

    try {
      return JSON.parse(value);
    } catch (e) {
      return value;
    }
  },

  delete(key, storage = window.localStorage) {
    storage.removeItem(key);
  },

  sessionSet(key, value) {
    Store.set(key, value, window.sessionStorage);
  },

  sessionGet(key) {
    Store.get(key, window.sessionStorage);
  },

  sessionDelete(key) {
    Store.delete(key, window.sessionStorage);
  },
};

export default Store;
