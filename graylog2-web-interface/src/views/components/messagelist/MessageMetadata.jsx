// @flow strict
import * as React from 'react';

type Props = {
  timestamp: string,
  receivedBy: React.Node,
  index: string,
  streams: Array<React.Node>,
};

const MessageMetadata = ({ timestamp, receivedBy, index, streams }: Props) => (
  <dl className="message-details">
    {timestamp}
    {receivedBy}

    <dt>Stored in index</dt>
    <dd>{index || 'Message is not stored'}</dd>

    {streams.length > 0 && (
      <React.Fragment>
        <dt>Routed into streams</dt>
        <dd className="stream-list">
          <ul>
            {streams}
          </ul>
        </dd>
      </React.Fragment>
    )}
  </dl>
);

export default MessageMetadata;
