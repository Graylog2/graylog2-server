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
import { SystemConfigurationOverrides } from '@graylog/server-api';

import request from 'routing/request';

import type { ConfigurationOverrides, ConfigurationOverridesPatch } from './types';

const configurationOverridesApi = {
  get: async (nodeId: string | null = null): Promise<ConfigurationOverrides> => {
    const response = await SystemConfigurationOverrides.get('datanode', nodeId ?? undefined);

    return { ...response, node_type: 'datanode', node_id: response.node_id ?? null };
  },
  // The generated PUT types omit nullable node IDs and values. Keep this correction at the API boundary.
  update: (changes: ConfigurationOverridesPatch, nodeId: string | null = null): Promise<ConfigurationOverrides> =>
    request(
      'PUT',
      '/system/configuration_overrides',
      { node_type: 'datanode', node_id: nodeId, overrides: changes },
      {},
      { Accept: ['application/json'] },
    ),
};

export default configurationOverridesApi;
