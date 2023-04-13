import { renderHook } from 'wrappedTestingLibrary/hooks';

import asMock from 'helpers/mocking/AsMock';
import useDataNodes from 'preflight/hooks/useDataNodes';
import useDataNodesCA from 'preflight/hooks/useDataNodesCA';
import { CONFIGURATION_STEPS } from 'preflight/Constants';

import useConfigurationStep from './useConfigurationStep';

jest.mock('preflight/hooks/useDataNodes');
jest.mock('preflight/hooks/useDataNodesCA');
jest.mock('logic/rest/FetchProvider');

describe('useConfigurationStep', () => {
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

  const useDataNodesResult = {
    data: availableDataNodes,
    isInitialLoading: false,
    isFetching: false,
    error: undefined,
  };

  const useDataNodesCAResult = {
    data: { isConfigured: false },
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
      data: { isConfigured: false },
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
      data: { isConfigured: true },
    });

    asMock(useDataNodes).mockReturnValue({
      ...useDataNodesResult,
      data: [{
        id: 'data-node-id-1',
        name: 'data-node-name',
        transportAddress: 'transport.address1',
        altNames: [],
        status: 'UNCONFIGURED',
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
      data: { isConfigured: true },
    });

    asMock(useDataNodes).mockReturnValue({
      ...useDataNodesResult,
      data: [{
        id: 'data-node-id-1',
        name: 'data-node-name',
        transportAddress: 'transport.address1',
        altNames: [],
        status: 'SIGNED',
      }],
    });

    const { result, waitFor } = renderHook(() => useConfigurationStep());

    await waitFor(() => expect(result.current).toEqual({
      step: CONFIGURATION_STEPS.CERTIFICATE_PROVISIONING.key,
      isLoading: false,
    }));
  });
});
