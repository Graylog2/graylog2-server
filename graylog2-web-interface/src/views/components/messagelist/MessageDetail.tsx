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
import { useState, useMemo } from 'react';
import Immutable from 'immutable';
import ImmutablePropTypes from 'react-immutable-proptypes';

import { AdditionalContext } from 'views/logic/ActionContext';
import { Link } from 'components/common/router';
import { Col, Label, Row } from 'components/bootstrap';
import StreamLink from 'components/streams/StreamLink';
import { MessageFields } from 'views/components/messagelist';
import MessageDetailsTitle from 'components/search/MessageDetailsTitle';
import { Icon, Spinner, Timestamp } from 'components/common';
import Routes from 'routing/Routes';
import type { Message } from 'views/components/messagelist/Types';
import type { Input } from 'components/messageloaders/Types';
import type { Stream } from 'views/stores/StreamsStore';
import CustomPropTypes from 'views/components/CustomPropTypes';
import type { FieldTypeMappingsList } from 'views/logic/fieldtypes/types';
import { useStore } from 'stores/connect';
import { SearchConfigStore } from 'views/stores/SearchConfigStore';
import FormatReceivedBy from 'views/components/messagelist/FormatReceivedBy';
import useIsLocalNode from 'views/hooks/useIsLocalNode';

import MessageDetailProviders from './MessageDetailProviders';
import MessageActions from './MessageActions';
import MessageAugmentations from './MessageAugmentations';
import MessageMetadata from './MessageMetadata';

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
  message: Message,
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
  const { fields, index, id, decoration_stats: decorationStats } = message;
  const { gl2_source_node, gl2_source_input } = fields;
  const { isLocalNode } = useIsLocalNode(gl2_source_node);
  const additionalContext = useMemo(() => ({ isLocalNode }), [isLocalNode]);

  const _toggleShowOriginal = () => {
    setShowOriginal(!showOriginal);
  };

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
    timestamp.push(<dd key={`dd-${rawTimestamp}`}><Timestamp dateTime={rawTimestamp} format="complete" /></dd>);
  }

  const messageTitle = _formatMessageTitle(index, id);

  return (
    <AdditionalContext.Provider value={additionalContext}>
      <MessageDetailProviders message={message}>
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
          <Row id={`sticky-augmentations-boundary-${message.id}`}>
            <Col md={3}>
              <MessageMetadata timestamp={timestamp}
                               index={index}
                               receivedBy={(
                                 <FormatReceivedBy isLocalNode={isLocalNode}
                                                   inputs={inputs}
                                                   sourceNodeId={gl2_source_node}
                                                   sourceInputId={gl2_source_input} />
                               )}
                               streams={streamsListItems} />
              <MessageAugmentations message={message} />
            </Col>
            <Col md={9}>
              <MessageFields message={message}
                             fields={messageFields} />
            </Col>
          </Row>
        </>
      </MessageDetailProviders>
    </AdditionalContext.Provider>
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
  message: CustomPropTypes.Message.isRequired,
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
  showTimestamp: true,
  streams: Immutable.Map(),
};

export default MessageDetail;
