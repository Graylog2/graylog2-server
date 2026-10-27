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

import type { ConfigurationParameter, ManagedConfiguration, ParameterValues } from '../types';

const byteUnits = (labelSuffix: string): ParameterValues =>
  Object.fromEntries(['b', 'kb', 'mb', 'gb', 'tb', 'pb'].map((unit) => [unit, `${unit.toUpperCase()}${labelSuffix}`]));

const diskUnits: ParameterValues = { '%': '% disk used', '': 'Ratio (0–1)', ...byteUnits(' free') };

const memoryUnits: ParameterValues = { '%': '% of heap', ...byteUnits('') };

const PERCENTAGE = '(100(\\.0+)?|[0-9]{1,2}(\\.[0-9]+)?)%';
const BYTE_SIZE = '[0-9]+(\\.[0-9]+)?([bB]|[kK][bB]|[mM][bB]|[gG][bB]|[tT][bB]|[pP][bB])';
const RATIO = '0(\\.[0-9]+)?|1(\\.0+)?';

type ParameterOverrides = Partial<ConfigurationParameter> & Pick<ConfigurationParameter, 'name' | 'label'>;

export const configurationParameter = (parameter: ParameterOverrides): ConfigurationParameter => ({
  description: `Description of ${parameter.label}.`,
  category: 'Node Settings',
  order: 10,
  type: 'string',
  units: {},
  constraints: { min: undefined, max: undefined, pattern: undefined, values: {} },
  requires_restart: false,
  backing: 'config_file',
  scope: 'node',
  value: null,
  source: null,
  set_value: null,
  set_at: null,
  set_by: null,
  node_value: null,
  node_source: null,
  ...parameter,
});

const withStoredValue = (parameter: ConfigurationParameter, value: string | null): ConfigurationParameter => {
  if (value === null) {
    return {
      ...parameter,
      set_value: null,
      set_at: null,
      set_by: null,
      value: parameter.node_value,
      source: parameter.node_value === null ? null : parameter.node_source,
    };
  }

  return {
    ...parameter,
    set_value: value,
    set_at: '2026-10-20T10:00:00.000Z',
    set_by: 'backend-admin',
    value,
    source: 'database',
  };
};

const clusterParameters = (): Array<ConfigurationParameter> => [
  configurationParameter({
    name: 'cluster.routing.allocation.disk.watermark.low',
    label: 'Stop allocating shards at',
    category: 'Storage and Capacity',
    order: 10,
    type: 'size',
    units: diskUnits,
    constraints: { min: undefined, max: undefined, pattern: `${PERCENTAGE}|${BYTE_SIZE}|${RATIO}`, values: {} },
    backing: 'runtime',
    scope: 'cluster',
  }),
  configurationParameter({
    name: 'cluster.routing.allocation.disk.watermark.high',
    label: 'Move shards away at',
    category: 'Storage and Capacity',
    order: 20,
    type: 'size',
    units: diskUnits,
    constraints: { min: undefined, max: undefined, pattern: `${PERCENTAGE}|${BYTE_SIZE}|${RATIO}`, values: {} },
    backing: 'runtime',
    scope: 'cluster',
  }),
  configurationParameter({
    name: 'cluster.routing.allocation.disk.watermark.flood_stage',
    label: 'Protect indexes at',
    category: 'Storage and Capacity',
    order: 30,
    type: 'size',
    units: diskUnits,
    constraints: { min: undefined, max: undefined, pattern: `${PERCENTAGE}|${BYTE_SIZE}|${RATIO}`, values: {} },
    backing: 'runtime',
    scope: 'cluster',
  }),
  configurationParameter({
    name: 'cluster.max_shards_per_node',
    label: 'Shard limit per Data Node',
    category: 'Storage and Capacity',
    order: 40,
    type: 'integer',
    constraints: { min: 1, max: undefined, pattern: undefined, values: {} },
    backing: 'runtime',
    scope: 'cluster',
  }),
];

const nodeParameters = (): Array<ConfigurationParameter> => [
  configurationParameter({
    name: 'opensearch_heap',
    label: 'OpenSearch JVM heap',
    category: 'Node Settings',
    order: 10,
    type: 'size',
    units: { g: 'GB', m: 'MB', k: 'KB' },
    constraints: { min: undefined, max: undefined, pattern: '[1-9][0-9]*[gGmMkK]', values: {} },
    requires_restart: true,
  }),
  configurationParameter({
    name: 'node_roles',
    label: 'Node roles',
    category: 'Node Settings',
    order: 20,
    type: 'list',
    constraints: {
      min: undefined,
      max: undefined,
      pattern: undefined,
      values: {
        cluster_manager: 'Cluster manager',
        data: 'Data',
        ingest: 'Ingest',
        remote_cluster_client: 'Remote cluster client',
        search: 'Search',
      },
    },
    requires_restart: true,
  }),
  configurationParameter({
    name: 'opensearch_indices_query_bool_max_clause_count',
    label: 'Maximum query clauses',
    category: 'Search',
    order: 10,
    type: 'integer',
    constraints: { min: 1, max: undefined, pattern: undefined, values: {} },
    requires_restart: true,
  }),
  configurationParameter({
    name: 'indices.queries.cache.size',
    label: 'Query cache size',
    category: 'Search',
    order: 20,
    type: 'size',
    units: memoryUnits,
    constraints: { min: undefined, max: undefined, pattern: `${PERCENTAGE}|${BYTE_SIZE}`, values: {} },
    requires_restart: true,
    backing: 'runtime',
  }),
  configurationParameter({
    name: 'indices.memory.index_buffer_size',
    label: 'Indexing buffer size',
    category: 'Indexing and Recovery',
    order: 10,
    type: 'size',
    units: memoryUnits,
    constraints: { min: undefined, max: undefined, pattern: `${PERCENTAGE}|${BYTE_SIZE}`, values: {} },
    requires_restart: true,
    backing: 'runtime',
  }),
  configurationParameter({
    name: 'indices.recovery.max_bytes_per_sec',
    label: 'Recovery bandwidth limit',
    category: 'Indexing and Recovery',
    order: 20,
    type: 'size',
    units: byteUnits('/s'),
    constraints: { min: undefined, max: undefined, pattern: BYTE_SIZE, values: {} },
    backing: 'runtime',
  }),
];

export const managedConfigurationResponse = (
  storedValues: Record<string, string> = {},
  nodeId: string | null = null,
): ManagedConfiguration => ({
  node_type: 'datanode',
  node_id: nodeId,
  parameters: (nodeId === null ? clusterParameters() : nodeParameters()).map((parameter) =>
    withStoredValue(parameter, storedValues[parameter.name] ?? null),
  ),
});

export const sampleClusterConfiguration = managedConfigurationResponse({
  'cluster.routing.allocation.disk.watermark.low': '85%',
  'cluster.routing.allocation.disk.watermark.high': '90%',
  'cluster.routing.allocation.disk.watermark.flood_stage': '95%',
  'cluster.max_shards_per_node': '1000',
});

export const sampleNodeConfiguration = (nodeId: string) =>
  managedConfigurationResponse(
    {
      opensearch_heap: '2g',
      node_roles: 'cluster_manager,data,ingest,remote_cluster_client',
      opensearch_indices_query_bool_max_clause_count: '32768',
      'indices.queries.cache.size': '10%',
      'indices.memory.index_buffer_size': '10%',
      'indices.recovery.max_bytes_per_sec': '40mb',
    },
    nodeId,
  );

const mockManagedConfigurationApi = (initialConfigurations: Array<ManagedConfiguration> = []) => {
  const storedValues = new Map<string | null, Record<string, string>>(
    initialConfigurations.map((configuration) => [
      configuration.node_id,
      Object.fromEntries(
        configuration.parameters
          .filter(({ set_value: value }) => value !== null)
          .map(({ name, set_value: value }) => [name, value]),
      ),
    ]),
  );
  const transport = asMock(request).mockReset();

  transport.mockImplementation(async (method, _path, body, query) => {
    const nodeId: string | null = (method === 'GET' ? query.node_id : body.node_id) ?? null;
    const values = { ...(storedValues.get(nodeId) ?? {}) };

    if (method !== 'GET') {
      Object.entries(body.values as Record<string, string | null>).forEach(([name, value]) => {
        if (value === null) delete values[name];
        else values[name] = value;
      });
      storedValues.set(nodeId, values);
    }

    const configuration =
      initialConfigurations.find(({ node_id: id }) => id === nodeId) ?? managedConfigurationResponse({}, nodeId);

    return {
      ...configuration,
      parameters: configuration.parameters.map((parameter) =>
        withStoredValue(parameter, values[parameter.name] ?? null),
      ),
    };
  });

  return transport;
};

export default mockManagedConfigurationApi;
