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
import { keepPreviousData, useQuery } from '@tanstack/react-query';

import { ClusterInputState } from '@graylog/server-api';

import { defaultOnError } from 'util/conditional/onError';
import type { InputSummary } from 'hooks/usePaginatedInputs';

type Options = {
  enabled: boolean;
};

export type InputState = 'RUNNING' | 'FAILED' | 'STOPPED' | 'STARTING' | 'FAILING' | 'SETUP';
export const INPUT_STATES_QUERY_KEY = ['inputs', 'states'];
export type InputStateByNode = {
  [nodeId: string]: {
    state: InputState;
    id: string;
    detailed_message: string | null;
    last_failed_at?: string | null;
    message_input: InputSummary;
  };
};

export type InputStates = {
  [inputId: string]: InputStateByNode;
};

export const fetchInputStates = async (): Promise<InputStates> => {
  const response = await ClusterInputState.get();
  const result = {} as InputStates;

  Object.keys(response).forEach((node) => {
    if (!response[node]) {
      return;
    }

    response[node].forEach((input) => {
      if (!result[input.id]) {
        result[input.id] = {};
      }

      result[input.id][node] = { ...input, state: input.state as InputState };
    });
  });

  return result;
};

const useInputsStates = (
  { enabled }: Options = { enabled: true },
): {
  data: InputStates;
  refetch: () => void;
  isLoading: boolean;
} => {
  const { data, refetch, isLoading } = useQuery<InputStates, Error>({
    queryKey: INPUT_STATES_QUERY_KEY,
    queryFn: () =>
      defaultOnError<InputStates>(
        fetchInputStates(),
        'Loading input states failed with status',
        'Could not load states types',
      ),
    placeholderData: keepPreviousData,
    refetchInterval: 2000,
    retry: false,
    enabled,
  });

  return { data: data, refetch, isLoading };
};

export default useInputsStates;
