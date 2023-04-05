import { renderHook } from 'wrappedTestingLibrary/hooks';

import asMock from 'helpers/mocking/AsMock';
import fetch from 'logic/rest/FetchProvider';

import useDataNodes from './useDataNodes';

jest.mock('logic/rest/FetchProvider');

describe('useDataNodes', () => {
  const availableDataNodes = [
    { id: 'data-node-id-1', transportAddress: 'transport.address1', isSecured: false },
    { id: 'data-node-id-2', transportAddress: 'transport.address2', isSecured: false },
    { id: 'data-node-id-3', transportAddress: 'transport.address3', isSecured: false },
  ];

  beforeEach(() => {
    asMock(fetch).mockReturnValue(Promise.resolve(availableDataNodes));
  });

  it('should return data nodes CA status', async () => {
    const { result, waitFor } = renderHook(() => useDataNodes());

    // expect(result.current.data).toEqual([]);
    //
    // await waitFor(() => result.current.isFetching);
    // await waitFor(() => !result.current.isFetching);
    //
    // expect(fetch).toHaveBeenCalledWith('GET', '/api/preflight/datanodes');
    await waitFor(() => expect(result.current.data).toEqual(availableDataNodes));
  });
});
