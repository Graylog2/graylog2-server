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

import { CollectorsConfig as CollectorsConfigApi, SystemInputs } from '@graylog/server-api';

import UserNotification from 'util/UserNotification';

import { COLLECTOR_INPUT_IDS_KEY_PREFIX } from './useCollectorInputIds';

// The parts of an input summary needed to send it back as an update request.
type MovableInput = {
  id: string;
  title: string;
  type: string;
  global: boolean;
  node?: string;
  attributes?: Record<string, unknown>;
};

const useCollectorInputMutations = () => {
  const queryClient = useQueryClient();

  const createInputMutation = useMutation({
    mutationFn: () => CollectorsConfigApi.createInput(),
    onError: (errorThrown: unknown) => {
      UserNotification.error(
        `Creating Collector ingest input failed: ${errorThrown}`,
        'Could not create Collector ingest input',
      );
    },
    onSuccess: () => {
      UserNotification.success('Collector ingest input created.', 'Success!');

      return queryClient.invalidateQueries({ queryKey: COLLECTOR_INPUT_IDS_KEY_PREFIX });
    },
  });

  // Moves an existing collector ingest input to another port. The rest of the input's configuration is kept
  // as-is; the server restarts the input on update.
  const updateInputPortMutation = useMutation({
    mutationFn: ({ input, port }: { input: MovableInput; port: number }) =>
      SystemInputs.update(
        {
          title: input.title,
          type: input.type,
          global: input.global,
          node: input.node,
          configuration: { ...input.attributes, port },
        },
        input.id,
      ),
    onError: (errorThrown: unknown) => {
      UserNotification.error(
        `Updating Collector ingest input failed: ${errorThrown}`,
        'Could not update Collector ingest input',
      );
    },
    onSuccess: () => {
      UserNotification.success('Collector ingest input moved to the new port.', 'Success!');

      return Promise.all([
        queryClient.invalidateQueries({ queryKey: ['inputs'] }),
        queryClient.invalidateQueries({ queryKey: COLLECTOR_INPUT_IDS_KEY_PREFIX }),
      ]);
    },
  });

  return {
    createCollectorInput: createInputMutation.mutateAsync,
    isCreatingCollectorInput: createInputMutation.isPending,
    updateCollectorInputPort: updateInputPortMutation.mutateAsync,
    isUpdatingCollectorInputPort: updateInputPortMutation.isPending,
  };
};

export default useCollectorInputMutations;
