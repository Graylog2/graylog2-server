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

import { MessageDetailsDefinitionList } from 'components/common';
import type { Stream } from 'logic/streams/types';
import StreamLink from 'components/streams/StreamLink';

type Props = {
  timestamp: string;
  receivedBy: React.ReactElement;
  index: string;
  streams: Array<Stream>;
  assets: React.ReactElement;
};

const MessageMetadata = ({ timestamp, receivedBy, index, streams, assets }: Props) => (
  <MessageDetailsDefinitionList>
    {timestamp}
    {receivedBy}

    <dt>Stored in index</dt>
    <dd>{index || 'Message is not stored'}</dd>

    {!!streams.length && (
      <>
        <dt>Routed into streams</dt>
        <dd className="stream-list">
          <ul>
            {streams.map((stream) => (
              <li key={stream.id}>
                <StreamLink stream={stream} />
              </li>
            ))}
          </ul>
        </dd>
      </>
    )}

    {assets}
  </MessageDetailsDefinitionList>
);

export default MessageMetadata;
