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

export const INPUTS_STATES_QUERY_KEY = ['inputs_states'];

export type InputState = 'RUNNING' | 'FAILED' | 'STOPPED' | 'STARTING' | 'FAILING' | 'SETUP';

export type InputSummary = {
  creator_user_id: string;
  node: string;
  name: string;
  created_at: string;
  global: boolean;
  attributes: {
    [key: string]: object;
  };
  id: string;
  title: string;
  type: string;
  content_pack: string;
  static_fields: {
    [key: string]: string;
  };
};
export type InputStateSummary = {
  detailed_message: string;
  started_at: string;
  id: string;
  state: string;
  message_input: InputSummary;
};
export type InputStatesList = {
  states: Array<InputStateSummary>;
};

type Options = {
  enabled: boolean;
};

const useInputsStates = (
  { enabled }: Options = { enabled: true },
): {
  data: InputStatesList | undefined;
  refetch: () => void;
  isLoading: boolean;
} => {
  const { data, refetch, isLoading } = useQuery({
    queryKey: INPUTS_STATES_QUERY_KEY,

    queryFn: () =>
      defaultOnError(
        SystemInputStates.list(),
        'Loading inputs states failed with status',
        'Could not load inputs states',
      ),
    enabled,
  });

  return {
    data: data || { states: [] },
    refetch,
    isLoading,
  };
};

export default useInputsStates;
