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
import { useCallback, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';

import StreamsStore from 'stores/streams/StreamsStore';
import type { StreamRule } from 'stores/streams/StreamsStore';
import { StreamRulesStore } from 'stores/streams/StreamRulesStore';
import UserNotification from 'util/UserNotification';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

type CreateStreamRuleHandler = (streamRuleId: string | undefined | null, streamRule: StreamRule) => Promise<void>;

type Props = {
  streamId: string;
  streamIsPaused: boolean;
};

type Result = {
  onCreateStreamRule: CreateStreamRuleHandler;
  showStartStreamDialog: boolean;
  onCancelStartStreamDialog: () => void;
  onStartStream: () => Promise<void>;
  isStartingStream: boolean;
};

const useCreateStreamRule = ({ streamId, streamIsPaused }: Props): Result => {
  const [showStartStreamDialog, setShowStartStreamDialog] = useState(false);
  const [isStartingStream, setIsStartingStream] = useState(false);
  const queryClient = useQueryClient();
  const sendTelemetry = useSendTelemetry();

  const onStreamRuleCreated = useCallback(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.STREAMS.STREAM_ITEM_RULE_SAVED, {
      app_pathname: 'streams',
      app_action_value: 'stream-item-rule',
    });
    UserNotification.success('Stream rule was created successfully.', 'Success');
    queryClient.invalidateQueries({
      queryKey: ['stream', streamId],
    });

    if (streamIsPaused) {
      setShowStartStreamDialog(true);
    }
  }, [queryClient, sendTelemetry, streamId, streamIsPaused]);

  const onCreateStreamRule = useCallback<CreateStreamRuleHandler>(
    (_streamRuleId, streamRule) => StreamRulesStore.create(streamId, streamRule, onStreamRuleCreated),
    [onStreamRuleCreated, streamId],
  );

  const onCancelStartStreamDialog = useCallback(() => {
    setShowStartStreamDialog(false);
  }, []);

  const onStartStream = useCallback(async () => {
    setIsStartingStream(true);

    try {
      await StreamsStore.resume(streamId, () => {
        setShowStartStreamDialog(false);
      });
    } finally {
      setIsStartingStream(false);
    }
  }, [streamId]);

  return {
    onCreateStreamRule,
    showStartStreamDialog,
    onCancelStartStreamDialog,
    onStartStream,
    isStartingStream,
  };
};

export default useCreateStreamRule;
