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
import { Builder } from 'logic/rest/FetchProvider';

import request from './request';

jest.mock('util/URLUtils', () => ({
  qualifyUrl: (url: string) => `http://example.org${url}`,
}));

const mockSetHeaders = jest.fn();
const mockJson = jest.fn();
const mockBuild = jest.fn(() => Promise.resolve({}));

jest.mock('logic/rest/FetchProvider', () => ({
  Builder: jest.fn().mockImplementation(() => ({
    json: (...args: unknown[]) => {
      mockJson(...args);

      return {
        setHeaders: (headers: Record<string, unknown>) => {
          mockSetHeaders(headers);

          return { build: mockBuild };
        },
      };
    },
  })),
}));

describe('request', () => {
  beforeEach(() => {
    mockSetHeaders.mockClear();
    mockJson.mockClear();
    mockBuild.mockClear();
    (Builder as unknown as jest.Mock).mockClear();
  });

  it('does not set the session-extension header when no options are provided', () => {
    request('GET', '/foo', null, {}, {});

    expect(mockSetHeaders).toHaveBeenCalledWith({});
  });

  it('does not set the session-extension header when options omit requestShouldExtendSession', () => {
    request('GET', '/foo', null, {}, {}, {});

    expect(mockSetHeaders).toHaveBeenCalledWith({});
  });

  it('opts out of session extension when requestShouldExtendSession is false', () => {
    request('GET', '/foo', null, {}, {}, { requestShouldExtendSession: false });

    expect(mockSetHeaders).toHaveBeenCalledWith({ 'X-Graylog-No-Session-Extension': true });
  });

  it('opts in to session extension when requestShouldExtendSession is true', () => {
    request('GET', '/foo', null, {}, {}, { requestShouldExtendSession: true });

    expect(mockSetHeaders).toHaveBeenCalledWith({ 'X-Graylog-No-Session-Extension': false });
  });

  it('merges caller-provided headers with the session-extension opt-out header', () => {
    request('GET', '/foo', null, {}, { 'X-Custom': 'value' }, { requestShouldExtendSession: false });

    expect(mockSetHeaders).toHaveBeenCalledWith({
      'X-Custom': 'value',
      'X-Graylog-No-Session-Extension': true,
    });
  });

  it('passes method and qualified URL to the Builder', () => {
    request('POST', '/bar', { hello: 'world' }, { q: 'value' }, {});

    expect(Builder).toHaveBeenCalledWith('POST', 'http://example.org/bar?q=value');
    expect(mockJson).toHaveBeenCalledWith({ hello: 'world' });
  });
});
