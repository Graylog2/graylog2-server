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
import { qualifyUrl } from 'util/URLUtils';

import enrollEndpointUrl from './enrollEndpointUrl';

jest.mock('util/URLUtils');

describe('enrollEndpointUrl', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    asMock(qualifyUrl).mockReturnValue('http://localhost:9000/api/');
  });

  it('strips the API path from the qualified server URL', () => {
    expect(enrollEndpointUrl()).toBe('http://localhost:9000');
  });

  it('handles an API URL without a trailing slash', () => {
    asMock(qualifyUrl).mockReturnValue('https://graylog.example.org/api');

    expect(enrollEndpointUrl()).toBe('https://graylog.example.org');
  });

  it('keeps the app path prefix when Graylog is served under a subpath', () => {
    asMock(qualifyUrl).mockReturnValue('https://graylog.example.org/graylog/api/');

    expect(enrollEndpointUrl()).toBe('https://graylog.example.org/graylog');
  });

  it('only strips a whole trailing api segment', () => {
    asMock(qualifyUrl).mockReturnValue('https://graylog.example.org/customapi/');

    expect(enrollEndpointUrl()).toBe('https://graylog.example.org/customapi');
  });

  it('strips a bare trailing slash when there is no api segment', () => {
    asMock(qualifyUrl).mockReturnValue('https://graylog.example.org/');

    expect(enrollEndpointUrl()).toBe('https://graylog.example.org');
  });
});
