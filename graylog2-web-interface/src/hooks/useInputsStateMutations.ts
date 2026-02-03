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
import { useMutation, useQueryClient } from '@tanstack/react-query';

import { SystemInputStates } from '@graylog/server-api';

import UserNotification from 'util/UserNotification';
import type { InputSummary } from 'hooks/usePaginatedInputs';
import { INPUT_STATES_QUERY_KEY } from 'hooks/useInputsStates';

type PROPS = {
  inputId: string;
};

const startInput = async ({ inputId }: PROPS): Promise<{ id: string }> => SystemInputStates.start(inputId);
const stopInput = async ({ inputId }: PROPS): Promise<{ id: string }> => SystemInputStates.stop(inputId);
const setupInput = async ({ inputId }: PROPS): Promise<{ id: string }> => SystemInputStates.setup(inputId);

const useInputStateMutations = (input: InputSummary) => {
  const queryClient = useQueryClient();

  const startMutation = useMutation({
    mutationFn: startInput,
    onError: (error) => {
      UserNotification.error(
        `Error starting input '${input.title}': ${error}`,
        `Input '${input.title}' could not be started`,
      );
    },
    onSuccess: () => {
      UserNotification.success('Input has been successfully started.', 'Success!');
      queryClient.invalidateQueries({ queryKey: INPUT_STATES_QUERY_KEY });
    },
  });
  const stopMutation = useMutation({
    mutationFn: stopInput,
    onError: (error) => {
      UserNotification.error(
        `Error stopping input '${input.title}': ${error}`,
        `Input '${input.title}' could not be stopped`,
      );
    },
    onSuccess: () => {
      UserNotification.success('Input has been successfully stopped.', 'Success!');
      queryClient.invalidateQueries({ queryKey: INPUT_STATES_QUERY_KEY });
    },
  });
  const setupMutation = useMutation({
    mutationFn: setupInput,
    onError: (error) => {
      UserNotification.error(
        `Error entering setup mode '${input.title}': ${error}`,
        `Input '${input.title}' could not set to setup mode`,
      );
    },
    onSuccess: () => {
      UserNotification.success('Input has been successfully deleted.', 'Success!');
      queryClient.invalidateQueries({ queryKey: INPUT_STATES_QUERY_KEY });
    },
  });

  return {
    startInput: startMutation.mutateAsync,
    stopInput: stopMutation.mutateAsync,
    setupInput: setupMutation.mutateAsync,
  };
};

export default useInputStateMutations;
