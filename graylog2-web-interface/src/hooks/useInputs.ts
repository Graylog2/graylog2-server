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
import { useContext } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import type { Input, ConfiguredInput } from 'components/messageloaders/Types';
import InputsContext from 'contexts/InputsContext';

const SOURCE_URL = '/system/inputs';
const INPUTS_QUERY_KEY = ['inputs', 'list'] as const;

export const fetchInputs = (): Promise<{ inputs: Array<Input>; total: number }> => fetch('GET', qualifyUrl(SOURCE_URL));

export const fetchInput = (inputId: string): Promise<Input> => fetch('GET', qualifyUrl(`${SOURCE_URL}/${inputId}`));

export const createInput = (input: ConfiguredInput): Promise<{ id: string }> => {
  const promise = fetch('POST', qualifyUrl(`${SOURCE_URL}?setup_wizard=true`), input);

  promise.then(
    () => {
      UserNotification.success(`Input '${input.title}' launched successfully`);
    },
    (error) => {
      UserNotification.error(`Launching input '${input.title}' failed with status: ${error}`, 'Could not launch input');
    },
  );

  return promise;
};

export const updateInput = (id: string, input: ConfiguredInput): Promise<void> => {
  const promise = fetch('PUT', qualifyUrl(`${SOURCE_URL}/${id}`), input);

  promise.then(
    () => {
      UserNotification.success(`Input '${input.title}' updated successfully`);
    },
    (error) => {
      UserNotification.error(`Updating input '${input.title}' failed with status: ${error}`, 'Could not update input');
    },
  );

  return promise;
};

export const deleteInput = (input: Input): Promise<void> => {
  const promise = fetch('DELETE', qualifyUrl(`${SOURCE_URL}/${input.id}`));

  promise.then(
    () => {
      UserNotification.success(`Input '${input.title}' deleted successfully`);
    },
    (error) => {
      UserNotification.error(`Deleting input '${input.title}' failed with status: ${error}`, 'Could not delete input');
    },
  );

  return promise;
};

export const inputsAsMap = (inputsList: Array<Input>): { [id: string]: Input } => {
  const inputsMap: { [id: string]: Input } = {};

  inputsList.forEach((input) => {
    inputsMap[input.id] = input;
  });

  return inputsMap;
};

const useInputsList = () =>
  useQuery({
    queryKey: INPUTS_QUERY_KEY,
    queryFn: () =>
      fetchInputs().catch((error) => {
        UserNotification.error(`Fetching Inputs failed with status: ${error}`, 'Could not retrieve Inputs');

        throw error;
      }),
    select: (data) => data.inputs,
  });

export const useCreateInput = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: ConfiguredInput) => createInput(input),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: INPUTS_QUERY_KEY });
    },
  });
};

/**
 * Context-based hook that returns the inputs map from InputsContext.
 * Preserved for backward compatibility with existing consumers.
 */
export const useInputs = () => useContext(InputsContext);

export { INPUTS_QUERY_KEY };
export default useInputsList;
