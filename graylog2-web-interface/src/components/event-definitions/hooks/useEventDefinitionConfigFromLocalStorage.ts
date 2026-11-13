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

import { useMemo } from 'react';

import Store from 'logic/local-storage/Store';
import useQuery from 'routing/useQuery';
import type { ParameterJson } from 'views/logic/parameters/Parameter';

export type EventDefinitionLocalStorageConfig = {
  type;
  query?: string;
  streams?: Array<string>;
  search_within_ms?: number;
  group_by?: Array<string>;
  agg_function?: string;
  agg_field?: string;
  agg_value?: string | number;
  // How the aggregation compares to agg_value; the form asks for it when it is missing.
  agg_operator?: '>' | '>=' | '<' | '<=' | '==';
  loc_query_parameters?: Array<ParameterJson>;
  execute_every_ms?: number;
  title?: string;
  description?: string;
  priority?: number;
};

export type EventDefinitionDetailsFromLocalStorage = Partial<{ title: string; description: string; priority: number }>;

type EventDefinitionConfigFromUrl = {
  type: string;
  query: string;
  query_parameters: ParameterJson[];
  streams: string[];
  group_by?: string[];
  series?: Array<{ id: string; function: string; field: string }>;
  search_within_ms: number;
  execute_every_ms?: number;
  conditions?: {
    expression: { left: any; right: any; expr: any };
  };
};

const useEventDefinitionConfigFromLocalStorage = (): {
  hasLocalStorageConfig: boolean;
  configFromLocalStorage: EventDefinitionConfigFromUrl;
  detailsFromLocalStorage?: EventDefinitionDetailsFromLocalStorage;
} => {
  const { 'session-id': sessionId } = useQuery();

  return useMemo(() => {
    const parsedLocalStorageConfig = Store.get<EventDefinitionLocalStorageConfig>(sessionId as string);
    if (!parsedLocalStorageConfig) return { hasLocalStorageConfig: false, configFromLocalStorage: undefined };
    Store.delete(sessionId as string);

    const {
      type,
      query,
      streams,
      search_within_ms,
      group_by,
      agg_function,
      agg_field,
      agg_value,
      agg_operator,
      loc_query_parameters,
      execute_every_ms,
      title,
      description,
      priority,
    } = parsedLocalStorageConfig;

    const details = Object.entries({ title, description, priority }).filter(([, value]) => value !== undefined);
    const aggData =
      agg_function && agg_value !== undefined && agg_value !== null && agg_value !== ''
        ? {
            conditions: {
              expression: {
                expr: agg_operator,
                left: { expr: 'number-ref', ref: `${agg_function}-${agg_field}` },
                right: { expr: 'number', value: agg_value },
              },
            },
            series: [{ id: `${agg_function}-${agg_field}`, function: agg_function, field: agg_field }],
            group_by: group_by || [],
          }
        : {};

    return {
      hasLocalStorageConfig: true,
      configFromLocalStorage: {
        type,
        query: query ?? '',
        streams: streams ?? [],
        search_within_ms,
        group_by: group_by ?? [],
        query_parameters: loc_query_parameters ?? [],
        ...(execute_every_ms ? { execute_every_ms } : {}),
        ...aggData,
      },
      detailsFromLocalStorage: details.length ? Object.fromEntries(details) : undefined,
    };
  }, [sessionId]);
};

export default useEventDefinitionConfigFromLocalStorage;
