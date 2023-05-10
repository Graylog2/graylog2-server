import { renderHook } from 'wrappedTestingLibrary/hooks';

import asMock from 'helpers/mocking/AsMock';
import fetch from 'logic/rest/FetchProvider';

import useDataNodesCA from './useDataNodesCA';

jest.mock('logic/rest/FetchProvider', () => jest.fn());

describe('useDataNodesCA', () => {
  beforeEach(() => {
    asMock(fetch).mockReturnValue(Promise.resolve({ id: 'ca-id', type: 'ca-type' }));
  });

  it('should return fetched data nodes', async () => {
    const { result, waitFor } = renderHook(() => useDataNodesCA());

    expect(result.current.data).toEqual(undefined);

    await waitFor(() => result.current.isFetching);
    await waitFor(() => !result.current.isFetching);

    expect(fetch).toHaveBeenCalledWith('GET', expect.stringContaining('/api/ca'), undefined, false);

    await waitFor(() => expect(result.current.data).toEqual({ id: 'ca-id', type: 'ca-type' }));
  });
});
