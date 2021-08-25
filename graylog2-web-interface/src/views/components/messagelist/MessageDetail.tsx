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
import * as React from 'react';
import { useEffect, useState } from 'react';
import Immutable from 'immutable';
import { PluginStore } from 'graylog-web-plugin/plugin';
import ImmutablePropTypes from 'react-immutable-proptypes';

import { Link } from 'components/graylog/router';
import { Col, Label, Row } from 'components/graylog';
import StreamLink from 'components/streams/StreamLink';
import { MessageFields } from 'views/components/messagelist';
import MessageDetailsTitle from 'components/search/MessageDetailsTitle';
import { Icon, Spinner, Timestamp } from 'components/common';
import Routes from 'routing/Routes';
import { Message } from 'views/components/messagelist/Types';
import { Input } from 'components/messageloaders/Types';
import { Stream } from 'views/stores/StreamsStore';
import CustomPropTypes from 'views/components/CustomPropTypes';
import { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';
import { useStore } from 'stores/connect';
import { SearchConfigStore } from 'views/stores/SearchConfigStore';

import NodeName from './NodeName';
import MessageActions from './MessageActions';
import MessageAugmentations from './MessageAugmentations';
import MessageMetadata from './MessageMetadata';

const _inputName = (inputs: Props['inputs'], inputId: string) => {
  // eslint-disable-next-line react/destructuring-assignment
  const input = inputs.get(inputId);

  return input ? <span style={{ wordBreak: 'break-word' }}>{input.title}</span> : 'deleted input';
};

const FormatReceivedBy = ({ inputs, sourceInputId, sourceNodeId }: { inputs: Props['inputs'], sourceNodeId: string, sourceInputId: string }) => {
  const [isLocalNode, setIsLocalNode] = useState<boolean | undefined>();

  const forwarderPlugin = PluginStore.exports('forwarder');
  const ForwarderReceivedBy = forwarderPlugin?.[0]?.ForwarderReceivedBy;
  const _isLocalNode = forwarderPlugin?.[0]?.isLocalNode;

  useEffect(() => {
    if (sourceNodeId) {
      _isLocalNode(sourceNodeId).then(setIsLocalNode);
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

const _formatMessageTitle = (index, id) => {
  if (index) {
    return (
      <Link to={Routes.message_show(index, id)}>{id}</Link>
    );
  }

  return <span>{id} <Label bsStyle="warning">Not stored</Label></span>;
};

type Props = {
  allStreams?: Immutable.List<Stream>,
  disableMessageActions?: boolean,
  disableSurroundingSearch?: boolean,
  disableTestAgainstStream?: boolean,
  expandAllRenderAsync?: boolean,
  fields: FieldTypeMappingsList,
  inputs?: Immutable.Map<string, Input>,
  message?: Message,
  showTimestamp?: boolean,
  streams?: Immutable.Map<string, Stream>,
};

const MessageDetail = ({
  disableMessageActions,
  disableSurroundingSearch,
  disableTestAgainstStream,
  expandAllRenderAsync,
  fields: messageFields,
  message,
  streams,
  inputs,
  showTimestamp,
  allStreams,
}: Props) => {
  const { searchesClusterConfig } = useStore(SearchConfigStore);
  const [showOriginal, setShowOriginal] = useState(false);

  const _toggleShowOriginal = () => {
    setShowOriginal(!showOriginal);
  };

  const { fields, index, id, decoration_stats: decorationStats } = message;

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

  const streamIds = Immutable.Set(fields.streams as Array<string>);
  const streamsListItems = streamIds.map((streamId) => {
    // eslint-disable-next-line react/destructuring-assignment
    const stream = streams.get(streamId);

    if (stream !== undefined) {
      return <li key={stream.id}><StreamLink stream={stream} /></li>;
    }

    return null;
  }).toSet();

  let timestamp = null;

  if (showTimestamp) {
    timestamp = [];
    const rawTimestamp = fields.timestamp;

    timestamp.push(<dt key={`dt-${rawTimestamp}`}>Timestamp</dt>);
    timestamp.push(<dd key={`dd-${rawTimestamp}`}><Timestamp dateTime={rawTimestamp} /></dd>);
  }

  const { gl2_source_node, gl2_source_input } = fields;

  const messageTitle = _formatMessageTitle(index, id);

  return (
    <>
      <Row className="row-sm">
        <Col md={12}>
          <MessageActions index={index}
                          id={id}
                          fields={fields}
                          decorationStats={decorationStats}
                          disabled={disableMessageActions}
                          disableSurroundingSearch={disableSurroundingSearch}
                          disableTestAgainstStream={disableTestAgainstStream}
                          showOriginal={showOriginal}
                          toggleShowOriginal={_toggleShowOriginal}
                          searchConfig={searchesClusterConfig}
                          streams={allStreams} />
          <MessageDetailsTitle>
            <Icon name="envelope" />
            &nbsp;
            {messageTitle}
          </MessageDetailsTitle>
        </Col>
      </Row>
      <Row>
        <Col md={3}>
          <MessageMetadata timestamp={timestamp}
                           index={index}
                           receivedBy={<FormatReceivedBy inputs={inputs} sourceNodeId={gl2_source_node} sourceInputId={gl2_source_input} />}
                           streams={streamsListItems} />
          <MessageAugmentations />
        </Col>
        <Col md={9}>
          <MessageFields message={message}
                         fields={messageFields} />
        </Col>
      </Row>
    </>
  );
};

MessageDetail.propTypes = {
  allStreams: ImmutablePropTypes.list,
  disableMessageActions: PropTypes.bool,
  disableSurroundingSearch: PropTypes.bool,
  disableTestAgainstStream: PropTypes.bool,
  expandAllRenderAsync: PropTypes.bool,
  fields: ImmutablePropTypes.list,
  inputs: ImmutablePropTypes.map,
  message: CustomPropTypes.Message,
  showTimestamp: PropTypes.bool,
  streams: ImmutablePropTypes.map,
};

MessageDetail.defaultProps = {
  allStreams: Immutable.List(),
  disableMessageActions: false,
  disableSurroundingSearch: false,
  disableTestAgainstStream: false,
  expandAllRenderAsync: false,
  fields: Immutable.List(),
  inputs: Immutable.Map(),
  message: {} as Message,
  showTimestamp: true,
  streams: Immutable.Map(),
};

export default MessageDetail;
