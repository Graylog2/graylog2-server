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

import { Link } from 'components/common/router';
import { MessageDetailsDefinitionList, ClipboardButton, Icon, Timestamp } from 'components/common';
import { ButtonGroup, Col, Label, Row } from 'components/bootstrap';
import StreamLink from 'components/streams/StreamLink';
import MessageFields from 'components/search/MessageFields';
import MessageDetailsTitle from 'components/search/MessageDetailsTitle';
import Routes from 'routing/Routes';
import AppConfig from 'util/AppConfig';
import MessagePermalinkButton from 'views/components/common/MessagePermalinkButton';

const MessageActions = ({ messageIndex, messageId }) => (
  <ButtonGroup className="pull-right">
    <MessagePermalinkButton messageIndex={messageIndex} messageId={messageId} />

    <ClipboardButton title="Copy ID" bsSize="small" text={message.id} />
  </ButtonGroup>
);

const InputName = ({ inputs, inputId }) => {
  const input = inputs.get(inputId);

  return input ? <span style={{ wordBreak: 'break-word' }}>{input.title}</span> : 'deleted input';
};

const NodeName = ({ nodes, nodeId }) => {
  const node = nodes.get(nodeId);
  let nodeInformation;

  if (node) {
    const nodeURL = Routes.node(nodeId);

    const nodeContent = (
      <>
        <Icon name="fork_right" />
        &nbsp;
        <span style={{ wordBreak: 'break-word' }}>{node.short_node_id}</span>&nbsp;/&nbsp;
        <span style={{ wordBreak: 'break-word' }}>{node.hostname}</span>
      </>
    );

    nodeInformation = AppConfig.isCloud()
      ? nodeContent
      : <a href={nodeURL}>{nodeContent}</a>;
  } else {
    nodeInformation = <span style={{ wordBreak: 'break-word' }}>stopped node</span>;
  }

  return nodeInformation;
};

const StreamLinks = ({ messageStreams, streamIds, streams }) => {
  if (messageStreams) {
    return messageStreams.map((stream) => (<li key={stream.id}><StreamLink stream={stream} /></li>));
  }

  return streamIds.map((id) => {
    const stream = streams.get(id);

    if (stream !== undefined) {
      return <li key={stream.id}><StreamLink stream={stream} /></li>;
    }

    return null;
  });
};

const MessageDetail = ({ renderForDisplay, inputs, nodes, streams, message, customFieldActions }) => {
  const streamIds = Immutable.Set(message.stream_ids);

  // Legacy
  const viaRadio = message.source_radio_id
    ? (
      <span>
        via <em><InputName inputs={inputs} inputId={message.source_radio_input_id} /></em> on
        radio <NodeName nodes={nodes} nodeId={message.source_radio_id} />
      </span>
    )
    : null;

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
            {(message.source_input_id && message.source_node_id && nodes) && (
              <div>
                <dt>Received by</dt>
                <dd>
                  <em><InputName inputs={inputs} inputId={message.source_input_id} /></em>{' '}
                  on <NodeName nodes={nodes} nodeId={message.source_node_id} />
                  {viaRadio && <br />}
                  {viaRadio}
                </dd>
              </div>
            )}

            <dt>Stored in index</dt>
            <dd>{message.index ? message.index : 'Message is not stored'}</dd>

            {streamIds.size > 0 && <dt>Routed into streams</dt>}
            {streamIds.size > 0
              && (
                <dd className="stream-list">
                  <ul>
                    <StreamLinks messageStreams={message.streams} streamIds={streamIds} streams={streams} />
                  </ul>
                </dd>
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

MessageDetail.defaultProps = {
  inputs: undefined,
  nodes: undefined,
  streams: undefined,
  customFieldActions: undefined,
};

export default MessageDetail;
