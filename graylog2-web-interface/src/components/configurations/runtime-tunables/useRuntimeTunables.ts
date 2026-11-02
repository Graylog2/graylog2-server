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

import { SystemRuntimeTunables } from '@graylog/server-api';

import { defaultOnError } from 'util/conditional/onError';

import { parseFeatureSummaries } from './parseRuntimeTunables';
import type { RuntimeTunableFeatureSummary } from './types';

export const RUNTIME_TUNABLES_KEY = ['system', 'runtime_tunables'];

const useRuntimeTunables = (): {
  features: Array<RuntimeTunableFeatureSummary>;
  isLoading: boolean;
  isError: boolean;
} => {
  const { data, isLoading, isError } = useQuery({
    queryKey: RUNTIME_TUNABLES_KEY,
    queryFn: () =>
      defaultOnError(
        SystemRuntimeTunables.list(),
        'Loading configurations failed',
        'Could not load configurations',
      ).then(parseFeatureSummaries),
  });

  return { features: data ?? [], isLoading, isError };
};

export default useRuntimeTunables;
