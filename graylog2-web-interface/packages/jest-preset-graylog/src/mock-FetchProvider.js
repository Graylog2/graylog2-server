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
const mockResponse = {};

class MockBuilder {
  authenticated = () => this;

  session = () => this;

  setHeader = () => this;

  json = () => this;

  plaintext = () => this;

  noSessionExtension = () => this;

  build = () => Promise.resolve({});
}

class MockFetchError {
}

const MockFetchProvider = Object.assign(
  jest.fn(() => Promise.resolve(mockResponse)),
  {
    FetchError: MockFetchError,
    Builder: MockBuilder,
    default: jest.fn(() => Promise.resolve(mockResponse)),
    fetchPlainText: jest.fn(() => Promise.resolve(mockResponse)),
    fetchPeriodically: jest.fn(() => Promise.resolve(mockResponse)),
    fetchFile: jest.fn(() => Promise.resolve(mockResponse)),
  },
);

jest.mock('logic/rest/FetchProvider', () => MockFetchProvider);
