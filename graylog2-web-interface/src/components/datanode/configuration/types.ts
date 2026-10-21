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

type ApiManagedConfiguration = Awaited<ReturnType<typeof SystemManagedConfiguration.get>>;
type ApiConfigurationParameter = ApiManagedConfiguration['parameters'][number];

/** Selectable values mapped to the label shown for them, like the values of an input dropdown field. */
export type ParameterValues = Record<string, string>;

export type ConfigurationSource = ApiConfigurationParameter['source'];

// The generated types mark every property as present, while the API omits the ones it has no value for.
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

/** The values to change, keyed by parameter name. A null value removes the stored value. */
export type ConfigurationValuesPatch = Record<string, string | null>;

/** The values currently shown in the form, keyed by parameter name. */
export type ConfigurationFormValues = Record<string, string>;

/** Parameters of one category, which is what a single section of the page shows. */
export type ConfigurationCategory = {
  id: string;
  label: string;
  parameters: Array<ConfigurationParameter>;
};
