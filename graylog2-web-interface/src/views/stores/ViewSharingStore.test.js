// @flow strict
import fetch from 'logic/rest/FetchProvider';
import { ViewSharingStore } from './ViewSharingStore';

jest.mock('logic/rest/FetchProvider', () => jest.fn());
jest.mock('util/AppConfig', () => ({ gl2ServerUrl: () => 'gl2ServerUrl' }));

describe('ViewSharingStore', () => {
  it('uses correct URL when removing view sharing config', (done) => {
    fetch.mockImplementation((method, url) => {
      expect(method).toEqual('DELETE');
      expect(url).toEqual('gl2ServerUrl/views/viewId/share');
      done();
      return Promise.resolve(null);
    });

    ViewSharingStore.remove('viewId');
  });
  it('does not deserialize response when removing view sharing config', () => {
    fetch.mockImplementation(() => Promise.resolve(null));

    return ViewSharingStore.remove('viewId')
      .then((response) => {
        expect(response).toEqual(null);
      });
  });
});
