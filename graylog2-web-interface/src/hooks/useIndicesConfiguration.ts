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

import { SystemIndicesRotation, SystemIndicesRetention } from '@graylog/server-api';

import type { RotationStrategyResponse, RetentionStrategyResponse } from 'components/indices/Types';
import { defaultOnError } from 'util/conditional/onError';
import i18n from 'i18n';

type Options = {
  enabled?: boolean;
};

const QUERY_KEY_ROTATION = ['system', 'indices', 'rotation', 'strategies'];
const QUERY_KEY_RETENTION = ['system', 'indices', 'retention', 'strategies'];

const useIndicesConfiguration = ({ enabled = true }: Options = {}) => {
  const { data: rotationData, isInitialLoading: isLoadingRotation } = useQuery<RotationStrategyResponse>({
    queryKey: QUERY_KEY_ROTATION,
    queryFn: () =>
      defaultOnError(
        SystemIndicesRotation.list() as unknown as Promise<RotationStrategyResponse>,
        i18n.t('use_indices_configuration_notifications.fetch_rotation_failed', { ns: 'indexsets' }),
        i18n.t('use_indices_configuration_notifications.fetch_rotation_failed_title', { ns: 'indexsets' }),
      ),
    enabled,
  });

  const { data: retentionData, isInitialLoading: isLoadingRetention } = useQuery<RetentionStrategyResponse>({
    queryKey: QUERY_KEY_RETENTION,
    queryFn: () =>
      defaultOnError(
        SystemIndicesRetention.list() as unknown as Promise<RetentionStrategyResponse>,
        i18n.t('use_indices_configuration_notifications.fetch_retention_failed', { ns: 'indexsets' }),
        i18n.t('use_indices_configuration_notifications.fetch_retention_failed_title', { ns: 'indexsets' }),
      ),
    enabled,
  });

  return {
    rotationStrategies: rotationData?.strategies,
    retentionStrategies: retentionData?.strategies,
    retentionStrategiesContext: retentionData?.context,
    rotationStrategiesContext: rotationData?.context,
    isLoading: isLoadingRotation || isLoadingRetention,
  };
};

export default useIndicesConfiguration;
