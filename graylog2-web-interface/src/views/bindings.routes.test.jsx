// @flow strict
import { StreamSearchPage } from 'views/pages';
import bindings from './bindings';

jest.mock('util/AppConfig', () => ({
  gl2ServerUrl: () => 'localhost:9000/api/',
  gl2AppPathPrefix: jest.fn(() => '/gl2/'),
}));

describe('bindings.routes', () => {
  it('Stream search route must be unqualified', () => {
    const streamSearchPageRoute = bindings.routes.find(({ component }) => (component === StreamSearchPage));
    expect(streamSearchPageRoute.path).toEqual('/streams/:streamId/search');
  });
});
