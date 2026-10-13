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
import type { SystemConfigurationOverrides } from '@graylog/server-api';

type ApiConfigurationOverrides = Awaited<ReturnType<typeof SystemConfigurationOverrides.get>>;

export type ConfigurationOverrideValue = ApiConfigurationOverrides['overrides'][string];

// The generated API types currently omit nullability for node IDs and removal values.
export type ConfigurationOverrides = Omit<ApiConfigurationOverrides, 'node_type' | 'node_id'> & {
  node_type: 'datanode';
  node_id: string | null;
};

export type ConfigurationOverridesPatch = Record<string, string | null>;

export type ConfigurationFormValues = Record<string, string>;

export type ConfigurationQuantityKind = 'disk' | 'memory' | 'bandwidth' | 'heap';

type ConfigurationFieldBase = {
  key: string;
  group: string;
  label: string;
  description: string;
  requiresRestart: boolean;
  validate: (value: string) => string | undefined;
};

export type ConfigurationField = ConfigurationFieldBase &
  (
    | { type: 'number' | 'heap' }
    | { type: 'quantity'; quantityKind: ConfigurationQuantityKind }
    | { type: 'roles'; options: Array<{ value: string; label: string }> }
  );

export type ConfigurationGroup = {
  id: string;
  label: string;
  description: string;
};
