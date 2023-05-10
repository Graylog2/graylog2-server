import { renderHook } from 'wrappedTestingLibrary/hooks';

import useDataNodesCA from 'preflight/hooks/useDataNodesCA';
import { CONFIGURATION_STEPS } from 'preflight/Constants';
import asMock from 'helpers/mocking/AsMock';
import useDataNodes from 'preflight/hooks/useDataNodes';

import useConfigurationStep from './useConfigurationStep';

jest.mock('preflight/hooks/useDataNodes');
jest.mock('preflight/hooks/useDataNodesCA');
jest.mock('logic/rest/FetchProvider');

describe('useConfigurationStep', () => {
  const availableDataNodes = [
    {
      hostname: '192.168.0.10',
      id: 'data-node-id-1',
      is_leader: false,
      is_master: false,
      last_seen: '2020-01-10T10:40:00.000Z',
      node_id: 'node-id-complete-1',
      short_node_id: 'node-id-1',
      transport_address: 'http://localhost:9200',
      type: 'DATANODE',
    },
    {
      hostname: '192.168.0.11',
      id: 'data-node-id-2',
      is_leader: false,
      is_master: false,
      last_seen: '2020-01-10T10:40:00.000Z',
      node_id: 'node-id-complete-2',
      short_node_id: 'node-id-2',
      transport_address: 'http://localhost:9201',
      type: 'DATANODE',
    },
    {
      hostname: '192.168.0.12',
      id: 'data-node-id-3',
      is_leader: false,
      is_master: false,
      last_seen: '2020-01-10T10:40:00.000Z',
      node_id: 'node-id-complete-3',
      short_node_id: 'node-id-3',
      transport_address: 'http://localhost:9202',
      type: 'DATANODE',
    },
  ];

  const useDataNodesResult = {
    data: availableDataNodes,
    isInitialLoading: false,
    isFetching: false,
    error: undefined,
  };

  const useDataNodesCAResult = {
    data: undefined,
    isInitialLoading: false,
    isFetching: false,
    error: undefined,
  };

  beforeEach(() => {
    asMock(useDataNodes).mockReturnValue(useDataNodesResult);
    asMock(useDataNodesCA).mockReturnValue(useDataNodesCAResult);
  });

  it('should return isLoading: true when data nodes are loading', async () => {
    asMock(useDataNodes).mockReturnValue({
      ...useDataNodesResult,
      isInitialLoading: true,
    });

    const { result, waitFor } = renderHook(() => useConfigurationStep());

    await waitFor(() => expect(result.current.isLoading).toBe(true));
  });

  it('should return isLoading: true when CA status is loading', async () => {
    asMock(useDataNodesCA).mockReturnValue({
      ...useDataNodesCAResult,
      isInitialLoading: true,
    });

    const { result, waitFor } = renderHook(() => useConfigurationStep());

    await waitFor(() => expect(result.current.isLoading).toBe(true));
  });

  it('should define CA configuration step as active step, when CA is not configured', async () => {
    asMock(useDataNodesCA).mockReturnValue({
      ...useDataNodesResult,
      data: undefined,
    });

    const { result, waitFor } = renderHook(() => useConfigurationStep());

    await waitFor(() => expect(result.current).toEqual({
      step: CONFIGURATION_STEPS.CA_CONFIGURATION.key,
      isLoading: false,
    }));
  });

  it('should define certificate provisioning step as active step, when CA has not been provisioned', async () => {
    asMock(useDataNodesCA).mockReturnValue({
      ...useDataNodesResult,
      data: { id: 'ca-id', type: 'ca-type' },
    });

    asMock(useDataNodes).mockReturnValue({
      ...useDataNodesResult,
      data: [{
        hostname: '192.168.0.10',
        id: 'data-node-id-1',
        is_leader: false,
        is_master: false,
        last_seen: '2020-01-10T10:40:00.000Z',
        node_id: 'node-id-complete-1',
        short_node_id: 'node-id-1',
        transport_address: 'http://localhost:9200',
        type: 'DATANODE',
      }],
    });

    const { result, waitFor } = renderHook(() => useConfigurationStep());

    await waitFor(() => expect(result.current).toEqual({
      step: CONFIGURATION_STEPS.CERTIFICATE_PROVISIONING.key,
      isLoading: false,
    }));
  });

  it('should define success step as active step, when CA been provisioned', async () => {
    asMock(useDataNodesCA).mockReturnValue({
      ...useDataNodesResult,
      data: { id: 'ca-id', type: 'ca-type' },
    });

    asMock(useDataNodes).mockReturnValue({
      ...useDataNodesResult,
      data: [{
        hostname: '192.168.0.10',
        id: 'data-node-id-1',
        is_leader: false,
        is_master: false,
        last_seen: '2020-01-10T10:40:00.000Z',
        node_id: 'node-id-complete-1',
        short_node_id: 'node-id-1',
        transport_address: 'http://localhost:9200',
        type: 'DATANODE',
      }],
    });

    const { result, waitFor } = renderHook(() => useConfigurationStep());

    await waitFor(() => expect(result.current).toEqual({
      step: CONFIGURATION_STEPS.CERTIFICATE_PROVISIONING.key,
      isLoading: false,
    }));
  });
});
