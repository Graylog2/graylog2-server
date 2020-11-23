// @flow strict
import { StreamSearchPage } from 'views/pages';
import bindings from './bindings';

jest.mock('util/AppConfig', () => ({
  gl2ServerUrl: () => 'localhost:9000/api/',
  gl2AppPathPrefix: jest.fn(() => '/gl2/'),
  isCloud: jest.fn(() => false),
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
