import React, { useCallback, useState } from 'react';

import { Button } from 'components/bootstrap';
import StreamModal from 'components/streams/StreamModal';
import type { Stream } from 'stores/streams/StreamsStore';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import type { BsSize } from 'components/bootstrap/types';
import type { StyleProps } from 'components/bootstrap/Button';

type Props = {
  bsSize?: BsSize,
  bsStyle?: StyleProps,
  buttonText?: string,
  className?: string,
  indexSets: Array<IndexSet>
  onCreate: (values: Partial<Stream>) => Promise<void>
}

const CreateStreamButton = ({ bsSize, bsStyle, buttonText = 'Create stream', className, indexSets, onCreate }: Props) => {
  const [showCreateModal, setShowCreateModal] = useState(false);
  const sendTelemetry = useSendTelemetry();

  const toggleCreateModal = useCallback(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.STREAMS.CREATE_FORM_MODAL_OPENED, {
      app_pathname: 'streams',
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

export default CreateStreamButton;
