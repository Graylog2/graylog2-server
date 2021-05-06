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
import MockStore from 'helpers/mocking/StoreMock';

import { StreamSearchPage } from 'views/pages';

import bindings from './bindings';

jest.mock('util/AppConfig', () => ({
  gl2ServerUrl: () => 'localhost:9000/api/',
  gl2AppPathPrefix: jest.fn(() => '/gl2/'),
  isFeatureEnabled: () => false,
  isCloud: jest.fn(() => false),
}));

jest.mock('views/stores/FieldTypesStore', () => ({
  FieldTypesStore: MockStore(['getInitialState', () => ({ all: {}, queryFields: {} })]),
}));

describe('bindings.routes', () => {
  it('Stream search route must be unqualified', () => {
    const streamSearchPageRoute = bindings.routes.find(({ component }) => (component === StreamSearchPage));

    if (!streamSearchPageRoute) {
      throw new Error('Stream search page route was not registered.');
    }

    expect(streamSearchPageRoute.path).toEqual('/streams/:streamId/search');
  });
});
