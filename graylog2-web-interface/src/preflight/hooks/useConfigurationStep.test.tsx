import { renderHook } from 'wrappedTestingLibrary/hooks';

/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import useDataNodesCA from 'preflight/hooks/useDataNodesCA';
import { CONFIGURATION_STEPS } from 'preflight/Constants';
import asMock from 'helpers/mocking/AsMock';
import useDataNodes from 'preflight/hooks/useDataNodes';
import { dataNodes } from 'fixtures/dataNodes';

import useConfigurationStep from './useConfigurationStep';

jest.mock('preflight/hooks/useDataNodes');
jest.mock('preflight/hooks/useDataNodesCA');
jest.mock('logic/rest/FetchProvider');

describe('useConfigurationStep', () => {
  const useDataNodesResult = {
    data: dataNodes,
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
        status: 'CONNECTED',
      }],
    });

    const { result, waitFor } = renderHook(() => useConfigurationStep());

    await waitFor(() => expect(result.current).toEqual({
      step: CONFIGURATION_STEPS.CONFIGURATION_FINISHED.key,
      isLoading: false,
    }));
  });
});
