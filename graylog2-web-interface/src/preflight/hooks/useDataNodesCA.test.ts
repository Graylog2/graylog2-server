import { renderHook } from 'wrappedTestingLibrary/hooks';

import asMock from 'helpers/mocking/AsMock';
import fetch from 'logic/rest/FetchProvider';

import useDataNodesCA from './useDataNodesCA';

jest.mock('logic/rest/FetchProvider', () => jest.fn());

describe('useDataNodesCA', () => {
  beforeEach(() => {
    asMock(fetch).mockReturnValue(Promise.resolve({ isConfigured: false }))
  })

  it('should return fetched data nodes', async () => {
    const { result, waitFor } = renderHook(() => useDataNodesCA());

    // expect(result.current.data).toEqual([]);
    //
    // await waitFor(() => result.current.isFetching);
    // await waitFor(() => !result.current.isFetching);
    //
    // expect(fetch).toHaveBeenCalledWith('GET', '/api/preflight/ca');
    waitFor(() => expect(result.current.data).toEqual({ data: { isConfigured: false }, isFetching: false }));
  });
});
