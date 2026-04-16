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

import { CollectorsConfig as CollectorsConfigApi } from '@graylog/server-api';

import UserNotification from 'util/UserNotification';

import { COLLECTOR_INPUT_IDS_KEY_PREFIX } from './useCollectorInputIds';

const useCollectorInputMutations = () => {
  const queryClient = useQueryClient();

  const createInputMutation = useMutation({
    mutationFn: () => CollectorsConfigApi.createInput(),
    onError: (errorThrown: unknown) => {
      UserNotification.error(
        `Creating collector ingest input failed: ${errorThrown}`,
        'Could not create collector ingest input',
      );
    },
    onSuccess: () => {
      UserNotification.success('Collector ingest input created.', 'Success!');

      return queryClient.invalidateQueries({ queryKey: COLLECTOR_INPUT_IDS_KEY_PREFIX });
    },
  });

  return {
    createCollectorInput: createInputMutation.mutateAsync,
    isCreatingCollectorInput: createInputMutation.isPending,
  };
};

export default useCollectorInputMutations;
