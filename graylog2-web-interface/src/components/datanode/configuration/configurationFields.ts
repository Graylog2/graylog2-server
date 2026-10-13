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
import type {
  ConfigurationField,
  ConfigurationFormValues,
  ConfigurationGroup,
  ConfigurationOverridesPatch,
  ConfigurationOverrideValue,
} from './types';
import { nodeConfigurationFields, nodeSettingsGroup } from './nodeConfigurationFields';

const validateInteger = (value: string) =>
  /^-?\d+$/.test(value) && Number.isSafeInteger(Number(value)) ? undefined : 'Enter a whole number.';

const validatePositiveInteger = (value: string) => {
  const error = validateInteger(value);

  return error ?? (Number(value) > 0 ? undefined : 'Enter a whole number greater than zero.');
};

const isPercentage = (value: string) => /^\d+(?:\.\d+)?%$/.test(value) && Number(value.slice(0, -1)) <= 100;

const isByteSize = (value: string) => /^\d+(?:\.\d+)?(?:b|kb|mb|gb|tb|pb)$/i.test(value);

const validateWatermark = (value: string) => {
  const isRatio = /^\d+(?:\.\d+)?$/.test(value) && Number(value) <= 1;

  return isPercentage(value) || isRatio || isByteSize(value)
    ? undefined
    : 'Enter a percentage from 0 to 100, a ratio from 0 to 1, or a non-negative free-space size.';
};

const validateMemorySize = (value: string) =>
  isPercentage(value) || isByteSize(value)
    ? undefined
    : 'Enter a percentage from 0 to 100 or a non-negative memory size.';

const validateRecoverySize = (value: string) =>
  isByteSize(value) ? undefined : 'Enter a non-negative bandwidth limit. Use 0 to disable throttling.';

const watermarkKeys = [
  'cluster.routing.allocation.disk.watermark.low',
  'cluster.routing.allocation.disk.watermark.high',
  'cluster.routing.allocation.disk.watermark.flood_stage',
] as const;

const parseWatermark = (value: string) => {
  if (value.endsWith('%')) return { kind: 'usage', amount: Number(value.slice(0, -1)) / 100 };

  if (isByteSize(value)) {
    const unit = value.replace(/^[\d.]+/, '').toLowerCase();
    const exponent = ['b', 'kb', 'mb', 'gb', 'tb', 'pb'].indexOf(unit);

    return { kind: 'free_bytes', amount: Number.parseFloat(value) * 1024 ** exponent };
  }

  return { kind: 'usage', amount: Number(value) };
};

const validateWatermarks = (values: ConfigurationFormValues) => {
  if (watermarkKeys.some((key) => values[key] === undefined || validateWatermark(values[key]))) return undefined;

  const low = parseWatermark(values[watermarkKeys[0]]);
  const high = parseWatermark(values[watermarkKeys[1]]);
  const flood = parseWatermark(values[watermarkKeys[2]]);

  if (low.kind !== high.kind || high.kind !== flood.kind) {
    return 'Use disk usage units for all three watermarks, or use free-space units for all three.';
  }

  if (low.kind === 'usage') {
    return low.amount <= high.amount && high.amount <= flood.amount
      ? undefined
      : 'Set watermarks in ascending order: low, high, then flood stage.';
  }

  return low.amount >= high.amount && high.amount >= flood.amount
    ? undefined
    : 'Set free-space watermarks in descending order: low, high, then flood stage.';
};

const configurationGroups: Array<ConfigurationGroup> = [
  {
    id: 'storage',
    label: 'Storage and Capacity',
    description: 'Control disk safeguards and the number of shards your cluster can hold.',
  },
  {
    id: 'search',
    label: 'Search',
    description: 'Set query limits and the memory reserved for the query cache.',
  },
  {
    id: 'indexing',
    label: 'Indexing and Recovery',
    description: 'Set the memory reserved for indexing and the bandwidth used to recover shards.',
  },
];

// Candidate settings and restart rules; the override API does not expose a supported-field catalog.
const configurationFields: Array<ConfigurationField> = [
  {
    key: 'cluster.routing.allocation.disk.watermark.low',
    group: 'storage',
    label: 'Stop allocating shards at',
    description:
      'Stop assigning new shards when disk usage reaches this percentage, or free space falls below a byte threshold.',
    type: 'quantity',
    quantityKind: 'disk',
    requiresRestart: false,
    validate: validateWatermark,
  },
  {
    key: 'cluster.routing.allocation.disk.watermark.high',
    group: 'storage',
    label: 'Move shards away at',
    description:
      'Move shards to other nodes when disk usage reaches this percentage, or free space falls below a byte threshold.',
    type: 'quantity',
    quantityKind: 'disk',
    requiresRestart: false,
    validate: validateWatermark,
  },
  {
    key: 'cluster.routing.allocation.disk.watermark.flood_stage',
    group: 'storage',
    label: 'Protect indexes at',
    description:
      'Block writes to affected indexes when disk usage reaches this percentage, or free space falls below a byte threshold.',
    type: 'quantity',
    quantityKind: 'disk',
    requiresRestart: false,
    validate: validateWatermark,
  },
  {
    key: 'indices.query.bool.max_clause_count',
    group: 'search',
    label: 'Maximum query clauses',
    description: 'Maximum number of clauses allowed in a Boolean query.',
    type: 'number',
    requiresRestart: false,
    validate: validatePositiveInteger,
  },
  {
    key: 'indices.queries.cache.size',
    group: 'search',
    label: 'Query cache size',
    description: 'Memory reserved for the query cache, measured as a percentage of the heap or a fixed memory size.',
    type: 'quantity',
    quantityKind: 'memory',
    requiresRestart: true,
    validate: validateMemorySize,
  },
  {
    key: 'indices.memory.index_buffer_size',
    group: 'indexing',
    label: 'Indexing buffer size',
    description: 'Memory reserved for indexing buffers, measured as a percentage of the heap or a fixed memory size.',
    type: 'quantity',
    quantityKind: 'memory',
    requiresRestart: true,
    validate: validateMemorySize,
  },
  {
    key: 'cluster.max_shards_per_node',
    group: 'storage',
    label: 'Shard limit per data node',
    description:
      'Cluster-wide limit on primary and replica shards, calculated as this value multiplied by the number of non-frozen Data Nodes.',
    type: 'number',
    requiresRestart: false,
    validate: validatePositiveInteger,
  },
  {
    key: 'indices.recovery.max_bytes_per_sec',
    group: 'indexing',
    label: 'Recovery bandwidth limit',
    description: 'Maximum incoming and outgoing recovery traffic per node. Set the limit to 0 to disable throttling.',
    type: 'quantity',
    quantityKind: 'bandwidth',
    requiresRestart: false,
    validate: validateRecoverySize,
  },
];

// Cluster-wide safeguards and query limits stay on the global configuration page.
const nodeSettingKeys = new Set([
  'indices.queries.cache.size',
  'indices.memory.index_buffer_size',
  'indices.recovery.max_bytes_per_sec',
]);

export const getConfigurationGroups = (nodeId: string | null = null) =>
  nodeId === null
    ? configurationGroups
    : [
        nodeSettingsGroup,
        {
          id: 'search',
          label: 'Search',
          description: 'Set the memory reserved for the query cache on this Data Node.',
        },
        {
          id: 'indexing',
          label: 'Indexing and Recovery',
          description: 'Set indexing memory and recovery bandwidth for this Data Node.',
        },
      ];

const getConfigurationFields = (nodeId: string | null = null) =>
  nodeId === null
    ? configurationFields
    : [
        ...nodeConfigurationFields,
        // A dynamic cluster settings update affects every node; per-node YAML edits need a node restart.
        ...configurationFields
          .filter(({ key }) => nodeSettingKeys.has(key))
          .map((field) => ({ ...field, requiresRestart: true })),
      ];

export const buildConfigurationFormValues = (
  overrides: Record<string, ConfigurationOverrideValue>,
  fields: Array<ConfigurationField>,
): ConfigurationFormValues => {
  const fieldKeys = new Set(fields.map(({ key }) => key));

  return Object.fromEntries(
    Object.entries(overrides)
      .filter(([key]) => fieldKeys.has(key))
      .map(([key, { value }]) => [key, value]),
  );
};

export const buildConfigurationOverridesPatch = (
  initialValues: ConfigurationFormValues,
  values: ConfigurationFormValues,
): ConfigurationOverridesPatch => {
  const keys = new Set([...Object.keys(initialValues), ...Object.keys(values)]);

  return Object.fromEntries(
    [...keys]
      .filter((key) => initialValues[key] !== values[key])
      .map((key) => [key, Object.hasOwn(values, key) ? values[key] : null]),
  );
};

export const validateConfigurationFormValues = (
  values: ConfigurationFormValues,
  fields: Array<ConfigurationField>,
): Record<string, string> => {
  const errors: Array<[string, string]> = [];

  fields.forEach((field) => {
    if (!Object.hasOwn(values, field.key)) return;

    const value = values[field.key];
    const error = field.validate(value);

    if (error) errors.push([field.key, error]);
  });

  const watermarkError = validateWatermarks(values);

  if (watermarkError) errors.push([watermarkKeys[0], watermarkError]);

  return Object.fromEntries(errors);
};

export default getConfigurationFields;
