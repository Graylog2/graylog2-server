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

import type { ApiManagedConfiguration, ManagedConfiguration, ConfigurationValuesPatch } from './types';

const NODE_TYPE = 'datanode';

const normalizeConfiguration = (response: ApiManagedConfiguration): ManagedConfiguration => ({
  ...response,
  node_id: response.node_id ?? null,
  parameters: response.parameters.map((parameter) => ({
    ...parameter,
    value: parameter.value ?? null,
    source: parameter.source ?? null,
    set_value: parameter.set_value ?? null,
    set_at: parameter.set_at ?? null,
    set_by: parameter.set_by ?? null,
    node_value: parameter.node_value ?? null,
    node_source: parameter.node_source ?? null,
  })),
});

const managedConfigurationApi = {
  get: async (nodeId: string | null = null): Promise<ManagedConfiguration> => {
    const response = await SystemManagedConfiguration.get(NODE_TYPE, nodeId ?? undefined);

    return normalizeConfiguration(response);
  },
  update: async (values: ConfigurationValuesPatch, nodeId: string | null = null): Promise<ManagedConfiguration> => {
    const response = await request(
      'PUT',
      '/system/managed_configuration',
      { node_type: NODE_TYPE, node_id: nodeId, values },
      {},
      { Accept: ['application/json'] },
    );

    return normalizeConfiguration(response);
  },
};

export default managedConfigurationApi;
