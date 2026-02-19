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
  set(key: string, value: unknown, storage: Storage = window.localStorage): void {
    storage.setItem(key, JSON.stringify(value));
  },

  get(key: string, storage: Storage = window.localStorage): any {
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

  delete(key: string, storage: Storage = window.localStorage): void {
    storage.removeItem(key);
  },

  sessionSet(key: string, value: unknown): void {
    return Store.set(key, value, window.sessionStorage);
  },

  sessionGet(key: string): any {
    return Store.get(key, window.sessionStorage);
  },

  sessionDelete(key: string): void {
    return Store.delete(key, window.sessionStorage);
  },
};

export default Store;
