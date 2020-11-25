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
type MockMethod = string | [string, Function];

export default function MockStore(...args: Array<MockMethod>) {
  const store = {
    // eslint-disable-next-line func-call-spacing,no-spaced-func
    listen: jest.fn(() => () => {}),
    getInitialState: jest.fn(),
  };

  Array.from(args).forEach((method) => {
    if (Array.isArray(method)) {
      const [name, fn] = method;
      store[name] = fn;
    } else {
      store[method] = jest.fn();
    }
  });

  return store;
}
