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
import StreamModal from 'components/streams/StreamModal';
import type { Stream } from 'stores/streams/StreamsStore';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

type Props = {
  bsSize?: string
  bsStyle?: string,
  buttonText?: string,
  className?: string,
  indexSets: Array<IndexSet>
  onCreate: (values: Partial<Stream>) => Promise<void>
}

const CreateStreamButton = ({ bsSize, bsStyle, buttonText, className, indexSets, onCreate }: Props) => {
  const [showCreateModal, setShowCreateModal] = useState(false);
  const sendTelemetry = useSendTelemetry();

  const toggleCreateModal = useCallback(() => {
    sendTelemetry('click', {
      app_pathname: 'streams',
      app_action_value: 'stream-create-button',
    });

    return setShowCreateModal((cur) => !cur);
  }, [sendTelemetry]);

  return (
    <>
      <Button bsSize={bsSize}
              bsStyle={bsStyle}
              className={className}
              onClick={toggleCreateModal}>
        {buttonText}
      </Button>
      {showCreateModal && (
        <StreamModal title="Create stream"
                     submitButtonText="Create stream"
                     submitLoadingText="Creating stream..."
                     indexSets={indexSets}
                     onSubmit={onCreate}
                     onClose={toggleCreateModal} />
      )}
    </>
  );
};

CreateStreamButton.propTypes = {
  buttonText: PropTypes.string,
  bsStyle: PropTypes.string,
  bsSize: PropTypes.string,
  className: PropTypes.string,
  onCreate: PropTypes.func.isRequired,
  indexSets: PropTypes.array.isRequired,
};

CreateStreamButton.defaultProps = {
  buttonText: 'Create stream',
  bsSize: undefined,
  bsStyle: undefined,
  className: undefined,
};

export default CreateStreamButton;
