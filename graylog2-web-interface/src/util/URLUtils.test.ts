import { qualifyUrl, qualifyUrlWithSessionCredentials } from 'util/URLUtils';
import { asMock } from 'helpers/mocking';

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
});

describe('qualifyUrlWithSessionCredentials', () => {
  it('adds session credentials to url if server url is relative', () => {
    asMock(AppConfig.gl2ServerUrl).mockReturnValue('/api');
    delete window.location;
    window.location = mockLocation('https://something.foo:2342/gnarf/42?bar=23');

    expect(qualifyUrlWithSessionCredentials('/something/else/23', 'deadbeef'))
      .toEqual('https://deadbeef:session@something.foo:2342/api/something/else/23');
  });
});
