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

import { SystemInputStates } from '@graylog/server-api';

import { defaultOnError } from 'util/conditional/onError';

type StateSummary = { [inputId: string]: Array<string> };

const PROBLEMATIC_STATES = new Set(['FAILED', 'FAILING', 'SETUP']);

const fetchStateSummary = (): Promise<StateSummary> =>
  SystemInputStates.summary();

const useInputStateSummary = (): { hasProblematicInputs: boolean; isLoading: boolean } => {
  const { data, isLoading } = useQuery<StateSummary>({
    queryKey: ['inputs', 'state-summary'],
    queryFn: () =>
      defaultOnError(
        fetchStateSummary(),
        'Loading input state summary failed with status',
        'Could not load input state summary.',
      ),
    refetchInterval: 5000,
    retry: false,
  });

  const hasProblematicInputs = data
    ? Object.values(data).some((states) => states.some((s) => PROBLEMATIC_STATES.has(s)))
    : false;

  return { hasProblematicInputs, isLoading };
};

export default useInputStateSummary;
