import * as React from 'react';
import { useState, useEffect } from 'react';
import * as Immutable from 'immutable';

import { Spinner } from 'components/common';
import NodeName from 'views/components/messagelist/NodeName';
import { Input } from 'components/messageloaders/Types';
import usePluginEntities from 'views/logic/usePluginEntities';

type Inputs = Immutable.Map<string, Input>;

const _inputName = (inputs: Inputs, inputId: string) => {
  // eslint-disable-next-line react/destructuring-assignment
  const input = inputs.get(inputId);

  return input ? <span style={{ wordBreak: 'break-word' }}>{input.title}</span> : 'deleted input';
};

const FormatReceivedBy = ({ inputs, sourceInputId, sourceNodeId }: { inputs: Inputs, sourceNodeId: string, sourceInputId: string }) => {
  const [isLocalNode, setIsLocalNode] = useState<boolean | undefined>();

  const forwarderPlugin = usePluginEntities('forwarder');
  const ForwarderReceivedBy = forwarderPlugin?.[0]?.ForwarderReceivedBy;
  const _isLocalNode = forwarderPlugin?.[0]?.isLocalNode;

  useEffect(() => {
    if (sourceNodeId && _isLocalNode) {
      _isLocalNode(sourceNodeId).then(setIsLocalNode, () => setIsLocalNode(true));
    } else {
      setIsLocalNode(true);
    }
  }, [sourceNodeId, _isLocalNode]);

  if (!sourceNodeId) {
    return null;
  }

  if (isLocalNode === undefined) {
    return <Spinner />;
  }

  if (isLocalNode === false) {
    return <ForwarderReceivedBy inputId={sourceInputId} forwarderNodeId={sourceNodeId} />;
  }

  return (
    <div>
      <dt>Received by</dt>
      <dd>
        <em>{_inputName(inputs, sourceInputId)}</em>{' '}
        on <NodeName nodeId={sourceNodeId} />
      </dd>
    </div>
  );
};

export default FormatReceivedBy;
