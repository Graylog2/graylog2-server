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
import asMock from 'helpers/mocking/AsMock';
import request from 'routing/request';

import type { ConfigurationOverrides, ConfigurationOverridesPatch } from '../types';

export const configurationOverridesResponse = (
  values: Record<string, string> = {},
  nodeId: string | null = null,
): ConfigurationOverrides => ({
  node_type: 'datanode',
  node_id: nodeId,
  overrides: Object.fromEntries(
    Object.entries(values).map(([key, value]) => [
      key,
      { value, updated_at: '2026-10-13T10:00:00.000Z', updated_by: 'backend-admin' },
    ]),
  ),
});

export const sampleGlobalConfiguration = configurationOverridesResponse({
  'cluster.routing.allocation.disk.watermark.low': '85%',
  'cluster.routing.allocation.disk.watermark.high': '90%',
  'cluster.routing.allocation.disk.watermark.flood_stage': '95%',
  'indices.query.bool.max_clause_count': '32768',
  'indices.queries.cache.size': '10%',
  'indices.memory.index_buffer_size': '10%',
  'cluster.max_shards_per_node': '1000',
  'indices.recovery.max_bytes_per_sec': '40mb',
});

export const sampleNodeConfiguration = (nodeId: string) =>
  configurationOverridesResponse(
    {
      opensearch_heap: '2g',
      node_roles: 'cluster_manager,data,ingest,remote_cluster_client',
    },
    nodeId,
  );

const mockConfigurationOverridesApi = (initialConfigurations: Array<ConfigurationOverrides> = []) => {
  const configurations = new Map(initialConfigurations.map((configuration) => [configuration.node_id, configuration]));
  const transport = asMock(request).mockReset();

  transport.mockImplementation(async (method, _path, body, query) => {
    const nodeId: string | null = (method === 'GET' ? query.node_id : body.node_id) ?? null;
    const current = configurations.get(nodeId) ?? configurationOverridesResponse({}, nodeId);

    if (method === 'GET') return current;

    const changes: ConfigurationOverridesPatch = body.overrides;
    const values = Object.fromEntries(Object.entries(current.overrides).map(([key, { value }]) => [key, value]));
    Object.entries(changes).forEach(([key, value]) => {
      if (value === null) delete values[key];
      else values[key] = value;
    });
    const saved = configurationOverridesResponse(values, nodeId);
    configurations.set(nodeId, saved);

    return saved;
  });

  return transport;
};

export default mockConfigurationOverridesApi;
