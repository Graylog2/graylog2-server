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
export default class CombinedProviderMock {
  static defaultStore = {
    get: () => jest.fn(() => ({})),
    listen: jest.fn(),
  };

  static defaultActions = {};

  constructor(rules = {}, defaultStore = CombinedProviderMock.defaultStore, defaultActions = CombinedProviderMock.defaultActions) {
    this.rules = rules;
    this.store = defaultStore;
    this.actions = defaultActions;
  }

  get(name) {
    const result = this.rules[name] || {};
    if (!result[`${name}Store`]) {
      result[`${name}Store`] = this.store;
    }
    if (!result[`${name}Actions`]) {
      result[`${name}Actions`] = this.actions;
    }

    return result;
  }
}
