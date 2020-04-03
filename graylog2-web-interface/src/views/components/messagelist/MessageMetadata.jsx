// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';

type Props = {
  timestamp: string,
  receivedBy: React.Node,
  index: string,
  streams: Immutable.Set<React.Node>,
};

const MessageMetadata = ({ timestamp, receivedBy, index, streams }: Props) => (
  <dl className="message-details">
    {timestamp}
    {receivedBy}

    <dt>Stored in index</dt>
    <dd>{index || 'Message is not stored'}</dd>

    {streams.size > 0 && (
      <>
        <dt>Routed into streams</dt>
        <dd className="stream-list">
          <ul>
            {streams}
          </ul>
        </dd>
      </>
    )}
  </dl>
);

export default MessageMetadata;
