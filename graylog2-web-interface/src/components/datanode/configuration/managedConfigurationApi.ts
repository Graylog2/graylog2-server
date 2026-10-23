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
import { SystemManagedConfiguration } from '@graylog/server-api';

import request from 'routing/request';

import type { ManagedConfiguration, ConfigurationValuesPatch } from './types';

const NODE_TYPE = 'datanode';

const managedConfigurationApi = {
  get: async (nodeId: string | null = null): Promise<ManagedConfiguration> => {
    const response = await SystemManagedConfiguration.get(NODE_TYPE, nodeId ?? undefined);

    return { ...response, node_id: response.node_id ?? null } as ManagedConfiguration;
  },
  // The generated PUT type doesn't allow null values, which is how a stored value is removed.
  update: (values: ConfigurationValuesPatch, nodeId: string | null = null): Promise<ManagedConfiguration> =>
    request(
      'PUT',
      '/system/managed_configuration',
      { node_type: NODE_TYPE, node_id: nodeId, values },
      {},
      { Accept: ['application/json'] },
    ),
};

export default managedConfigurationApi;
