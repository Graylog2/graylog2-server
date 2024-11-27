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
import React from 'react';
import Immutable from 'immutable';
import styled from 'styled-components';

import { Link } from 'components/common/router';
import { MessageDetailsDefinitionList, ClipboardButton, Icon, Timestamp } from 'components/common';
import { ButtonGroup, Col, Label, Row } from 'components/bootstrap';
import StreamLink from 'components/streams/StreamLink';
import MessageFields from 'components/search/MessageFields';
import MessageDetailsTitle from 'components/search/MessageDetailsTitle';
import Routes from 'routing/Routes';
import MessagePermalinkButton from 'views/components/common/MessagePermalinkButton';
import type { Message } from 'views/components/messagelist/Types';
import type { Stream } from 'views/stores/StreamsStore';
import type { Input } from 'components/messageloaders/Types';
import NodeName from 'views/components/messagelist/NodeName';

const Span = styled.span`
  word-break: break-word;
`;

const MessageActions = ({ messageIndex, messageId }: { messageIndex: string | undefined, messageId: string }) => (
  <ButtonGroup className="pull-right">
    <MessagePermalinkButton messageIndex={messageIndex} messageId={messageId} />

    <ClipboardButton title="Copy ID" bsSize="small" text={messageId} />
  </ButtonGroup>
);

const InputName = ({ inputs, inputId }: { inputs: Immutable.Map<string, Input> | undefined, inputId: string }) => {
  const input = inputs?.get(inputId);

  return input ? <Span>{input.title}</Span> : <>deleted input</>;
};

const StreamLinks = ({ messageStreams, streamIds, streams }: {
  messageStreams: Array<Stream>,
  streamIds: Immutable.Set<string>,
  streams: Immutable.Map<string, Stream>
}) => {
  if (messageStreams) {
    return (
      <>
        {messageStreams.map((stream) => (<li key={stream.id}><StreamLink stream={stream} /></li>))}
      </>
    );
  }

  return (
    <>
      {streamIds.map((id) => streams.get(id))
        .filter((stream) => !!stream)
        .map((stream) => <li key={stream.id}><StreamLink stream={stream} /></li>)
        .toArray()}
    </>
  );
};

type Props = {
  message: Message & { streams?: Array<Stream> },
  inputs?: Immutable.Map<string, Input>,
  streams?: Immutable.Map<string, Stream>,
  renderForDisplay: (fieldName: string) => React.ReactNode,
  customFieldActions?: React.ReactNode
}

const MessageDetail = ({ renderForDisplay, inputs = undefined, streams = undefined, message, customFieldActions = undefined }: Props) => {
  const streamIds = Immutable.Set(message.stream_ids);
  const rawTimestamp = message.fields.timestamp;
  const timestamp = [
    <dt key={`dt-${rawTimestamp}`}>Timestamp</dt>,
    <dd key={`dd-${rawTimestamp}`}><Timestamp dateTime={rawTimestamp} /></dd>,
  ];

  return (
    <div>
      <Row className="row-sm">
        <Col md={12}>
          {customFieldActions
            ? <MessageActions messageIndex={message.index} messageId={message.id} />
            : <ButtonGroup className="pull-right" />}
          <MessageDetailsTitle>
            <Icon name="mail" />
            &nbsp;
            {message.index ? (
              <Link to={Routes.message_show(message.index, message.id)}>
                {message.id}
              </Link>
            ) : <span>{message.id} <Label bsStyle="warning">Not stored</Label></span>}
          </MessageDetailsTitle>
        </Col>
      </Row>
      <Row>
        <Col md={3}>
          <MessageDetailsDefinitionList>
            {timestamp}
            {(message.source_input_id && message.source_node_id) && (
              <div>
                <dt>Received by</dt>
                <dd>
                  <em><InputName inputs={inputs} inputId={message.source_input_id} /></em>{' '}
                  on <NodeName nodeId={message.source_node_id} />

                  {/* Legacy */}
                  {message.source_radio_id && (
                    <>
                      <br />
                      <span>
                        via <em><InputName inputs={inputs} inputId={message.source_radio_input_id} /></em> on
                        radio <NodeName nodeId={message.source_radio_id} />
                      </span>
                    </>
                  )}
                </dd>
              </div>
            )}

            <dt>Stored in index</dt>
            <dd>{message.index ? message.index : 'Message is not stored'}</dd>

            {streamIds.size > 0 && (
              <>
                <dt>Routed into streams</dt>
                <dd className="stream-list">
                  <ul>
                    <StreamLinks messageStreams={message.streams} streamIds={streamIds} streams={streams} />
                  </ul>
                </dd>
              </>
            )}
          </MessageDetailsDefinitionList>
        </Col>
        <Col md={9}>
          <div>
            <MessageFields message={message}
                           renderForDisplay={renderForDisplay}
                           customFieldActions={customFieldActions} />
          </div>
        </Col>
      </Row>
    </div>
  );
};

export default MessageDetail;
