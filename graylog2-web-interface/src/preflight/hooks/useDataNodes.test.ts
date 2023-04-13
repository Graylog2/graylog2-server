import { renderHook } from 'wrappedTestingLibrary/hooks';

import asMock from 'helpers/mocking/AsMock';
import fetch from 'logic/rest/FetchProvider';

import useDataNodes from './useDataNodes';

jest.mock('logic/rest/FetchProvider', () => jest.fn());

describe('useDataNodes', () => {
  const availableDataNodes = [
    {
      id: 'data-node-id-1',
      name: 'data-node-name',
      transportAddress: 'transport.address1',
      altNames: [],
      status: 'UNCONFIGURED',
    },
    {
      id: 'data-node-id-2',
      name: 'data-node-name',
      altNames: [],
      transportAddress: 'transport.address2',
      status: 'UNCONFIGURED',
    },
    {
      id: 'data-node-id-3',
      name: 'data-node-name',
      altNames: [],
      transportAddress: 'transport.address3',
      status: 'UNCONFIGURED',
    },
  ];

  beforeEach(() => {
    asMock(fetch).mockReturnValue(Promise.resolve(availableDataNodes));
  });

  it('should return data nodes CA status', async () => {
    const { result, waitFor } = renderHook(() => useDataNodes());

    expect(result.current.data).toEqual([]);

    await waitFor(() => result.current.isFetching);
    await waitFor(() => !result.current.isFetching);

    expect(fetch).toHaveBeenCalledWith('GET', expect.stringContaining('/api/data_nodes'), undefined, false);

    await waitFor(() => expect(result.current.data).toEqual(availableDataNodes));
  });
});
