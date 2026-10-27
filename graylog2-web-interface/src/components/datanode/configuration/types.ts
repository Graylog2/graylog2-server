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
import type { SystemManagedConfiguration } from '@graylog/server-api';

export type ApiManagedConfiguration = Awaited<ReturnType<typeof SystemManagedConfiguration.get>>;
type ApiConfigurationParameter = ApiManagedConfiguration['parameters'][number];

export type ParameterValues = Record<string, string>;

export type ConfigurationSource = ApiConfigurationParameter['source'];

export type ConfigurationParameter = Omit<
  ApiConfigurationParameter,
  'value' | 'source' | 'set_value' | 'set_at' | 'set_by' | 'node_value' | 'node_source'
> & {
  value: string | null;
  source: ConfigurationSource | null;
  set_value: string | null;
  set_at: string | null;
  set_by: string | null;
  node_value: string | null;
  node_source: ConfigurationSource | null;
};

export type ManagedConfiguration = Omit<ApiManagedConfiguration, 'node_id' | 'parameters'> & {
  node_id: string | null;
  parameters: Array<ConfigurationParameter>;
};

export type ConfigurationValuesPatch = Record<string, string | null>;

export type ConfigurationFormValues = Record<string, string | null>;

export type ConfigurationCategory = {
  id: string;
  label: string;
  parameters: Array<ConfigurationParameter>;
};
