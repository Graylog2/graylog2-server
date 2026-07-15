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
import type React from 'react';

import type { InputDetailsFetcher } from './useInputDetails';

export type EventDefinitionQueryParameterType = {
  type: string;
  title: string;
  fromJSON: (json: any) => any;
  validate: (parameter: any) => Record<string, string | undefined>;
  editComponent: React.ComponentType<{
    parameter: any;
    onChange: (key: string, value: any) => void;
    identifier: string | number;
    validationState?: Record<string, ['error' | 'warning' | 'success', string] | undefined>;
  }>;
};

declare module 'graylog-web-plugin/plugin' {
  interface PluginExports {
    inputDetailsFetchers?: Array<InputDetailsFetcher>;
    eventDefinitionQueryParameterTypes?: Array<EventDefinitionQueryParameterType>;
  }
}
