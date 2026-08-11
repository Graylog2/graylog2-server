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
import asMock from 'helpers/mocking/AsMock';
import AppConfig from 'util/AppConfig';
import { qualifyUrl } from 'util/URLUtils';

import enrollEndpointUrl from './enrollEndpointUrl';

jest.mock('util/AppConfig');
jest.mock('util/URLUtils');

describe('enrollEndpointUrl', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    asMock(AppConfig.gl2AppPathPrefix).mockReturnValue('');
    asMock(qualifyUrl).mockReturnValue('http://localhost:9000/api/');
  });

  it('uses the scheme and API port from the qualified server URL', () => {
    expect(enrollEndpointUrl('collectors.example.org')).toBe('http://collectors.example.org:9000');
  });

  it('omits the port when the API URL has none', () => {
    asMock(qualifyUrl).mockReturnValue('https://graylog.example.org/api/');

    expect(enrollEndpointUrl('collectors.example.org')).toBe('https://collectors.example.org');
  });

  it('appends the app path prefix when Graylog is served under a subpath', () => {
    asMock(AppConfig.gl2AppPathPrefix).mockReturnValue('/graylog/');

    expect(enrollEndpointUrl('collectors.example.org')).toBe('http://collectors.example.org:9000/graylog');
  });

  it('ignores a bare "/" path prefix', () => {
    asMock(AppConfig.gl2AppPathPrefix).mockReturnValue('/');

    expect(enrollEndpointUrl('collectors.example.org')).toBe('http://collectors.example.org:9000');
  });
});
