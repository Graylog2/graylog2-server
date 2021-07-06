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

import { qualifyUrl, qualifyUrlWithSessionCredentials } from 'util/URLUtils';
import AppConfig from 'util/AppConfig';

jest.mock('util/AppConfig');

const oldLocation = window.location;

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
});

describe('qualifyUrlWithSessionCredentials', () => {
  it('adds session credentials to url if server url is relative', () => {
    asMock(AppConfig.gl2ServerUrl).mockReturnValue('/api');
    delete window.location;
    window.location = mockLocation('https://something.foo:2342/gnarf/42?bar=23');

    expect(qualifyUrlWithSessionCredentials('/something/else/23', 'deadbeef'))
      .toEqual('https://deadbeef:session@something.foo:2342/api/something/else/23');
  });

  it('adds session credentials to url that was already qualified', () => {
    asMock(AppConfig.gl2ServerUrl).mockReturnValue('http://something.graylog.cloud/api');

    expect(qualifyUrlWithSessionCredentials(qualifyUrl('/foo'), 'deadbeef'))
      .toEqual('http://deadbeef:session@something.graylog.cloud/api/foo');
  });
});
