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
import React, { useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';

import {
  useRuleMetricsConfig,
  updateRuleMetricsConfig,
  RULE_METRICS_CONFIG_QUERY_KEY,
} from 'components/rules/hooks/useRules';
import type { MetricsConfigType } from 'components/rules/hooks/useRules';

import RuleMetricsConfig from './RuleMetricsConfig';

type Props = {
  onClose?: (...args: any[]) => void;
};

const RuleMetricsConfigContainer = ({ onClose = () => {} }: Props) => {
  const queryClient = useQueryClient();
  const { data: metricsConfig } = useRuleMetricsConfig();

  const handleChange = useCallback(
    (nextConfig: MetricsConfigType) =>
      updateRuleMetricsConfig(nextConfig).then((response) => {
        queryClient.invalidateQueries({ queryKey: RULE_METRICS_CONFIG_QUERY_KEY });

        return response;
      }),
    [queryClient],
  );

  if (!metricsConfig) {
    return null;
  }

  return <RuleMetricsConfig config={metricsConfig} onChange={handleChange} onClose={onClose} />;
};

export default RuleMetricsConfigContainer;
