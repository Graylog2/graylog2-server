// @flow strict
import PropTypes from 'prop-types';
import React, { useState } from 'react';
import Immutable from 'immutable';
import { Link } from 'react-router';
import styled from 'styled-components';

import { Col, Label, Row } from 'components/graylog';
import StreamLink from 'components/streams/StreamLink';
import { MessageFields } from 'views/components/messagelist';
import { Icon, Spinner, Timestamp } from 'components/common';

import Routes from 'routing/Routes';
import type { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';
import MessageActions from './MessageActions';
import MessageMetadata from './MessageMetadata';
import NodeName from './NodeName';
import type { Message } from './Types';
import { Message as MessagePropType } from './MessagePropTypes';

const Title = styled.h3`
  height: 30px;

  a {
    color: #000;
  }

  .label {
    font-size: 50%;
    line-height: 200%;
    margin-left: 5px;
    vertical-align: bottom;
  }
`;

const _inputName = (inputs, inputId) => {
  const input = inputs.get(inputId);
  return input ? <span style={{ wordBreak: 'break-word' }}>{input.title}</span> : 'deleted input';
};

const _formatReceivedBy = (inputs, sourceNodeId, sourceInputId) => {
  if (!sourceNodeId) {
    return null;
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

const _formatMessageTitle = (index, id) => {
  if (index) {
    return (
      <Link to={Routes.message_show(index, id)}>{id}</Link>
    );
  }
  return <span>{id} <Label bsStyle="warning">Not stored</Label></span>;
};

type Props = {
  expandAllRenderAsync: boolean,
  message: Message,
  fields: FieldTypeMappingsList,
  allStreams: Immutable.List,
  disableFieldActions: boolean,
  disableMessageActions: boolean,
  disableSurroundingSearch: boolean,
  disableTestAgainstStream: boolean,
  searchConfig: Object,
  inputs: Immutable.Map,
  showTimestamp: boolean,
  streams: Immutable.Map,
}

const MessageDetail = ({
  expandAllRenderAsync,
  message,
  fields: messageFields,
  allStreams,
  disableFieldActions,
  disableMessageActions,
  disableSurroundingSearch,
  disableTestAgainstStream,
  searchConfig,
  inputs,
  showTimestamp,
  streams: streamsProp,
}: Props) => {
  const [showOriginal, setShowOriginal] = useState(false);
  const { fields, index, id } = message;
  // Short circuit when all messages are being expanded at the same time
  if (expandAllRenderAsync) {
    return (
      <Row>
        <Col md={12}>
          <Spinner />
        </Col>
      </Row>
    );
  }

  const streamIds = Immutable.Set(fields.streams);
  const streams = streamIds.map((streamId) => {
    // eslint-disable-next-line react/destructuring-assignment
    const stream = streamsProp.get(streamId);
    if (stream !== undefined) {
      return <li key={stream.id}><StreamLink stream={stream} /></li>;
    }
    return null;
  });

  let timestamp = null;
  if (showTimestamp) {
    timestamp = [];
    const rawTimestamp = fields.timestamp;

    timestamp.push(<dt key={`dt-${rawTimestamp}`}>Timestamp</dt>);
    timestamp.push(<dd key={`dd-${rawTimestamp}`}><Timestamp dateTime={rawTimestamp} /></dd>);
  }

  // eslint-disable-next-line camelcase
  const { gl2_source_node, gl2_source_input } = fields;
  const receivedBy = _formatReceivedBy(inputs, gl2_source_node, gl2_source_input);
  const messageTitle = _formatMessageTitle(index, id);

  return (
    <React.Fragment>
      <Row className="row-sm">
        <Col md={12}>
          <MessageActions index={index}
                          id={id}
                          fields={fields}
                          decorationStats={message.decoration_stats}
                          disabled={disableMessageActions}
                          disableSurroundingSearch={disableSurroundingSearch}
                          disableTestAgainstStream={disableTestAgainstStream}
                          showOriginal={showOriginal}
                          toggleShowOriginal={() => setShowOriginal(!showOriginal)}
                          searchConfig={searchConfig}
                          streams={allStreams} />
          <Title>
            <Icon name="envelope" />
              &nbsp;
            {messageTitle}
          </Title>
        </Col>
      </Row>
      <Row>
        <Col md={3}>
          <MessageMetadata timestamp={timestamp}
                           index={index}
                           receivedBy={receivedBy}
                           streams={streams} />
        </Col>
        <Col md={9}>
          <MessageFields message={message}
                         fields={messageFields}
                         disableFieldActions={disableFieldActions}
                         showDecoration={showOriginal} />
        </Col>
      </Row>
    </React.Fragment>
  );
};

MessageDetail.propTypes = {
  allStreams: PropTypes.object,
  disableFieldActions: PropTypes.bool,
  disableMessageActions: PropTypes.bool,
  disableSurroundingSearch: PropTypes.bool,
  disableTestAgainstStream: PropTypes.bool,
  expandAllRenderAsync: PropTypes.bool,
  fields: PropTypes.object.isRequired,
  inputs: PropTypes.object,
  message: MessagePropType.isRequired,
  searchConfig: PropTypes.object,
  showTimestamp: PropTypes.bool,
  streams: PropTypes.object,
};

MessageDetail.defaultProps = {
  allStreams: {},
  disableFieldActions: false,
  disableMessageActions: false,
  disableSurroundingSearch: false,
  disableTestAgainstStream: false,
  expandAllRenderAsync: false,
  inputs: {},
  searchConfig: {},
  showTimestamp: true,
  streams: {},
};

export default MessageDetail;
