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

import type { Stream } from 'stores/streams/StreamsStore';
import { Button } from 'components/bootstrap';
import StreamRuleModal from 'components/streamrules/StreamRuleModal';
import Routes from 'routing/Routes';
import { LinkContainer } from 'components/common/router';
import { IfPermitted } from 'components/common';
import useCreateStreamRule from 'components/streamrules/hooks/useCreateStreamRule';
import StartStreamAfterRuleCreateDialog from 'components/streamrules/StartStreamAfterRuleCreateDialog';

type Props = {
  stream: Stream;
};

const RulesSectionActions = ({ stream }: Props) => {
  const [showAddRuleModal, setShowAddRuleModal] = useState(false);

  const isDefaultStream = stream.is_default;
  const isNotEditable = !stream.is_editable;

  const toggleAddRuleModal = useCallback(() => {
    setShowAddRuleModal((cur) => !cur);
  }, []);

  const {
    onCreateStreamRule,
    showStartStreamDialog,
    onCancelStartStreamDialog,
    onStartStream,
    isStartingStream,
  } = useCreateStreamRule({
    streamId: stream.id,
    streamIsPaused: stream.disabled,
  });

  return (
    <>
      <IfPermitted permissions={[`streams:edit:${stream.id}`]}>
        <LinkContainer to={Routes.stream_edit(stream.id)}>
          <Button bsStyle="link" bsSize="xsmall" disabled={isDefaultStream || isNotEditable}>
            Manage Rules
          </Button>
        </LinkContainer>
      </IfPermitted>
      <IfPermitted permissions={[`streams:edit:${stream.id}`]}>
        <Button bsStyle="info" bsSize="xsmall" disabled={isDefaultStream || isNotEditable} onClick={toggleAddRuleModal}>
          Quick add rule
        </Button>
      </IfPermitted>
      {showAddRuleModal && (
        <StreamRuleModal
          onClose={toggleAddRuleModal}
          title="New Stream Rule"
          submitButtonText="Create Rule"
          submitLoadingText="Creating Rule..."
          onSubmit={onCreateStreamRule}
        />
      )}
      <StartStreamAfterRuleCreateDialog
        show={showStartStreamDialog}
        streamTitle={stream.title}
        onConfirm={onStartStream}
        onCancel={onCancelStartStreamDialog}
        isSubmitting={isStartingStream}
      />
    </>
  );
};

export default RulesSectionActions;
