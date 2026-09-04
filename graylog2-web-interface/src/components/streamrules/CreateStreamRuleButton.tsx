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

import { Button } from 'components/bootstrap';
import type { BsSize } from 'components/bootstrap/types';
import type { StyleProps } from 'components/bootstrap/Button';
import { IfPermitted } from 'components/common';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useCreateStreamRule from 'components/streamrules/hooks/useCreateStreamRule';
import StartStreamAfterRuleCreateDialog from 'components/streamrules/StartStreamAfterRuleCreateDialog';

import StreamRuleModal from './StreamRuleModal';

type Props = {
  bsSize?: BsSize;
  bsStyle?: StyleProps;
  buttonText?: string;
  className?: string;
  disabled?: boolean;
  streamId: string;
  streamTitle?: string;
  streamIsPaused?: boolean;
};

const CreateStreamRuleButton = ({
  bsSize = undefined,
  bsStyle = undefined,
  buttonText = 'Create Rule',
  className = undefined,
  disabled = false,
  streamId,
  streamTitle = undefined,
  streamIsPaused = false,
}: Props) => {
  const [showCreateModal, setShowCreateModal] = useState(false);
  const toggleCreateModal = useCallback(() => setShowCreateModal((cur) => !cur), []);
  const sendTelemetry = useSendTelemetry();
  const {
    onCreateStreamRule: onSaveStreamRule,
    showStartStreamDialog,
    onCancelStartStreamDialog,
    onStartStream,
    isStartingStream,
  } = useCreateStreamRule({
    streamId,
    streamIsPaused,
  });

  const openCreateStreamRuleModal = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.STREAMS.STREAM_ITEM_DATA_ROUTING_INTAKE_CREATE_RULE_OPENED, {
      app_pathname: 'streams',
    });

    toggleCreateModal();
  };

  return (
    <IfPermitted permissions={`streams:edit:${streamId}`}>
      <Button
        bsSize={bsSize}
        bsStyle={bsStyle}
        disabled={disabled}
        className={className}
        onClick={openCreateStreamRuleModal}>
        {buttonText}
      </Button>
      {showCreateModal && (
        <StreamRuleModal
          onClose={toggleCreateModal}
          title="New Stream Rule"
          submitButtonText="Create Rule"
          submitLoadingText="Creating Rule..."
          onSubmit={onSaveStreamRule}
        />
      )}
      <StartStreamAfterRuleCreateDialog
        show={showStartStreamDialog}
        streamTitle={streamTitle}
        onConfirm={onStartStream}
        onCancel={onCancelStartStreamDialog}
        isSubmitting={isStartingStream}
      />
    </IfPermitted>
  );
};

export default CreateStreamRuleButton;
