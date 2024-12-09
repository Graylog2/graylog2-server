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
import { asMock } from 'helpers/mocking';
import { qualifyUrl, currentPathnameWithoutPrefix } from 'util/URLUtils';
import AppConfig from 'util/AppConfig';

jest.mock('util/AppConfig');

const oldLocation = window.location;

// eslint-disable-next-line compat/compat
const mockLocation = (url: string): Location => new URL(url) as unknown as Location;

describe('qualifyUrl', () => {
  afterEach(() => {
    window.location = oldLocation;
  });

  it('qualifies url with hostname/scheme from current location if server url is relative', () => {
    asMock(AppConfig.gl2ServerUrl).mockReturnValue('/api');
    delete window.location;
    window.location = mockLocation('https://something.foo:2342/gnarf/42?bar=23');

    expect(qualifyUrl('/foo?baz=17')).toEqual('https://something.foo:2342/api/foo?baz=17');
  });

  it('qualifies url with server url only if it contains host and scheme', () => {
    asMock(AppConfig.gl2ServerUrl).mockReturnValue('http://something.graylog.cloud/api');

    expect(qualifyUrl('/foo')).toEqual('http://something.graylog.cloud/api/foo');
  });

  it('is idempotent', () => {
    asMock(AppConfig.gl2ServerUrl).mockReturnValue('http://something.graylog.cloud/api');

    const qualifiedUrl = qualifyUrl('/foo');

    expect(qualifyUrl(qualifiedUrl)).toEqual('http://something.graylog.cloud/api/foo');
  });

  describe('currentPathnameWithoutPrefix', () => {
    const setLocation = (pathname: string) => Object.defineProperty(window, 'location', {
      value: {
        pathname,
      },
      writable: true,
    });

    const mockPathPrefix = (pathPrefix: string | undefined | null) => asMock(AppConfig.gl2AppPathPrefix).mockReturnValue(pathPrefix);

    it('returns current path when prefix is undefined/null/empty/single slash', () => {
      const pathname = '/welcome';
      setLocation(pathname);

      mockPathPrefix(null);

      expect(currentPathnameWithoutPrefix()).toBe(pathname);

      mockPathPrefix(undefined);

      expect(currentPathnameWithoutPrefix()).toBe(pathname);

      mockPathPrefix('');

      expect(currentPathnameWithoutPrefix()).toBe(pathname);

      mockPathPrefix('/');

      expect(currentPathnameWithoutPrefix()).toBe(pathname);
    });

    it('returns current path when prefix is defined', () => {
      const pathname = '/welcome';
      setLocation(`/foo${pathname}`);

      mockPathPrefix('/foo');

      expect(currentPathnameWithoutPrefix()).toBe(pathname);
    });

    it('returns current path when prefix is defined and ends with `/`', () => {
      const pathname = '/welcome';
      setLocation(`/foo${pathname}`);

      mockPathPrefix('/foo/');

      expect(currentPathnameWithoutPrefix()).toBe(pathname);
    });
  });
});
