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
import type { ConfigurationField, ConfigurationGroup } from './types';

export const nodeSettingsGroup: ConfigurationGroup = {
  id: 'node',
  label: 'Node Settings',
  description: 'Configure the OpenSearch JVM heap and roles for this Data Node.',
};

const roles = [
  { value: 'cluster_manager', label: 'Cluster manager' },
  { value: 'data', label: 'Data' },
  { value: 'ingest', label: 'Ingest' },
  { value: 'remote_cluster_client', label: 'Remote cluster client' },
  { value: 'search', label: 'Search' },
];

// Automatic heap sizing is a pitch requirement, not a value supported by the current heap validator.
export const nodeConfigurationFields: Array<ConfigurationField> = [
  {
    key: 'opensearch_heap',
    group: 'node',
    label: 'OpenSearch JVM heap',
    description: 'Set a fixed initial and maximum OpenSearch JVM heap size. Automatic sizing is not available yet.',
    type: 'heap',
    requiresRestart: true,
    validate: (value) =>
      /^[1-9]\d*[gmk]$/i.test(value) ? undefined : 'Enter a whole number greater than zero for the heap size.',
  },
  {
    key: 'node_roles',
    group: 'node',
    label: 'Node roles',
    description: 'Choose the roles this Data Node performs in the OpenSearch cluster.',
    type: 'roles',
    options: roles,
    requiresRestart: true,
    validate: (value) => {
      const selected = value.split(',');

      return selected.every((role) => roles.some((option) => option.value === role)) &&
        new Set(selected).size === selected.length
        ? undefined
        : 'Select at least one role from the available options.';
    },
  },
];
