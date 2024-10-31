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
import * as React from 'react';
import { useCallback, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';

import { Button } from 'components/bootstrap';
import type { BsSize } from 'components/bootstrap/types';
import type { StyleProps } from 'components/bootstrap/Button';
import type { StreamRule } from 'stores/streams/StreamsStore';
import { StreamRulesStore } from 'stores/streams/StreamRulesStore';
import UserNotification from 'util/UserNotification';
import { IfPermitted } from 'components/common';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

import StreamRuleModal from './StreamRuleModal';

type Props = {
  bsSize?: BsSize,
  bsStyle?: StyleProps,
  buttonText?: string,
  className?: string,
  disabled?: boolean,
  streamId?: string
}

const CreateStreamRuleButton = ({ bsSize, bsStyle, buttonText = 'Create Rule', className, disabled = false, streamId }: Props) => {
  const [showCreateModal, setShowCreateModal] = useState(false);
  const queryClient = useQueryClient();
  const toggleCreateModal = useCallback(() => setShowCreateModal((cur) => !cur), []);
  const sendTelemetry = useSendTelemetry();

  const onSaveStreamRule = useCallback((_streamRuleId: string, streamRule: StreamRule) => StreamRulesStore.create(streamId, streamRule, () => {
    UserNotification.success('Stream rule was created successfully.', 'Success');
    queryClient.invalidateQueries(['stream', streamId]);
  }), [streamId, queryClient]);

  const onCreateStreamRule = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.STREAMS.STREAM_ITEM_DATA_ROUTING_INTAKE_CREATE_RULE_OPENED, {
      app_pathname: 'streams',
    });

    toggleCreateModal();
  };

  return (
    <IfPermitted permissions={`streams:edit:${streamId}`}>
      <Button bsSize={bsSize}
              bsStyle={bsStyle}
              disabled={disabled}
              className={className}
              onClick={onCreateStreamRule}>
        {buttonText}
      </Button>
      {showCreateModal && (
        <StreamRuleModal onClose={toggleCreateModal}
                         title="New Stream Rule"
                         submitButtonText="Create Rule"
                         submitLoadingText="Creating Rule..."
                         onSubmit={onSaveStreamRule} />

      )}

    </IfPermitted>
  );
};

export default CreateStreamRuleButton;
