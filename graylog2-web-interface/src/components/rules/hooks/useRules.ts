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
import { useQuery } from '@tanstack/react-query';

import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import UserNotification from 'util/UserNotification';
import { qualifyUrl } from 'util/URLUtils';
import PaginationURL from 'util/PaginationURL';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import type { Pagination, PaginatedListJSON, ListPagination } from 'stores/PaginationTypes';
import type FetchError from 'logic/errors/FetchError';
import type { RuleBuilderType, BlockDict } from 'components/rules/rule-builder/types';

export type RuleType = {
  id?: string;
  source: string;
  title: string;
  description: string;
  created_at: string;
  modified_at: string;
  rule_builder: RuleBuilderType;
  errors?: [];
  simulator_message?: string;
  _scope?: string;
};
export type MetricsConfigType = {
  metrics_enabled: boolean;
};
export type PipelineSummary = {
  id: string;
  title: string;
};
export type RulesContext = {
  used_in_pipelines: {
    [id: string]: Array<PipelineSummary>;
  };
};
export type PaginatedRulesResponse = PaginatedListJSON & {
  rules: Array<RuleType>;
  context: RulesContext;
};

export type PaginatedRules = {
  list: Array<RuleType>;
  context: RulesContext;
  pagination: ListPagination;
};

// Root key for rule entity data (list / single rule). Broad invalidation of this key
// must NOT refetch the function descriptors or the metrics config, which have their own
// root keys below.
export const RULES_QUERY_KEY = ['rules'] as const;

// Function descriptors are static-ish reference data, not rule entity data, so they get
// their own root key and are never refetched on rule mutations.
export const RULE_FUNCTION_DESCRIPTORS_QUERY_KEY = ['rule-function-descriptors'] as const;

// Metrics config is separate configuration, not rule entity data, so it gets its own
// root key and is never refetched on rule mutations.
export const RULE_METRICS_CONFIG_QUERY_KEY = ['rule-metrics-config'] as const;

export const fetchRules = (): Promise<Array<RuleType>> => {
  const url = qualifyUrl(ApiRoutes.RulesController.list().url);

  return fetch('GET', url).then(
    (response: Array<RuleType>) => response,
    (error: Error) => {
      UserNotification.error(
        `Fetching rules failed with status: ${error.message}`,
        'Could not retrieve processing rules',
      );

      throw error;
    },
  );
};

export const fetchRulesPaginated = ({ page, perPage, query }: Pagination): Promise<PaginatedRules> => {
  const url = PaginationURL(ApiRoutes.RulesController.paginatedList().url, page, perPage, query);

  return fetch('GET', qualifyUrl(url)).then(
    (response: PaginatedRulesResponse) => ({
      list: response.rules,
      context: response.context,
      pagination: {
        count: response.count,
        total: response.total,
        page: response.page,
        perPage: response.per_page,
        query: response.query,
      },
    }),
    (error: FetchError) => {
      if (!error.additional || error.additional.status !== 404) {
        UserNotification.error(`Loading rules list failed with status: ${error}`, 'Could not load rules.');
      }

      throw error;
    },
  );
};

export const getRule = (ruleId: string): Promise<RuleType> => {
  const url = qualifyUrl(ApiRoutes.RulesController.get(ruleId).url);

  return fetch('GET', url).then(
    (response: RuleType) => response,
    (error: Error) => {
      UserNotification.error(
        `Fetching rule "${ruleId}" failed with status: ${error.message}`,
        `Could not retrieve processing rule "${ruleId}"`,
      );

      throw error;
    },
  );
};

export const saveRule = (ruleSource: RuleType): Promise<RuleType> => {
  const url = qualifyUrl(ApiRoutes.RulesController.create().url);
  const rule = {
    title: ruleSource.title,
    description: ruleSource.description,
    source: ruleSource.source,
    simulator_message: ruleSource.simulator_message,
  };

  return fetch('POST', url, rule).then(
    (response: RuleType) => {
      UserNotification.success(`Rule "${response.title}" created successfully`);

      return response;
    },
    (error: Error) => {
      UserNotification.error(
        `Saving rule "${ruleSource.title}" failed with status: ${error.message}`,
        `Could not save processing rule "${ruleSource.title}"`,
      );

      throw error;
    },
  );
};

export const updateRule = (ruleSource: RuleType): Promise<RuleType> => {
  const url = qualifyUrl(ApiRoutes.RulesController.update(ruleSource.id).url);
  const rule = {
    id: ruleSource.id,
    title: ruleSource.title,
    description: ruleSource.description,
    source: ruleSource.source,
    simulator_message: ruleSource.simulator_message,
  };

  return fetch('PUT', url, rule).then(
    (response: RuleType) => {
      UserNotification.success(`Rule "${response.title}" updated successfully`);

      return response;
    },
    (error: Error) => {
      UserNotification.error(
        `Updating rule "${ruleSource.title}" failed with status: ${error.message}`,
        `Could not update processing rule "${ruleSource.title}"`,
      );

      throw error;
    },
  );
};

export const deleteRule = (rule: RuleType): Promise<void> => {
  const url = qualifyUrl(ApiRoutes.RulesController.delete(rule.id).url);

  return fetch('DELETE', url).then(
    () => {
      UserNotification.success(`Rule "${rule.title}" was deleted successfully`);
    },
    (error: Error) => {
      UserNotification.error(
        `Deleting rule "${rule.title}" failed with status: ${error.message}`,
        `Could not delete processing rule "${rule.title}"`,
      );

      throw error;
    },
  );
};

export type RuleParseError = {
  line: number;
  position_in_line: number;
  reason: string;
};

// Promise-returning replacement for the old callback-based `RulesActions.parse`.
// Resolves with `[]` on a successful parse (no errors), or with the returned parse
// errors on a 400. Other errors reject.
export const parseRule = (ruleSource: RuleType): Promise<Array<RuleParseError>> => {
  const url = qualifyUrl(ApiRoutes.RulesController.parse().url);
  const rule = {
    title: ruleSource.title,
    description: ruleSource.description,
    source: ruleSource.source,
  };

  return fetch('POST', url, rule).then(
    (): Array<RuleParseError> => [],
    (error: { status?: number; additional?: { body?: Array<RuleParseError> } }) => {
      // a Bad Request indicates a parse error, return all the returned errors so the
      // caller can set them in the editor
      if (error.status === 400) {
        return error.additional.body;
      }

      throw error;
    },
  );
};

// Promise-returning replacement for the old callback-based `RulesActions.simulate`.
// Resolves with the simulation result. Rejects on error so callers can decide whether to
// handle it; the old store passed an empty error callback, so callers swallow the rejection.
type SimulateRuleRequest = {
  message: string;
  rule_source?: {
    title: string;
    description: string;
    source: string;
  };
  rule_builder_dto?: {
    title: string;
    description: string;
    rule_builder: RuleBuilderType;
  };
};

export const simulateRule = (message: string, ruleToSimulate: RuleType): Promise<unknown> => {
  const url = qualifyUrl(
    ruleToSimulate?.rule_builder
      ? ApiRoutes.RuleBuilderController.simulate().url
      : ApiRoutes.RulesController.simulate().url,
  );
  const rule: SimulateRuleRequest = {
    message,
    rule_source: undefined,
    rule_builder_dto: undefined,
  };

  if (ruleToSimulate?.rule_builder) {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { source, ...ruleBuilderRuleToSimulate } = ruleToSimulate;

    rule.rule_builder_dto = {
      title: ruleBuilderRuleToSimulate.title,
      description: ruleBuilderRuleToSimulate.description,
      rule_builder: ruleBuilderRuleToSimulate.rule_builder,
    };
  } else {
    rule.rule_source = {
      title: ruleToSimulate.title,
      description: ruleToSimulate.description,
      source: ruleToSimulate.source,
    };
  }

  return fetch('POST', url, rule);
};

export const fetchMultipleRules = (ruleNames: Array<string>): Promise<unknown> => {
  const url = qualifyUrl(ApiRoutes.RulesController.multiple().url);

  return fetch('POST', url, { rules: ruleNames });
};

export const fetchRuleFunctionDescriptors = (): Promise<Array<BlockDict>> => {
  const url = qualifyUrl(ApiRoutes.RulesController.functions().url);

  return fetch('GET', url).then((functions: Array<BlockDict>) =>
    functions ? [...functions].sort((fn1, fn2) => naturalSort(fn1.name, fn2.name)) : functions,
  );
};

export const fetchRuleMetricsConfig = (): Promise<MetricsConfigType> => {
  const url = qualifyUrl(ApiRoutes.RulesController.metricsConfig().url);

  return fetch('GET', url).then(
    (response: MetricsConfigType) => response,
    (error: Error) => {
      UserNotification.error(
        `Couldn't load rule metrics config: ${error.message}`,
        "Couldn't load rule metrics config",
      );

      throw error;
    },
  );
};

export const updateRuleMetricsConfig = (nextConfig: MetricsConfigType): Promise<MetricsConfigType> => {
  const url = qualifyUrl(ApiRoutes.RulesController.metricsConfig().url);

  return fetch('PUT', url, nextConfig).then(
    (response: MetricsConfigType) => {
      UserNotification.success('Successfully updated rule metrics config');

      return response;
    },
    (error: Error) => {
      UserNotification.error(
        `Couldn't update rule metrics config: ${error.message}`,
        "Couldn't update rule metrics config",
      );

      throw error;
    },
  );
};

export const useRules = () =>
  useQuery({
    queryKey: RULES_QUERY_KEY,
    queryFn: fetchRules,
    retry: false,
  });

export const useRulesPaginated = (pagination: Pagination) =>
  useQuery({
    queryKey: [...RULES_QUERY_KEY, 'paginated', pagination],
    queryFn: () => fetchRulesPaginated(pagination),
    retry: false,
  });

export const useRule = (ruleId: string, { enabled = true }: { enabled?: boolean } = {}) =>
  useQuery({
    queryKey: [...RULES_QUERY_KEY, 'rule', ruleId],
    queryFn: () => getRule(ruleId),
    retry: false,
    enabled,
  });

export const useRuleFunctionDescriptors = () =>
  useQuery({
    queryKey: RULE_FUNCTION_DESCRIPTORS_QUERY_KEY,
    queryFn: fetchRuleFunctionDescriptors,
    staleTime: Infinity,
  });

export const useRuleMetricsConfig = ({ enabled = true }: { enabled?: boolean } = {}) =>
  useQuery({
    queryKey: RULE_METRICS_CONFIG_QUERY_KEY,
    queryFn: fetchRuleMetricsConfig,
    retry: false,
    enabled,
  });
