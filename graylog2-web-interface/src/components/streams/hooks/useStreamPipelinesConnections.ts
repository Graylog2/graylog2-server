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

import i18n from 'i18n';
import ApiRoutes from 'routing/ApiRoutes';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';

export type SaveStreamPipelinesConnectionProps = {
  pipelineIds: Array<string>;
  streamId: string;
};

const saveStreamPipelinesConnection = ({ pipelineIds, streamId }: SaveStreamPipelinesConnectionProps) =>
  fetch('POST', qualifyUrl(ApiRoutes.ConnectionsController.to_stream().url), {
    pipeline_ids: pipelineIds,
    stream_id: streamId,
  });

const useStreamPipelinesConnectionMutation = (): {
  onSaveStreamPipelinesConnection: (steamPipelinesConnectionProps: SaveStreamPipelinesConnectionProps) => Promise<void>;
} => {
  const queryClient = useQueryClient();

  const { mutateAsync: onSaveStreamPipelinesConnection } = useMutation({
    mutationFn: saveStreamPipelinesConnection,

    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['stream', 'pipelines', 'connections'],
      });

      UserNotification.success(
        i18n.t('pipelines_connection_form.save_success', { ns: 'streams' }),
        i18n.t('pipelines_connection_form.save_title', { ns: 'streams' }),
      );
    },

    onError: (errorThrown) => {
      UserNotification.error(
        i18n.t('pipelines_connection_form.save_failed', { ns: 'streams', error: errorThrown }),
        i18n.t('pipelines_connection_form.save_title', { ns: 'streams' }),
      );
    },
  });

  return { onSaveStreamPipelinesConnection };
};

export default useStreamPipelinesConnectionMutation;
