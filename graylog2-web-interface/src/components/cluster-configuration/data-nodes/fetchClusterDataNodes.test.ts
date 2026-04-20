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
import fetch from 'logic/rest/FetchProvider';
import asMock from 'helpers/mocking/AsMock';
import { fetchDataNodes } from 'components/datanode/hooks/useDataNodes';
import type { DataNode } from 'components/datanode/Types';

import { fetchClusterDataNodesWithMetrics } from './fetchClusterDataNodes';

jest.mock('components/datanode/hooks/useDataNodes', () => ({
  fetchDataNodes: jest.fn(),
  keyFn: jest.fn(),
}));

jest.mock('logic/rest/FetchProvider', () => jest.fn());

describe('fetchClusterDataNodesWithMetrics', () => {
  const createDataNode = (overrides: Partial<DataNode>): DataNode => ({
    hostname: 'node',
    id: 'node-id',
    is_leader: false,
    is_master: false,
    last_seen: '2024-01-01T00:00:00.000Z',
    node_id: 'node-id',
    short_node_id: 'node',
    transport_address: '127.0.0.1:9300',
    type: 'DATA',
    status: 'CONNECTED',
    cert_valid_until: null,
    datanode_version: '5.0.0',
    version_compatible: true,
    cluster_address: 'http://localhost:9200',
    rest_api_address: 'http://localhost:8999',
    action_queue: '',
    ...overrides,
  });

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('skips metrics requests for incompatible data nodes', async () => {
    asMock(fetchDataNodes).mockResolvedValue({
      list: [
        createDataNode({ hostname: 'compatible-node', version_compatible: true }),
        createDataNode({ hostname: 'incompatible-node', version_compatible: false }),
      ],
      pagination: { total: 2, count: 2, page: 1, per_page: 0, query: null },
      attributes: [],
    });
    asMock(fetch).mockResolvedValue({ metrics: [] });

    await fetchClusterDataNodesWithMetrics();

    expect(fetch).toHaveBeenCalledTimes(1);
    expect(asMock(fetch).mock.calls[0][1]).toContain('/datanodes/compatible-node/rest/metrics/multiple');
  });
});
