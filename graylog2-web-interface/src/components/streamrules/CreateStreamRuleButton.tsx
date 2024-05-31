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
import PropTypes from 'prop-types';
import React, { useCallback, useState } from 'react';

import { Button } from 'components/bootstrap';
import type { BsSize } from 'components/bootstrap/types';
import type { StyleProps } from 'components/bootstrap/Button';
import type { StreamRule } from 'stores/streams/StreamsStore';
import { StreamRulesStore } from 'stores/streams/StreamRulesStore';
import UserNotification from 'util/UserNotification';

import StreamRuleModal from './StreamRuleModal';

type Props = {
  bsSize?: BsSize,
  bsStyle?: StyleProps,
  buttonText?: string,
  className?: string,
  streamId: string,
}

const CreateStreamRuleButton = ({ bsSize, bsStyle, buttonText, className, streamId }: Props) => {
  const [showCreateModal, setShowCreateModal] = useState(false);

  const toggleCreateModal = useCallback(() => setShowCreateModal((cur) => !cur), []);

  const onSaveStreamRule = useCallback((_streamRuleId: string, streamRule: StreamRule) => StreamRulesStore.create(streamId, streamRule, () => {
    UserNotification.success('Stream rule was created successfully.', 'Success');
    // TODO invalidate stream query
  }), [streamId]);

  return (
    <>
      <Button bsSize={bsSize}
              bsStyle={bsStyle}
              className={className}
              onClick={toggleCreateModal}>
        {buttonText}
      </Button>
      {showCreateModal && (
        <StreamRuleModal onClose={toggleCreateModal}
                         title="New Stream Rule"
                         submitButtonText="Create Rule"
                         submitLoadingText="Creating Rule..."
                         onSubmit={onSaveStreamRule} />

      )}
    </>
  );
};

CreateStreamRuleButton.propTypes = {
  buttonText: PropTypes.string,
  bsStyle: PropTypes.string,
  bsSize: PropTypes.string,
  className: PropTypes.string,
  streamId: PropTypes.string,
};

CreateStreamRuleButton.defaultProps = {
  buttonText: 'Create Rule',
  bsSize: undefined,
  bsStyle: undefined,
  className: undefined,
  streamId: undefined,
};

export default CreateStreamRuleButton;
